package org.oxcart.streams;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * Base class for tests on streams.
 * 
 * @author ox.to.a.cart /at/ gmail.com
 * 
 */
public abstract class StreamTest {
    protected final byte[] bunchOfBytes;

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
