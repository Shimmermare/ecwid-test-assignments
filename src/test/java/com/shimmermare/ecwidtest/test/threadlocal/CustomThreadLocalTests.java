package com.shimmermare.ecwidtest.test.threadlocal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.util.ReflectionUtils.tryToReadFieldValue;

import com.shimmermare.ecwidtest.threadlocal.CustomThreadLocal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.function.Try;

public class CustomThreadLocalTests {
  @Test
  public void setsGetsRemoves() {
    CustomThreadLocal<Object> threadLocal = new CustomThreadLocal<>();

    Object obj = new Object();

    assertNull(threadLocal.get());
    threadLocal.set(obj);
    assertEquals(obj, threadLocal.get());
    threadLocal.remove();
    assertNull(threadLocal.get());
  }

  @Test
  public void localForThreads() throws Throwable {
    CustomThreadLocal<Object> threadLocal = new CustomThreadLocal<>();
    Object obj1 = new Object();
    Object obj2 = new Object();
    ExecutorService thread1 = Executors.newSingleThreadExecutor();
    ExecutorService thread2 = Executors.newSingleThreadExecutor();

    // Set locals for threads and wait before both are finished.
    Future<?> setLocal1 = thread1.submit(() -> threadLocal.set(obj1));
    Future<?> setLocal2 = thread2.submit(() -> threadLocal.set(obj2));
    try {
      setLocal1.get();
    } catch (ExecutionException e) {
      throw e.getCause();
    }
    try {
      setLocal2.get();
    } catch (ExecutionException e) {
      throw e.getCause();
    }

    // Assert that threads have the correct value.
    Future<?> assertLocal1 = thread1.submit(() -> assertEquals(obj1, threadLocal.get()));
    Future<?> assertLocal2 = thread2.submit(() -> assertEquals(obj2, threadLocal.get()));
    try {
      assertLocal1.get();
    } catch (ExecutionException e) {
      throw e.getCause();
    }
    try {
      assertLocal2.get();
    } catch (ExecutionException e) {
      throw e.getCause();
    }
  }

  @Test
  public void doesntPreventGCOfThreads() throws InterruptedException {
    CustomThreadLocal<Object> threadLocal = new CustomThreadLocal<>();

    Try<Object> threadLocalMapTry =
        tryToReadFieldValue(CustomThreadLocal.class, "locals", threadLocal);
    @SuppressWarnings("unchecked")
    Map<Thread, Object> threadLocalMap =
        (Map<Thread, Object>)
            threadLocalMapTry.getOrThrow(
                (e) -> new IllegalStateException("Can't find thread local map", e));

    Thread thread = new Thread(() -> threadLocal.set(new Object()));
    thread.start();
    thread.join();
    thread = null;

    assertFalse(threadLocalMap.isEmpty());

    System.gc();

    // Unfortunately WeakHashMap isn't immediately cleared after thread is GCed.
    // One solution that should work in 99.(9)% cases is to wait until the map is cleaned.
    // A little bit dirty, but working.
    long startedWaiting = System.currentTimeMillis();
    while (!threadLocalMap.isEmpty()) {
      if (System.currentTimeMillis() - startedWaiting > 10000) {
        throw new IllegalStateException("Thread local map wasn't cleared in 10 seconds");
      }
    }

    assertTrue(threadLocalMap.isEmpty());
  }

  @Test
  public void concurrent() throws Throwable {
    ExecutorService executor = Executors.newFixedThreadPool(128);
    Map<Thread, Object> currentValues = new ConcurrentHashMap<>();

    CustomThreadLocal<Object> threadLocal = new CustomThreadLocal<>();

    List<Future<?>> tasks = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      for (int j = 0; j < 128; j++) {
        Future<?> task =
            executor.submit(
                () -> {
                  Thread currentThread = Thread.currentThread();
                  Object currentValue = currentValues.get(currentThread);
                  if (currentValue != null) {
                    assertEquals(currentValue, threadLocal.get());
                  }
                  Object newValue = new Object();
                  currentValues.put(currentThread, newValue);
                  threadLocal.set(newValue);
                });
        tasks.add(task);
      }
    }
    for (Future<?> task : tasks) {
      try {
        task.get();
      } catch (ExecutionException e) {
        throw e.getCause();
      }
    }
  }
}
