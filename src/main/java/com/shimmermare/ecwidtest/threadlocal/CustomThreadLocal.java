package com.shimmermare.ecwidtest.threadlocal;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Thread Local variable storage.
 *
 * <p>Optimization concern: thread local map is externally synchronized, that means it's fully
 * blocked. Throughput will be less than of ConcurrentHashMap.
 */
public class CustomThreadLocal<T> {
  private Map<Thread, T> locals;

  public CustomThreadLocal() {
    this.locals = Collections.synchronizedMap(new WeakHashMap<>());
  }

  public T get() {
    return locals.get(Thread.currentThread());
  }

  public void set(T local) {
    locals.put(Thread.currentThread(), local);
  }

  public void remove() {
    locals.remove(Thread.currentThread());
  }
}
