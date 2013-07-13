package org.oxcart.streams;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import org.junit.Test;
import org.oxcart.streams.MergedInputStream;

/**
 * @author ox.to.a.cart /at/ gmail.com
 * 
 */
public class MergedInputStreamTest extends StreamTest {

    @Test
    public void testSuccess() throws Exception {
        int splitPoint1 = bunchOfBytes.length / 3;
        int splitPoint2 = splitPoint1 * 2;
        ByteArrayInputStream segment1 = new ByteArrayInputStream(Arrays.copyOfRange(bunchOfBytes,
                                                                                    0,
                                                                                    splitPoint1));
        ByteArrayInputStream segment2 = new ByteArrayInputStream(Arrays.copyOfRange(bunchOfBytes,
                                                                                    splitPoint1,
                                                                                    splitPoint2));
        ByteArrayInputStream segment3 = new ByteArrayInputStream(Arrays.copyOfRange(bunchOfBytes,
                                                                                    splitPoint2,
                                                                                    bunchOfBytes.length));

        MergedInputStream inputStream = new MergedInputStream(segment1, segment2, segment3);
        byte[] actualResult = toByteArray(inputStream);

        assertArrayEquals("Result of reading from MergedInputStream should match original bytes",
                          bunchOfBytes, actualResult);

        assertEquals("Reported bytes should match actual bytes read", inputStream.getBytesRead(), actualResult.length);
    }

    // TODO: add failure mode test cases
}
