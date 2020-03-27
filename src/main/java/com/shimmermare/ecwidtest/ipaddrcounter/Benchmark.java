package com.shimmermare.ecwidtest.ipaddrcounter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import org.openjdk.jol.info.GraphLayout;

/**
 * Quick and dirty benchmark to test execution time and memory footprint. Results aren't 100%
 * reliable and can only show the general side of things.
 *
 * <p>Note: OpenJDK JOL is used for finding object size. Building object graph with it can be quite
 * slow and memory-intensive.
 */
public final class Benchmark {
  public static void main(String[] args) throws IOException, InterruptedException {
    createRandomIPv4List("ipv4list.txt", 10_000_000);
    List<String> jvmArgs = List.of("-Djdk.attach.allowAttachSelf", "-Xmx8G");
    exec(Chunked.class, jvmArgs, Collections.emptyList());
    Thread.sleep(20000L);
    exec(Naive.class, jvmArgs, Collections.emptyList());
  }

  private static void bench(Function<InputStream, UniqueIPCounter> counterConstructor, String file)
      throws IOException {
    long start = System.currentTimeMillis();
    InputStream in = new FileInputStream(file);
    UniqueIPCounter counter = counterConstructor.apply(in);
    long count = counter.count();
    in.close();
    long end = System.currentTimeMillis();

    System.out.println("Count: " + count);
    System.out.println("Time: " + (end - start) + "ms");
    System.out.println(
        "Used memory: " + (GraphLayout.parseInstance(counter).totalSize() / 1_000_000) + "MB");
  }

  public static int exec(Class<?> clazz, List<String> jvmArgs, List<String> args)
      throws IOException, InterruptedException {
    String javaHome = System.getProperty("java.home");
    String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
    String classpath = System.getProperty("java.class.path");
    String className = clazz.getName();
    List<String> command = new ArrayList<>();
    command.add(javaBin);
    command.addAll(jvmArgs);
    command.add("-cp");
    command.add(classpath);
    command.add(className);
    command.addAll(args);
    ProcessBuilder builder = new ProcessBuilder(command);
    Process process = builder.inheritIO().start();
    process.waitFor();
    return process.exitValue();
  }

  private static void createRandomIPv4List(String file, long count) throws IOException {
    Path path = Path.of(file);
    Files.deleteIfExists(path);
    Files.createFile(path);
    BufferedWriter writer = new BufferedWriter(new FileWriter(path.toString()));

    Random random = new Random();
    for (long i = 0; i < count; i++) {
      writer.write(
          random.nextInt(256)
              + "."
              + random.nextInt(256)
              + "."
              + random.nextInt(256)
              + "."
              + random.nextInt(256));
      writer.newLine();
    }
    writer.close();
  }

  public static class Naive {
    public static void main(String[] args) throws IOException {
      System.out.println("Benching naive:");
      bench(NaiveUniqueIPCounter::new, "ipv4list.txt");
    }
  }

  public static class Chunked {
    public static void main(String[] args) throws IOException {
      System.out.println("Benching chunked:");
      bench(ChunkedUniqueIPCounter::new, "ipv4list.txt");
    }
  }
}
