package org.oxcart.streams;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An InputStream that reads ahead in the background and buffers data via the file system. ReadAheadInputStreams are
 * opened using {@link #open(org.oxcart.streams.IStreamProvider, java.util.concurrent.ExecutorService) open}.
 * 
 * @author ox.to.a.cart /at/ gmail.com
 * 
 */
public class ReadAheadInputStream extends InputStream implements Runnable {
    private IStreamProvider streamProvider;
    private File bufferFile;
    private BufferedOutputStream bufferOutputStream;
    private BufferedInputStream bufferInputStream;
    private AtomicBoolean stillBuffering = new AtomicBoolean(true);
    private AtomicInteger bufferedBytes = new AtomicInteger(0);
    private AtomicReference<IOException> readAheadException = new AtomicReference<IOException>(null);

    /**
     * Open a new ReadAheadInputStream that wraps the supplied originalStream and that reads ahead using a task
     * submitted to the provided ExecutorService.
     * 
     * @param streamProvider
     *            provides the underlying stream to read (called on its own thread)
     * @param executorService
     */
    public static ReadAheadInputStream open(IStreamProvider streamProvider, ExecutorService executorService)
            throws IOException {
        final ReadAheadInputStream stream = new ReadAheadInputStream(streamProvider);
        // Start reading ahead
        executorService.submit(stream);
        return stream;
    }

    private ReadAheadInputStream(IStreamProvider streamProvider) throws IOException {
        this.streamProvider = streamProvider;
        String tempFileName = UUID.randomUUID().toString();
        this.bufferFile = File.createTempFile(tempFileName, ".tmp");
        this.bufferOutputStream = new BufferedOutputStream(new FileOutputStream(bufferFile));
        this.bufferInputStream = new BufferedInputStream(new FileInputStream(bufferFile));
    }

    @Override
    public void run() {
        try {
            BufferedInputStream originalStream = new BufferedInputStream(streamProvider.openStream());
            try {
                int next;
                while ((next = originalStream.read()) != -1) {
                    bufferOutputStream.write(next);
                    bufferedBytes.incrementAndGet();
                }
            } catch (IOException ioe) {
                readAheadException.set(ioe);
            } finally {
                try {
                    originalStream.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        } catch (IOException ioe) {
            readAheadException.set(ioe);
        } catch (Exception e) {
            readAheadException.set(new IOException(e.getMessage(), e));
        } finally {
            stillBuffering.set(false);
            try {
                bufferOutputStream.close();
            } catch (Exception e) {
                // ignore
            }
        }

    }

    @Override
    public int read() throws IOException {
        while (stillBuffering.get() && bufferInputStream.available() == 0) {
            // Wait for the buffer to catch up
            try {
                Thread.sleep(50);
            } catch (InterruptedException ie) {
                throw new IOException(String.format("Interrupted while waiting for buffer to catch up: "
                        + ie.getMessage()), ie);
            }
        }

        // Check to see if there was an exception doing the read ahead
        IOException ioe = readAheadException.get();
        if (ioe != null) {
            throw ioe;
        }

        return bufferInputStream.read();
    }

    @Override
    public void close() throws IOException {
        try {
            bufferInputStream.close();
        } finally {
            bufferFile.delete();
        }
    }

    /**
     * Returns the number of bytes buffered up to this point.
     * 
     * @return
     */
    public int getBufferedBytes() {
        return bufferedBytes.get();
    }
}
