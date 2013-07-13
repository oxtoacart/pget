package org.oxcart.streams;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;
import org.junit.Test;

/**
 * @author ox.to.a.cart /at/ gmail.com
 * 
 */
public class ChecksummingInputStreamTest extends StreamTest {

    @Test
    public void testSHA256Success() throws Exception {
        MessageDigest digester = MessageDigest.getInstance("SHA-256");
        byte[] expectedDigest = digester.digest(bunchOfBytes);
        ChecksummingInputStream stream = new ChecksummingInputStream(
                                                                     new ByteArrayInputStream(bunchOfBytes),
                                                                     "SHA-256",
                                                                     expectedDigest);
        try {
            while (stream.read() != -1) {
                // Loop through the stream
            }
            assertTrue("Stream should be valid", stream.valid());
        } finally {
            stream.close();
        }
    }

    @Test
    public void testSHA256Failure() throws Exception {
        MessageDigest digester = MessageDigest.getInstance("SHA-256");
        byte[] actualDigest = digester.digest(bunchOfBytes);
        byte[] intentionallyIncorrectDigest = Arrays.copyOf(actualDigest, actualDigest.length);
        intentionallyIncorrectDigest[0] = intentionallyIncorrectDigest[0] == 0 ? (byte) 1 : (byte) 0;
        ChecksummingInputStream stream = new ChecksummingInputStream(
                                                                     new ByteArrayInputStream(bunchOfBytes),
                                                                     "SHA-256",
                                                                     intentionallyIncorrectDigest);
        try {
            while (stream.read() != -1) {
                // Loop through the stream
            }
            assertFalse("Stream should not be valid", stream.valid());
            assertEquals(1, stream.getValidationErrors().size());
            String firstValidationError = stream.getValidationErrors().get(0);
            assertTrue("Error should contain expected digest as hex-encoded",
                       firstValidationError.contains(Hex.encodeHexString(intentionallyIncorrectDigest)));
            assertTrue("Error should contain actual digest as hex-encoded",
                       firstValidationError.contains(Hex.encodeHexString(actualDigest)));
        } finally {
            stream.close();
        }
    }

    @Test
    public void testUnknownAlgorithm() {
        try {
            new ChecksummingInputStream(null, "Unknown Algorithm", null);
            fail("Supplying an unknown digest algorithm should have thrown an exception");
        } catch (Exception e) {
            assertTrue(e instanceof NoSuchAlgorithmException);
        }
    }

    @Test
    public void testCloseFailure() throws Exception {
        MockInputStream mockStream = new MockInputStream(true, true);
        ChecksummingInputStream stream = new ChecksummingInputStream(mockStream,
                                                                     "SHA-256",
                                                                     new byte[0]);
        try {
            stream.close();
            fail("Exception on underlying close should have propagated");
        } catch (IOException ioe) {
            assertEquals("Exception thrown on close() should have the right message",
                         MockInputStream.CLOSE_FAILURE_MESSAGE,
                         ioe.getMessage());
        }
    }

    @Test
    public void testReadFailure() throws Exception {
        MockInputStream mockStream = new MockInputStream(true, false);
        ChecksummingInputStream stream = new ChecksummingInputStream(mockStream,
                                                                     "SHA-256",
                                                                     new byte[0]);
        try {
            stream.read();
            fail("Trying to read from an underlying FailingStream should have thrown an exception");
        } catch (IOException ioe) {
            assertEquals("Exception thrown on read() should have the right message",
                         MockInputStream.READ_FAILURE_MESSAGE,
                         ioe.getMessage());
        } finally {
            stream.close();
            assertTrue("Underlying stream should have been closed", mockStream.wasCloseCalled());
        }
    }

}
