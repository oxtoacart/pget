pget
====

This utility provides a parallel HTTP GET that can download a single file from multiple locations.  To get help on the command, run:

pget -?

In addition to being a utility, the core functionality is also available for use as a library via the [ParallelHttpGetInputStream](#parallelhttpgetinputstream) class.

## Quick Start

This quick start shows you how to get pget, download a file from two separate locations in parallel and pipe it into /tmp/downloaded_file.m4v.

From a UNIX shell:

```bash
git clone https://github.com/oxtoacart/pget.git
cd pget
./pget -c 04a1b0fc8a98999c6f78b35df9d8296996b5f6107c1b7f179c26a0496895b03f \
    "https://lantern.s3.amazonaws.com/lantern-video-broadband.m4v" \
    "https://dl.dropboxusercontent.com/s/rxnpmdrs2jms193/lantern-video-broadband.m4v" \
    > /tmp/downloaded_file.m4v
```

## Reports and Documentation

[JavaDoc](http://oxtoacart.github.io/pget/javadoc/index.html)
[Test Reports](http://oxtoacart.github.io/pget/tests/index.html)
[Code Coverage](http://oxtoacart.github.io/pget/jacoco/test/html/index.html)

note - code coverage styling is currently broken.

## Building

pget is built using <a href="http://www.gradle.org/">Gradle</a>.

## [ParallelHttpGetInputStream](http://oxtoacart.github.io/pget/javadoc/org/oxcart/streams/ParallelHttpGetInputStream.html)

This class implements the parallel HTTP Get functionality and can be used as an InputStream by any program.

## [ReadAheadInputStream](http://oxtoacart.github.io/pget/javadoc/org/oxcart/streams/ReadAheadInputStream.html)

This class provides an InputStream that reads its content as fast as it can and buffers it locally.  This is useful for slow readers who may be temporarily blocking on some other I/O.  It is used by ParallelHttpGetInputStream to fetch the multiple parts of the file in the background.

## [MergedInputStream](http://oxtoacart.github.io/pget/javadoc/org/oxcart/streams/MergedInputStream.html)

This InputStream merges multiple independent InputStreams.  It is used by ParallelHttpGetInputStream to reassemble the portions of the file that were fetched in parallel.

## [ChecksummingInputStream](http://oxtoacart.github.io/pget/javadoc/org/oxcart/streams/ChecksummingInputStream.html)

This InputStream calculates a checksum of data consumed out of an underlying InputStream and validates the checksum against a provided value when finished.

## Dependencies

See <a href="build.gradle">build.gradle</a> for all dependencies.

## Acknowledgments
The following articles were helpful in writing pget.

Multi-Threading with HTTPClient - http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d5e639

HTTP HEAD Request with HTTPClient - http://stackoverflow.com/questions/7822432/how-to-implement-the-head-method-of-httpclient

HTTP Range Requests - http://stackoverflow.com/questions/8293687/sample-http-range-request-session

HTTP Accept-Ranges Header - http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.5

Setting up GitHub Pages - https://help.github.com/articles/creating-project-pages-manually