package org.oxcart.pget;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.oxcart.streams.ChecksummingInputStream;
import org.oxcart.streams.IProgressRecorder;
import org.oxcart.streams.ParallelHttpGetInputStream;
import org.oxcart.streams.ValidatingInputStream;

/**
 * <p>
 * Downloads a file in parallel using one or more urls.
 * </p>
 * 
 * <p>
 * For example:
 * </p>
 * 
 * <pre>
 * java org.oxcart.pget.PGet -c 04a1b0fc8a98999c6f78b35df9d8296996b5f6107c1b7f179c26a0496895b03f \
 *      "https://lantern.s3.amazonaws.com/lantern-video-broadband.m4v" \
 *      "https://dl.dropboxusercontent.com/s/rxnpmdrs2jms193/lantern-video-broadband.m4v" \
 *      > /tmp/downloaded_file.m4v
 * </pre>
 * 
 * @author ox.to.a.cart /at/ gmail.com
 * 
 */
public class PGet {
    private static final String SHA_256 = "SHA-256";

    private Options options;
    private CommandLine commandLine;
    private AtomicBoolean finished = new AtomicBoolean(false);

    public PGet(String... args) {
        options = new Options();
        options.addOption("?", "help", false, "Print this message");
        options.addOption("o", "outfile", true,
                          "(Optional) Name of file to which to output downloaded file.  If not specified, data goes to stdout.");
        options.addOption("t", "threads", true,
                          "(Optional) Number of concurrent threads to use for download.  Defaults to the number of urls.");
        options.addOption("c", "checksum", true, "(Optional) SHA-256 checksum to use to validate result (hex encoded)");
        try {
            commandLine = new GnuParser().parse(options, args);
        } catch (ParseException pe) {
            printUsage();
            throw new RuntimeException("Unable to parse args: " + pe.getMessage(), pe);
        }
    }

    /**
     * Fetch the download per the specified command-line options.
     */
    public int fetch() {
        String[] urls = commandLine.getArgs();
        if (commandLine.hasOption("help")) {
            printUsage();
            return 0;
        } else if (urls.length == 0) {
            System.err.println("Please supply at least 1 url");
            printUsage();
            return 1;
        } else {
            boolean valid = true;
            int numberOfThreads = urls.length;
            if (commandLine.hasOption("threads")) {
                numberOfThreads = Integer.parseInt(commandLine.getOptionValue("threads"));
            }
            ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
            try {
                valid = doFetch(executorService, urls);
            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace(System.err);
                return 3;
            } finally {
                executorService.shutdown();
            }
            return valid ? 0 : 2;
        }
    }

    /**
     * Perform the I/O activity for the download.
     * 
     * @param executorService
     * @param urls
     * @return
     * @throws IOException
     */
    private boolean doFetch(ExecutorService executorService, String[] urls) throws IOException {
        boolean valid = false;
        ParallelHttpGetInputStream parallelStream = new ParallelHttpGetInputStream(executorService, urls);
        ValidatingInputStream stream = parallelStream;
        if (commandLine.hasOption("checksum")) {
            // Add SHA-256 checksumming
            try {
                stream = new ChecksummingInputStream(parallelStream, SHA_256,
                                                     Hex.decodeHex(commandLine.getOptionValue("checksum")
                                                                              .toCharArray()));
            } catch (NoSuchAlgorithmException nsae) {
                System.err.println("WARNING: SHA-256 checksum not supported on this system, skipping checksumming");
            } catch (DecoderException de) {
                System.err.println("WARNING: Invalid SHA-256 checksum (not Hex encoded?), skipping checksumming");
            }
        }
        try {
            trackProgress(parallelStream);
            if (commandLine.hasOption("outfile")) {
                doFetchToFile(stream);
            } else {
                doFetchToStandardOut(stream);
            }
            valid = stream.isValid();
            if (!valid) {
                for (String errorMessage : stream.getValidationErrors()) {
                    System.err.println("WARNING: " + errorMessage);
                }
            }
        } finally {
            try {
                finished.set(true);
            } finally {
                stream.close();
            }
        }
        return valid;
    }

    /**
     * Fetch the download into a file.
     * 
     * @param stream
     * @throws IOException
     */
    private void doFetchToFile(ValidatingInputStream stream) throws IOException {
        // Write to out file
        File file = new File(commandLine.getOptionValue("outfile"));
        if (file.exists()) {
            file.delete();
        }
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        try {
            IOUtils.copy(stream, out);
        } finally {
            out.close();
        }
    }

    /**
     * Fetch the download and write to stdout.
     * 
     * @param stream
     * @throws IOException
     */
    private void doFetchToStandardOut(ValidatingInputStream stream) throws IOException {
        // Write to standard out
        IOUtils.copy(stream, System.out);
    }

    /**
     * Asynchronously track the progress of the download.
     * 
     * @param stream
     */
    private void trackProgress(final ParallelHttpGetInputStream stream) {
        new Thread() {
            @Override
            public void run() {
                IProgressRecorder recorder = new IProgressRecorder() {
                    @Override
                    public void record(String name, String category, double total, double progress) {
                        System.err.println(String.format("%1$s (%2$s) %3$.0f %%", name, category, 100 * progress
                                / total));
                    }
                };
                while (!finished.get()) {
                    // Wait 1 second between progress reports
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        break;
                    }
                    System.err.println("\n\n");
                    stream.reportProgress(recorder);
                }
            }
        }.start();
    }

    private void printUsage() {
        String command = "pget [options] url1, url2, ...";
        PrintWriter writer = new PrintWriter(System.err);
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp(writer, 80, command,
                                "pget downloads a file in parallel from one or more supplied urls\n", options, 2, 2, "");
        writer.print("\n\n");
        writer.close();
    }

    public static void main(String[] args) throws Exception {
        System.exit(new PGet(args).fetch());
    }
}
