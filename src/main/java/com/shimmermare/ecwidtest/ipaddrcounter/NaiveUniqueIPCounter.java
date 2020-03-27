package com.shimmermare.ecwidtest.ipaddrcounter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Naive implementation; only for benchmarking.
 */
public final class NaiveUniqueIPCounter implements UniqueIPCounter {
  private final BufferedReader reader;

  private final Set<String> uniques;

  public NaiveUniqueIPCounter(InputStream in) {
    this.reader = new BufferedReader(new InputStreamReader(in));
    this.uniques = new HashSet<>();
  }

  @Override
  public long count() throws IOException {
    try {
      reader.lines().forEach(uniques::add);
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
    return uniques.size();
  }
}
