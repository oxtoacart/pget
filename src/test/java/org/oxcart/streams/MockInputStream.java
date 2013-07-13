package org.oxcart.streams;

import java.io.IOException;
import java.io.InputStream;

/**
 * An input streams that fails on anything and everything you try to do with it.
 * 
 * @author ox.to.a.cart /at/ gmail.com
 * 
 */
public class MockInputStream extends InputStream {
    public static String READ_FAILURE_MESSAGE = "I fail because that's who I am!";
    public static String CLOSE_FAILURE_MESSAGE = "I'm such a failure that I can't even close without failing :(";

    private boolean failOnRead = false;
    private boolean failOnClose = false;
    private boolean closeCalled = false;

    public MockInputStream(boolean failOnRead, boolean failOnClose) {
        super();
        this.failOnRead = failOnRead;
        this.failOnClose = failOnClose;
    }

    @Override
    public int read() throws IOException {
        if (failOnRead) {
            throw new IOException(READ_FAILURE_MESSAGE);
        } else {
            return -1;
        }
    }

    @Override
    public void close() throws IOException {
        closeCalled = true;
        if (failOnClose) {
            throw new IOException(CLOSE_FAILURE_MESSAGE);
        }
    }

    public boolean wasCloseCalled() {
        return closeCalled;
    }
}
