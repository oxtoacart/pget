package org.oxcart.streams;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;
import org.oxcart.streams.ChecksummingInputStream;

/**
 * @author ox.to.a.cart /at/ gmail.com
 * 
 */
public class ChecksummingInputStreamTest extends StreamTest {

    @Test
    public void testSHA256() throws Exception {
        MessageDigest digester = MessageDigest.getInstance("SHA-256");
        byte[] expectedDigest = digester.digest(bunchOfBytes);
        ChecksummingInputStream stream = new ChecksummingInputStream(
                                                                     new ByteArrayInputStream(bunchOfBytes),
                                                                     "SHA-256",
                                                                     expectedDigest);
        while (stream.read() != -1) {
            // Loop through the stream
        }
        assertTrue(stream.valid());
        stream.close();
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

    // TODO: add failure mode test cases

}
