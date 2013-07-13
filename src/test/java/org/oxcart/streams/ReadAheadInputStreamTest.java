package org.oxcart.streams;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.oxcart.streams.IStreamProvider;
import org.oxcart.streams.ReadAheadInputStream;

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
        Field fileField = ReadAheadInputStream.class.getDeclaredField("bufferFile");
        fileField.setAccessible(true);
        File bufferFile = (File) fileField.get(inputStream);

        assertTrue("Buffer file should exist before closing stream",
                   bufferFile.exists());

        assertEquals("Length of buffer file should match original data",
                     bufferFile.length(), bunchOfBytes.length);

        inputStream.close();
        assertFalse("Buffer file should be cleaned up by closing stream",
                    bufferFile.exists());
    }

    // TODO: add failure mode test cases

}
