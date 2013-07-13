package org.oxcart.streams;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

/**
 * An InputStream that reads a resource in parallel from multiple urls by fetching ranges of the file on multiple
 * threads.
 * 
 * @author ox.to.a.cart /at/ gmail.com
 * 
 */
public class ParallelHttpGetInputStream extends IValidatingInputStream implements IProgressProvider {
    private ExecutorService executorService;
    private HttpClient httpClient;
    private List<Resource> resources;
    private MergedInputStream inputStream;
    private List<ReadAheadInputStream> contentInputStreams;
    private List<Integer> expectedContentLengths;

    /**
     * Construct a stream to fetch a single resource from multiple urls in parallel, using the given ExecutorService to
     * perform the parallel fetches.
     * 
     * @param executorService
     * @param urls
     * @throws IOException
     */
    public ParallelHttpGetInputStream(ExecutorService executorService, String... urls) throws IOException {
        this.executorService = executorService;
        initHttpClient();
        buildResources(urls);
        ensureResourcesAreCompatible();
        open();
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    public int getContentLength() {
        return resources.get(0).contentLength;
    }

    public int getBytesRead() {
        return inputStream.getBytesRead();
    }

    @Override
    public void provideProgress(IProgressReporter recorder) {
        for (int i = 0; i < contentInputStreams.size(); i++) {
            recorder.report(resources.get(i).url,
                            "Buffered",
                            expectedContentLengths.get(i),
                            contentInputStreams.get(i).getBufferedBytes());
        }
    }

    /**
     * Validate the results of the download. This method should be called only after reading all bytes from this stream.
     * 
     * @return
     */
    @Override
    protected void collectValidationErrors(List<String> validationErrors) {
        if (getBytesRead() != getContentLength()) {
            validationErrors.add(String.format("WARNING - Amount of read content did not match expected content length, data may be corrupted.  Expected %1$s, read %2$s",
                                               getContentLength(), getBytesRead()));
        }
    }

    private void initHttpClient() {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(
                      new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        schemeRegistry.register(
                      new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));

        PoolingClientConnectionManager cm = new PoolingClientConnectionManager(schemeRegistry);
        cm.setDefaultMaxPerRoute(16);
        cm.setMaxTotal(64);
        this.httpClient = new DefaultHttpClient(cm);
    }

    private void buildResources(String[] urls) throws IOException {
        this.resources = new ArrayList<Resource>();
        for (String url : urls) {
            Resource resource = new Resource(url);
            if (!resource.acceptsRangeRequests) {
                throw new IOException(String.format("Resource does not allow range requests: %1$s", resource.url));
            }
            resources.add(resource);
        }
    }

    private void ensureResourcesAreCompatible() throws IOException {
        Resource previousResource = null;
        for (Resource nextResource : resources) {
            if (previousResource != null) {
                previousResource.ensureCompatibleWith(nextResource);
            }
            previousResource = nextResource;
        }
    }

    private void open() throws IOException {
        contentInputStreams = new ArrayList<ReadAheadInputStream>();
        expectedContentLengths = new ArrayList<Integer>();
        // Figure out size of each segment to download
        int segmentSize = getContentLength() / resources.size();
        for (int i = 0; i < resources.size(); i++) {
            boolean onLastSegment = i == resources.size() - 1;
            final int currentOffset = i * segmentSize;
            final int end = onLastSegment ? getContentLength() : (i + 1) * segmentSize;
            final Resource resource = resources.get(i);
            contentInputStreams.add(ReadAheadInputStream.open(new IStreamProvider() {
                @Override
                public InputStream openStream() throws IOException {
                    return resource.fetchRange(currentOffset, end);
                }
            }, executorService));
            expectedContentLengths.add(end - currentOffset);
        }
        this.inputStream = new MergedInputStream(
                                                 contentInputStreams.toArray(new InputStream[contentInputStreams.size()]));
    }

    /**
     * Represents a resource which will be used to fetch part of the download.
     * 
     * @author ox.to.a.cart /at/ gmail.com
     * 
     */
    private class Resource {
        private String url;
        private HttpContext context;
        private String transferEncoding = "";
        private int contentLength;
        private boolean acceptsRangeRequests;

        public Resource(String url) throws IOException {
            this.url = url;
            this.context = new BasicHttpContext();
            HttpHead method = new HttpHead(url);
            HttpResponse response = httpClient.execute(method, context);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new IOException(
                                      String.format("Unrecognized response on attempting to get resource HEAD information. Expected 200, got %1$s",
                                                    statusCode));
            }
            if (response.containsHeader("Transfer-Encoding")) {
                transferEncoding = response.getFirstHeader("Transfer-Encoding").getValue();
            }
            contentLength = Integer.parseInt(response.getFirstHeader("Content-Length").getValue());
            // Per RFC 2616, we assume that range requests are accepted unless the server explicitly says they aren't
            Header acceptRangesHeader = response.getFirstHeader("Accept-Ranges");
            acceptsRangeRequests = acceptRangesHeader == null
                    || !"none".equalsIgnoreCase(acceptRangesHeader.getValue().trim());
        }

        /**
         * Fetches a section of the resource specified by the given range (in bytes).
         * 
         * @param start
         *            start of range (inclusive)
         * @param end
         *            end of range (exclusive)
         * @return
         * @throws IOException
         */
        public InputStream fetchRange(int start, int end) throws IOException {
            HttpGet method = new HttpGet(url);
            method.addHeader("Range", String.format("bytes=%1$s-%2$s", start, end - 1));
            HttpResponse response = httpClient.execute(method, context);
            return response.getEntity().getContent();
        }

        /**
         * Two resources are considered compatible if their transfer encodings and content lengths match.
         * 
         * @param other
         * @return
         */
        public void ensureCompatibleWith(Resource other) throws IOException {
            if (!transferEncoding.equals(other.transferEncoding)) {
                throw new IOException(String.format("Transfer-Encoding for resources '%1$s' and '%2$s' did not match",
                                                    url, other.url));
            }
            if (contentLength != other.contentLength) {
                throw new IOException(String.format("Content-Length for resources '%1$s' and '%2$s' did not match",
                                                    url, other.url));
            }
        }
    }

}
