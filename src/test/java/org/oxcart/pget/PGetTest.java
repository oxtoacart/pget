package org.oxcart.pget;

import static org.junit.Assert.*;

import java.io.File;
import java.security.MessageDigest;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.oxcart.streams.BaseHTTPGetTest;

public class PGetTest extends BaseHTTPGetTest {
    private static final String OUT_FILE_NAME = "/tmp/PGetTest_outfile.tmp";

    @Before
    public void deleteOutFile() {
        File outFile = new File(OUT_FILE_NAME);
        if (outFile.exists()) {
            outFile.delete();
        }
    }

    @Test
    public void testSuccessToStandardout() throws Exception {
        int fetchResult = new PGet("http://localhost:8089/good1",
                                   "http://localhost:8089/good2").fetch();
        assertEquals("Fetch should be successful", 0, fetchResult);
    }

    @Test
    public void testSuccessToFileWithoutChecksum() throws Exception {
        int fetchResult = new PGet("-o", OUT_FILE_NAME,
                                   "http://localhost:8089/good1",
                                   "http://localhost:8089/good2").fetch();
        assertEquals("Fetch should be successful", 0, fetchResult);
        checkSavedFile();
    }

    @Test
    public void testSuccessToFileWithChecksum() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String checksum = Hex.encodeHexString(digest.digest(CONTENT.getBytes()));
        int fetchResult = new PGet("-o", OUT_FILE_NAME,
                                   "-c", checksum,
                                   "http://localhost:8089/good1",
                                   "http://localhost:8089/good2").fetch();
        assertEquals("Fetch should be successful", 0, fetchResult);
        checkSavedFile();
    }

    @Test
    public void testChecksumFailure() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String checksum = Hex.encodeHexString(digest.digest(CONTENT.substring(0, 19).getBytes()));
        int fetchResult = new PGet("-o", OUT_FILE_NAME,
                                   "-c", checksum,
                                   "http://localhost:8089/good1",
                                   "http://localhost:8089/good2").fetch();
        assertEquals("Fetch should have failed checksum", 2, fetchResult);
    }

    @Test
    public void testSuccessToFileWithInvalidChecksum() throws Exception {
        int fetchResult = new PGet("-o", OUT_FILE_NAME,
                                   "-c", "Z3523$$%@#%",
                                   "http://localhost:8089/good1",
                                   "http://localhost:8089/good2").fetch();
        assertEquals("Fetch should be successful", 0, fetchResult);
        checkSavedFile();
    }

    @Test
    public void testFailureBecauseOfBadFile() throws Exception {
        int fetchResult = new PGet("-o", "/tmp/folder/bad_outfile_name",
                                   "http://localhost:8089/good1",
                                   "http://localhost:8089/good2").fetch();
        assertEquals("Fetch should have failed because of bad file", 3, fetchResult);
    }

    @Test
    public void testHelp() throws Exception {
        int fetchResult = new PGet("--help").fetch();
        assertEquals("Fetch should be successful", 0, fetchResult);
    }

    @Test
    public void testMissingUrls() throws Exception {
        int fetchResult = new PGet().fetch();
        assertEquals("Fetch should have failed", 1, fetchResult);
    }

    private void checkSavedFile() throws Exception {
        String savedString = FileUtils.readFileToString(new File(OUT_FILE_NAME));
        assertEquals("Saved file's content didn't match original content", CONTENT, savedString);
    }

}
