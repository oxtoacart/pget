package org.oxcart.streams;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import org.junit.Before;
import org.junit.Rule;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

/**
 * Base class for tests that need an actual HTTP server listening. The HTTP server is mocked using <a
 * href="http://wiremock.org">WireMock</a>.
 * 
 * @author ox.to.a.cart /at/ gmail.com
 * 
 */
public class BaseHTTPGetTest extends StreamTest {
    public static final String CONTENT_PART_1 = "1234567890";
    public static final String CONTENT_PART_2 = "ABCDEFGHIJ";
    public static final String CONTENT = CONTENT_PART_1 + CONTENT_PART_2;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Before
    public void setUpHttpMock() {
        stubFor(head(urlEqualTo("/good1")).willReturn(aResponse().withStatus(200)
                                                                 .withHeader("Transfer-Encoding", "")
                                                                 .withHeader("Content-Length", "20")));
        stubFor(head(urlEqualTo("/good2")).willReturn(aResponse().withStatus(200)
                                                                 .withHeader("Accept-Ranges", "bytes")
                                                                 .withHeader("Content-Length", "20")));
        stubFor(head(urlEqualTo("/corrupted")).willReturn(aResponse().withStatus(200)
                                                                     .withHeader("Content-Length", "20")));
        stubFor(head(urlEqualTo("/bad-length")).willReturn(aResponse().withStatus(2006)
                                                                      .withHeader("Content-Length", "21")));
        stubFor(head(urlEqualTo("/bad-range")).willReturn(aResponse().withStatus(200)
                                                                     .withHeader("Accept-Ranges", "none")
                                                                     .withHeader("Content-Length", "20")));
        stubFor(get(urlEqualTo("/good1"))
                                         .withHeader("Range", equalTo("bytes=0-9"))
                                         .willReturn(aResponse().withStatus(206)
                                                                .withHeader("Transfer-Encoding", "")
                                                                .withHeader("Content-Range", "bytes 0-9/10")
                                                                .withBody(CONTENT_PART_1)));
        stubFor(get(urlEqualTo("/good2"))
                                         .withHeader("Range", equalTo("bytes=10-19"))
                                         .willReturn(aResponse().withStatus(206)
                                                                .withHeader("Transfer-Encoding", "")
                                                                .withHeader("Content-Range", "bytes 10-19/10")
                                                                .withBody(CONTENT_PART_2)));
        stubFor(get(urlEqualTo("/corrupted"))
                                             .withHeader("Range", equalTo("bytes=10-19"))
                                             .willReturn(aResponse().withStatus(206)
                                                                    .withHeader("Transfer-Encoding", "")
                                                                    .withHeader("Content-Range", "bytes 10-19/10")
                                                                    .withBody(CONTENT_PART_2.substring(0, 9))));
    }

}
