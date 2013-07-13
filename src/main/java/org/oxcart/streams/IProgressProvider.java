package org.oxcart.streams;

/**
 * Interface for objects that can provide progress updates.
 * 
 * @author ox.to.a.cart /at/ gmail.com
 * 
 */
public interface IProgressProvider {
    /**
     * Called to provide progress. This method needs to be thread-safe.
     * 
     * @param recorder
     */
    void provideProgress(IProgressReporter recorder);
}
