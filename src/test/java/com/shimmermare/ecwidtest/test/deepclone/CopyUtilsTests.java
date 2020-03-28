package com.shimmermare.ecwidtest.test.deepclone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shimmermare.ecwidtest.deepclone.CopyUtils;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import org.junit.jupiter.api.Test;

public class CopyUtilsTests {
  @Test
  public void doesntCopiesImmutable() {
    BigInteger original = BigInteger.valueOf(123);
    BigInteger copy = CopyUtils.deepCopy(original);
    assertEquals(original, copy);
    assertSame(original, copy);
  }

  @Test
  public void copiesSimple() {
    Simple original = new Simple(10, "simple", new long[] {1, 2, 3}, new String[] {"1", "2", "3"});
    Simple copy = CopyUtils.deepCopy(original);
    assertEquals(original, copy);
    assertNotSame(original, copy);
  }

  @Test
  public void copiesSelfReference() {
    SelfReference original = new SelfReference();
    original.selfReference = original;
    SelfReference copy = CopyUtils.deepCopy(original);
    assertNotSame(original, copy);
    assertSame(original, original.selfReference);
    assertSame(copy, copy.selfReference);
  }

  @Test
  public void copiesWithoutDuplicates() {
    Duplicates original = new Duplicates();
    original.target = new Duplicates.Target();
    original.inner = new Duplicates.Inner();
    original.inner.target = original.target;

    Duplicates copy = CopyUtils.deepCopy(original);

    assertNotSame(original, copy);
    assertSame(original.target, original.inner.target);
    assertSame(copy.target, copy.inner.target);
    assertNotSame(original.target, copy.target);
  }

  @Test
  public void failsWhenNoSuitableConstructor() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          NoSuitableConstructor original = new NoSuitableConstructor(1);
          CopyUtils.deepCopy(original);
        });
  }

  private static class Simple {
    private Object nullField;
    private int intField;
    private String stringField;
    private long[] primitiveArrayField;
    private String[] objectArrayField;

    public Simple() {}

    public Simple(
        int intField, String stringField, long[] primitiveArrayField, String[] objectArrayField) {
      this.intField = intField;
      this.stringField = stringField;
      this.primitiveArrayField = primitiveArrayField;
      this.objectArrayField = objectArrayField;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Simple simple = (Simple) o;
      return nullField == simple.nullField
          && intField == simple.intField
          && Objects.equals(stringField, simple.stringField)
          && Arrays.equals(primitiveArrayField, simple.primitiveArrayField)
          && Arrays.equals(objectArrayField, simple.objectArrayField);
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(intField, stringField);
      result = 31 * result + Arrays.hashCode(primitiveArrayField);
      result = 31 * result + Arrays.hashCode(objectArrayField);
      return result;
    }
  }

  private static class SelfReference {
    private SelfReference selfReference;

    public SelfReference() {}
  }

  private static class Duplicates {
    private Target target;
    private Inner inner;

    private static class Inner {
      private Target target;
    }

    private static class Target {
      private int field;
    }
  }

  private static class NoSuitableConstructor {
    private int field;

    public NoSuitableConstructor(int field) {
      throw new IllegalArgumentException();
    }
  }
}
