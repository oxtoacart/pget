package org.oxcart.streams;

/**
 * Callback for reporting progress.
 * 
 * @author ox.to.a.cart /at/ gmail.com
 * 
 */
public interface IProgressRecorder {
    /**
     * Report progress (e.g. by writing to System.err).
     * 
     * @param name
     *            name of the IProgressReporter
     * @param category
     *            category of the status being provided
     * @param total
     *            the total possible progress
     * @param progress
     *            progress up to this point
     */
    void record(String name, String category, double total, double progress);
}
