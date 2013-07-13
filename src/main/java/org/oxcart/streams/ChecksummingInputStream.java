package org.oxcart.streams;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.codec.binary.Hex;

/**
 * An IValidatingInputStream that wraps another input stream and validates against a provided checksum.
 * 
 * @author ox.to.a.cart /at/ gmail.com
 * 
 */
public class ChecksummingInputStream extends IValidatingInputStream {
    private InputStream originalStream;
    private MessageDigest digester;
    private byte[] expectedChecksum;

    /**
     * Construct a ChecksummingInputStream that wraps the given originalStream and calculates a message digest using the
     * specified algorithm.
     * 
     * @param originalStream
     * @param digestAlgorithm
     * @throws NoSuchAlgorithmException
     */
    public ChecksummingInputStream(InputStream originalStream, String digestAlgorithm, byte[] expectedChecksum)
            throws NoSuchAlgorithmException {
        this.originalStream = originalStream;
        this.digester = MessageDigest.getInstance(digestAlgorithm);
        this.expectedChecksum = expectedChecksum;
    }

    @Override
    public int read() throws IOException {
        int next = originalStream.read();
        if (next != -1) {
            digester.update((byte) next);
        }
        return next;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            originalStream.close();
        }
    }

    @Override
    protected void collectValidationErrors(List<String> validationErrors) {
        byte[] actualChecksum = digester.digest();
        if (!Arrays.equals(expectedChecksum, actualChecksum)) {
            validationErrors.add(String.format("Checksum failure.\n\nExpected: %1$s\nActual:   %2$s",
                                               Hex.encodeHexString(expectedChecksum),
                                               Hex.encodeHexString(actualChecksum)));
        }
    }

}
