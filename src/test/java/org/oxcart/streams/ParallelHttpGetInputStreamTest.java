package org.oxcart.streams;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class ParallelHttpGetInputStreamTest extends BaseHTTPGetTest {
    @Test
    public void testSuccess() throws Exception {
        ParallelHttpGetInputStream stream = new ParallelHttpGetInputStream(executorService,
                                                                           "http://localhost:8089/good1",
                                                                           "http://localhost:8089/good2");
        try {
            String result = IOUtils.toString(stream);
            assertTrue("Stream should be valid", stream.isValid());
            assertEquals("Result should contain all the right characters", CONTENT, result);

            // Report progress
            MockProgressRecorder recorder = new MockProgressRecorder();
            stream.reportProgress(recorder);
            assertTrue(recorder.recordedNames.contains("http://localhost:8089/good1"));
            assertTrue(recorder.recordedNames.contains("http://localhost:8089/good2"));
            assertTrue(recorder.recordedCategories.contains("Buffered"));
            assertTrue(recorder.recordedTotals.contains(10.0d));
            assertTrue(recorder.recordedProgress.contains(10.0d));
        } finally {
            stream.close();
        }
    }

    @Test
    public void testCorrupted() throws Exception {
        ParallelHttpGetInputStream stream = new ParallelHttpGetInputStream(executorService,
                                                                           "http://localhost:8089/good1",
                                                                           "http://localhost:8089/corrupted");
        try {
            String result = IOUtils.toString(stream);
            assertFalse("Stream should not be valid", stream.isValid());
            assertEquals("There should be 1 validation error", 1, stream.getValidationErrors().size());
            String expectedValidationError = String.format("WARNING - Amount of read content did not match expected content length, data may be corrupted.  Expected %1$s, read %2$s",
                                                           20, 19);
            assertEquals("Validation error message should contain right byte counts",
                         expectedValidationError,
                         stream.getValidationErrors().get(0));
            assertEquals("Result should contain only the sent characters",
                         CONTENT.substring(0, 19), result);
        } finally {
            stream.close();
        }
    }

    @Test
    public void testMismatchedLength() throws Exception {
        try {
            new ParallelHttpGetInputStream(executorService,
                                           "http://localhost:8089/good1",
                                           "http://localhost:8089/bad-length");
            fail("Mismatched Content-Length header should have prevented initialization of the stream");
        } catch (IOException ioe) {
            // ignore
        }
    }

    @Test
    public void testDisallowedRange() throws Exception {
        try {
            new ParallelHttpGetInputStream(executorService,
                                           "http://localhost:8089/good1",
                                           "http://localhost:8089/bad-range");
            fail("Disallowed Range requests header should have prevented initialization of the stream");
        } catch (IOException ioe) {
            // ignore
        }
    }

    private static class MockProgressRecorder implements IProgressRecorder {
        private Set<String> recordedNames = new HashSet<String>();
        private Set<String> recordedCategories = new HashSet<String>();
        private Set<Double> recordedTotals = new HashSet<Double>();
        private Set<Double> recordedProgress = new HashSet<Double>();

        @Override
        public void record(String name, String category, double total, double progress) {
            recordedNames.add(name);
            recordedCategories.add(category);
            recordedTotals.add(total);
            recordedProgress.add(progress);
        }
    }
}
