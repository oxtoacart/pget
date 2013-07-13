package org.oxcart.streams;

import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

/**
 * InputStream that merges multiple other InputStreams.
 * 
 * @author ox.to.a.cart /at/ gmail.com
 * 
 */
public class MergedInputStream extends InputStream {
    private Stack<InputStream> remainingStreams = new Stack<InputStream>();
    private InputStream currentStream;
    private int bytesRead = 0;

    public MergedInputStream(InputStream... originalStreams) {
        // Add streams to stack backwards so that we pop() them in the same order as given.
        for (int i = originalStreams.length - 1; i >= 0; i--) {
            remainingStreams.push(originalStreams[i]);
        }
    }

    @Override
    public int read() throws IOException {
        int result;
        while (currentStream == null || (result = currentStream.read()) == -1) {
            if (currentStream != null) {
                // close the current stream
                currentStream.close();
            }
            if (remainingStreams.isEmpty()) {
                // We're done
                return -1;
            }
            // Move on to next stream
            currentStream = remainingStreams.pop();
        }
        bytesRead += 1;
        return result;
    }

    /**
     * Close the currentStream and all remainingStreams as necessary. If the MergedInputStream was already completely
     * read, then all original streams will have already been closed.
     */
    @Override
    public void close() throws IOException {
        //
        try {
            if (currentStream != null) {
                currentStream.close();
            }
        } finally {
            IOException closeException = null;
            while (!remainingStreams.isEmpty()) {
                try {
                    remainingStreams.pop().close();
                } catch (IOException ioe) {
                    closeException = ioe;
                }
            }
            if (closeException != null) {
                throw closeException;
            }
        }
    }

    public int getBytesRead() {
        return bytesRead;
    }

}
