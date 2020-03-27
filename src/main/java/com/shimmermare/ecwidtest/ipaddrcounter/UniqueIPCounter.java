package com.shimmermare.ecwidtest.ipaddrcounter;

import java.io.IOException;

/**
 * Count unique IPv4 addresses from input stream where each address is on it's own line.
 *
 * @see ChunkedUniqueIPCounter
 */
public interface UniqueIPCounter {
  long count() throws IOException;
}
