package org.oxcart.streams;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * An InputStream that can validate itself.
 * 
 * @author ox.to.a.cart /at/ gmail.com
 * 
 */
public abstract class IValidatingInputStream extends InputStream {
    private List<String> validationErrors = new ArrayList<String>();

    /**
     * Check if this stream is valid.
     * 
     * @return
     */
    public final boolean valid() {
        validationErrors.clear();
        collectValidationErrors(validationErrors);
        return validationErrors.size() == 0;
    }

    /**
     * Get any validation errors (make sure to call valid() first).
     * 
     * @return
     */
    public List<String> getValidationErrors() {
        return validationErrors;
    }

    /**
     * Implement this and record any validation errors in list.
     * 
     * @param validationErrors
     */
    protected abstract void collectValidationErrors(List<String> validationErrors);

}
