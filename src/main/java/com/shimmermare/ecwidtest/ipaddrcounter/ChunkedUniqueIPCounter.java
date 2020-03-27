package com.shimmermare.ecwidtest.ipaddrcounter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * The chunked BitSet implementation of unique IP counter.
 *
 * <p>There's one known edge case where simple implementation is preferred: when there's not so much
 * addresses (<100k) and they uniformly distributed throughout the address space.
 *
 * <p>This implementation based on two ideas:
 * <li>1. There are only 2^32 of IPv4 addresses, that means we can use BitSet to check if address
 *     was encountered before. This way we use 1 bit per address, and in the worst case that gives
 *     us less than 600 MB for ALL address space. With naive implementation of set of strings, we
 *     would use not less than (16+16+30)*2^32 = 266 GB of memory only on strings!
 * <li>2. In real IP datasets, addresses tend to form islands, and it's possible that some parts of
 *     address space are completely empty. Instead of using one big BitSet, we could break it into
 *     chunks to reduce memory footprint.
 *
 * <p>Possible optimization: change IP parsing so it doesn't use additional objects and instead
 * parses chars. That will squeeze another 20% perf or so at the cost of complete unreadability.
 */
public final class ChunkedUniqueIPCounter implements UniqueIPCounter {
  private static final long IPv4_RANGE = 4_294_967_296L;
  private static final int BLOCK_SIZE = 4096;

  private final BufferedReader reader;
  private long currentLine;

  private final BitSet[] blocks;
  private long uniqueCount;

  public ChunkedUniqueIPCounter(InputStream in) {
    this.reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    this.blocks = new BitSet[(int) (IPv4_RANGE / BLOCK_SIZE)];
  }

  public long count() throws IOException {
    try {
      reader.lines().forEach(this::processLine);
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
    return uniqueCount;
  }

  private void processLine(String line) {
    long address;
    try {
      address = parseAddress(line);
    } catch (Exception e) {
      throw new IllegalArgumentException("Line " + currentLine + " is not a valid IPv4", e);
    }

    int blockIndex = (int) (address / BLOCK_SIZE);
    int indexInBlock = (int) (address % BLOCK_SIZE);

    // Create new block if wasn't used before
    BitSet block = blocks[blockIndex];
    if (block == null) {
      blocks[blockIndex] = block = new BitSet(BLOCK_SIZE);
    }

    // Only increment unique counter if bit at index wasn't set already.
    if (!block.get(indexInBlock)) {
      block.set(indexInBlock);
      uniqueCount++;
    }

    currentLine++;
  }

  private static long parseAddress(String line) {
    List<String> octets = linearSplit(line, '.');
    if (octets.size() != 4) {
      throw new IllegalArgumentException("IPv4 should have 4 octets separated by dots");
    }

    int octet0 = parseOctet(octets.get(0), 0);
    int octet1 = parseOctet(octets.get(1), 1);
    int octet2 = parseOctet(octets.get(2), 2);
    int octet3 = parseOctet(octets.get(3), 3);

    long address = 0;
    address |= ((long) octet0) << 24;
    address |= octet1 << 16;
    address |= octet2 << 8;
    address |= octet3;
    return address;
  }

  private static List<String> linearSplit(String string, char separator) {
    List<String> result = new ArrayList<>();
    int start = 0;
    for (int i = 0; i < string.length(); i++) {
      if (string.charAt(i) == separator) {
        result.add(string.substring(start, i));
        start = i + 1;
        i = i + 1;
        if (i >= string.length()) {
          break;
        }
      }
    }
    if (start < string.length()) {
      result.add(string.substring(start));
    }
    return result;
  }

  private static int parseOctet(String string, int octetPos) {
    int octet = Integer.parseInt(string);
    if (octet < 0 || octet > 255) {
      throw new IllegalArgumentException(octetPos + " octet is not a 8-bit integer");
    }
    return octet;
  }
}
