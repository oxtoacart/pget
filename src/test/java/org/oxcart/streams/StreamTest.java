package org.oxcart.streams;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;

/**
 * Base class for tests on streams.
 * 
 * @author ox.to.a.cart /at/ gmail.com
 * 
 */
public abstract class StreamTest {
    protected final byte[] bunchOfBytes;
    protected ExecutorService executorService;

    @Before
    public void setUpExecutorService() {
        executorService = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDownExecutorService() {
        // Shut down our ExecutorService so the program can end
        executorService.shutdownNow();
    }

    public StreamTest() {
        bunchOfBytes = new byte[50000];
        for (int i = 0; i < bunchOfBytes.length; i++) {
            bunchOfBytes[i] = (byte) (i % 255);
        }
    }

    protected byte[] toByteArray(InputStream inputStream) throws IOException {
        return IOUtils.toByteArray(inputStream);
    }
}
