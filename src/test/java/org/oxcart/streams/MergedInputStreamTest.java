package org.oxcart.streams;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

/**
 * @author ox.to.a.cart /at/ gmail.com
 * 
 */
public class MergedInputStreamTest extends StreamTest {

    @Test
    public void testSuccess() throws Exception {
        int splitPoint1 = bunchOfBytes.length / 3;
        int splitPoint2 = splitPoint1 * 2;
        ByteArrayInputStream segment1 = new ByteArrayInputStream(Arrays.copyOfRange(bunchOfBytes,
                                                                                    0,
                                                                                    splitPoint1));
        ByteArrayInputStream segment2 = new ByteArrayInputStream(Arrays.copyOfRange(bunchOfBytes,
                                                                                    splitPoint1,
                                                                                    splitPoint2));
        ByteArrayInputStream segment3 = new ByteArrayInputStream(Arrays.copyOfRange(bunchOfBytes,
                                                                                    splitPoint2,
                                                                                    bunchOfBytes.length));

        MergedInputStream inputStream = new MergedInputStream(segment1, segment2, segment3);
        byte[] actualResult = toByteArray(inputStream);

        assertArrayEquals("Result of reading from MergedInputStream should match original bytes",
                          bunchOfBytes, actualResult);

        assertEquals("Reported bytes should match actual bytes read", inputStream.getBytesRead(), actualResult.length);
        inputStream.close();
    }

    @Test
    public void testClose() throws Exception {
        MockInputStream[] mockStreams = new MockInputStream[] {
                new MockInputStream(false, false),
                new MockInputStream(false, false),
                new MockInputStream(false, false)
        };
        MergedInputStream stream = new MergedInputStream(mockStreams);
        stream.close();

        assertTrue("The first underlying stream should have been closed", mockStreams[0].wasCloseCalled());
        assertTrue("The second underlying stream should have been closed", mockStreams[1].wasCloseCalled());
        assertTrue("The third underlying stream should have been closed", mockStreams[2].wasCloseCalled());
    }

    @Test
    public void testCloseFailure() throws Exception {
        MockInputStream[] mockStreams = new MockInputStream[] {
                new MockInputStream(false, true),
                new MockInputStream(false, true),
                new MockInputStream(false, true)
        };
        MergedInputStream stream = new MergedInputStream(mockStreams);
        try {
            stream.close();
            fail("Exception on underlying close should have propagated");
        } catch (IOException ioe) {
            assertEquals("Exception thrown on close() should have the right message",
                         MockInputStream.CLOSE_FAILURE_MESSAGE,
                         ioe.getMessage());
            assertTrue("The first underlying stream should have been closed", mockStreams[0].wasCloseCalled());
            assertTrue("The second underlying stream should have been closed", mockStreams[1].wasCloseCalled());
            assertTrue("The third underlying stream should have been closed", mockStreams[2].wasCloseCalled());
        }
    }

    @Test
    public void testReadFailure() throws Exception {
        MockInputStream[] mockStreams = new MockInputStream[] {
                new MockInputStream(true, false),
                new MockInputStream(true, false),
                new MockInputStream(true, false)
        };
        MergedInputStream stream = new MergedInputStream(mockStreams);
        try {
            stream.read();
            fail("Trying to read from an underlying FailingStream should have thrown an exception");
        } catch (IOException ioe) {
            assertEquals("Exception thrown on read() should have the right message",
                         MockInputStream.READ_FAILURE_MESSAGE,
                         ioe.getMessage());
        } finally {
            stream.close();
            assertTrue("The first underlying stream should have been closed", mockStreams[0].wasCloseCalled());
            assertTrue("The second underlying stream should have been closed", mockStreams[1].wasCloseCalled());
            assertTrue("The third underlying stream should have been closed", mockStreams[2].wasCloseCalled());
        }
    }
}
