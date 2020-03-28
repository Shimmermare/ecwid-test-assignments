package com.shimmermare.ecwidtest.test.ipaddrcounter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shimmermare.ecwidtest.ipaddrcounter.ChunkedUniqueIPCounter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ChunkedUniqueIPCounterTests {
  @Test
  public void counts() throws IOException {
    String input = "0.0.0.0\n1.1.1.1\n2.2.2.2\n255.255.255.255\n1.1.1.1";
    InputStream in = new ByteArrayInputStream(input.getBytes());

    ChunkedUniqueIPCounter counter = new ChunkedUniqueIPCounter(in);
    assertEquals(4, counter.count());

    in.close();
  }

  @Test
  public void breaksOnBadFormat() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          String input = "1.1.1_1";
          try (InputStream in = new ByteArrayInputStream(input.getBytes())) {
            ChunkedUniqueIPCounter counter = new ChunkedUniqueIPCounter(in);
            counter.count();
          }
        });
  }

  @Test
  public void breaksOnTooBigOctets() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          String input = "1.1.1.256";
          try (InputStream in = new ByteArrayInputStream(input.getBytes())) {
            ChunkedUniqueIPCounter counter = new ChunkedUniqueIPCounter(in);
            counter.count();
          }
        });
  }

  @Test
  public void breaksOnTooSmallOctets() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          String input = "1.1.1.-1";
          try (InputStream in = new ByteArrayInputStream(input.getBytes())) {
            ChunkedUniqueIPCounter counter = new ChunkedUniqueIPCounter(in);
            counter.count();
          }
        });
  }

  @Test
  public void randomizedStreamTest() throws IOException {
    Random random = new Random();

    String[] randomized = new String[10000];
    for (int i = 0; i < randomized.length; i++) {
      // Force repetitions
      if (random.nextFloat() > 0.95F) {
        randomized[i] = randomized[random.nextInt(i)];
        continue;
      }
      randomized[i] =
          random.nextInt(256)
              + "."
              + random.nextInt(256)
              + "."
              + random.nextInt(256)
              + "."
              + random.nextInt(256);
    }

    String input = String.join("\n", randomized);
    InputStream in = new ByteArrayInputStream(input.getBytes());

    Set<String> uniques = new HashSet<>();
    long startHashSet = System.currentTimeMillis();
    uniques.addAll(Arrays.asList(randomized));
    long endHashSet = System.currentTimeMillis();

    ChunkedUniqueIPCounter counter = new ChunkedUniqueIPCounter(in);
    long startCounter = System.currentTimeMillis();
    long count = counter.count();
    long endCounter = System.currentTimeMillis();
    in.close();

    System.out.println("Uniques: " + uniques.size());
    System.out.println("HashSet time: " + (endHashSet - startHashSet) + " ms");
    System.out.println("Counter time: " + (endCounter - startCounter) + " ms");
    assertEquals(uniques.size(), count);
  }
}
