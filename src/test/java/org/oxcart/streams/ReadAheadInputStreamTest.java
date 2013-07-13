package org.oxcart.streams;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ox.to.a.cart /at/ gmail.com
 * 
 */
public class ReadAheadInputStreamTest extends StreamTest {

    private ExecutorService executorService;

    @Before
    public void setUp() {
        executorService = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() {
        // Shut down our ExecutorService so the program can end
        executorService.shutdownNow();
    }

    @Test
    public void testSuccess() throws Exception {
        ReadAheadInputStream inputStream = ReadAheadInputStream.open(new IStreamProvider() {
            public InputStream openStream() {
                return new ByteArrayInputStream(bunchOfBytes);
            }
        }, executorService);
        byte[] result = toByteArray(inputStream);

        // Check the contents of the read array
        assertArrayEquals("Result of reading from ReadAheadInputStream should match original data",
                          bunchOfBytes, result);

        // Check the buffer file
        File bufferFile = getBufferFile(inputStream);

        assertTrue("Buffer file should exist before closing stream",
                   bufferFile.exists());

        assertEquals("Length of buffer file should match original data",
                     bufferFile.length(), bunchOfBytes.length);

        inputStream.close();
        assertFalse("Buffer file should be cleaned up by closing stream",
                    bufferFile.exists());

        assertEquals("By end, all bytes should be buffered", bunchOfBytes.length, inputStream.getBufferedBytes());
    }

    @Test
    public void testCloseFailure() throws Exception {
        final MockInputStream mockStream = new MockInputStream(false, true);
        ReadAheadInputStream stream = ReadAheadInputStream.open(new IStreamProvider() {
            public InputStream openStream() throws IOException {
                return mockStream;
            }
        }, executorService);
        try {
            while (stream.read() != -1) {
                // Read as far as we can
            }
            fail("Exception on close of underlying stream (during buffering) should have propagated");
        } catch (IOException ioe) {
            assertEquals("Exception thrown on close of underlying stream (during buffering) should have the right message",
                         MockInputStream.CLOSE_FAILURE_MESSAGE,
                         ioe.getMessage());
        }
    }

    @Test
    public void testReadFailure() throws Exception {
        final MockInputStream mockStream = new MockInputStream(true, true);
        ReadAheadInputStream stream = ReadAheadInputStream.open(new IStreamProvider() {
            public InputStream openStream() throws IOException {
                return mockStream;
            }
        }, executorService);
        try {
            while (stream.read() != -1) {
                // Read as far as we can
            }
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

    private File getBufferFile(ReadAheadInputStream inputStream) throws Exception {
        Field fileField = ReadAheadInputStream.class.getDeclaredField("bufferFile");
        fileField.setAccessible(true);
        return (File) fileField.get(inputStream);
    }
}
