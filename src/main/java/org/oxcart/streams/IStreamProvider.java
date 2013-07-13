package org.oxcart.streams;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface for objects that can provide a stream.
 * 
 * @author ox.to.a.cart /at/ gmail.com
 * 
 */
public interface IStreamProvider {
    InputStream openStream() throws IOException;
}
