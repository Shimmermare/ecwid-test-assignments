package com.shimmermare.ecwidtest.deepclone;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.IdentityHashMap;
import java.util.Set;

public final class CopyUtils {
  // Types that should not be copied.
  private static final Set<Class<?>> IMMUTABLE_TYPES = createImmutableTypesSet();

  private CopyUtils() {}

  /**
   * Deep copy object.
   *
   * <p>Types from {@link #IMMUTABLE_TYPES} are not cloned and the reference is simply copied. E.g.
   * boxed types and String. <br>
   *
   * <p>All copied objects are added to the copy context. If there was multiple references to the
   * same object in the hierarchy, a copied instance will be reused from the context. <br>
   * For example, object A has a field that references object X, and a field that references object
   * B. Object B also has field that references object X. In this case, object X will be copied only
   * once, and for object's B field the reference will be reused from copy context.
   *
   * <p>If an object in the hierarchy doesn't have default constructor OR at least one constructor
   * that successfully creates object when all passed parameters are null - entire copying fails.
   * <br>
   * I honestly don't think there's another way to create instance without Cloneable, Serializable,
   * and JVM internal tools.
   */
  public static <T> T deepCopy(T object) throws IllegalArgumentException {
    @SuppressWarnings("unchecked")
    T copy = (T) new ReflectiveDeepCopier().copy(object);
    return copy;
  }

  private static Set<Class<?>> createImmutableTypesSet() {
    return Set.of(
        Byte.class,
        Short.class,
        Integer.class,
        Long.class,
        Float.class,
        Double.class,
        Boolean.class,
        Character.class,
        String.class,
        BigInteger.class,
        BigDecimal.class);
  }

  private static class ReflectiveDeepCopier {
    // Restrict this field to implementation because IdentityHashMap is not a conventional map.
    private final IdentityHashMap<Object, Object> copyContext;

    public ReflectiveDeepCopier() {
      this.copyContext = new IdentityHashMap<>();
    }

    public Object copy(Object original) {
      if (original == null) {
        return null;
      }
      Class<?> clazz = original.getClass();
      if (clazz.isPrimitive() || IMMUTABLE_TYPES.contains(clazz)) {
        return original;
      }
      if (copyContext.containsKey(original)) {
        return copyContext.get(original);
      }
      try {
        return clazz.isArray() ? copyArray(original) : copyObject(original);
      } catch (Exception e) {
        throw new IllegalArgumentException("Can't copy object of type " + original.getClass(), e);
      }
    }

    /** Copy array instance and then copy it's values by calling {@link #copy(Object)}. */
    private Object copyArray(Object original) {
      Class<?> componentType = original.getClass().getComponentType();
      int size = Array.getLength(original);
      Object copy = Array.newInstance(componentType, size);
      copyContext.put(original, copy);

      for (int i = 0; i < size; i++) {
        Object member = Array.get(original, i);
        Object memberCopy = copy(member);
        Array.set(copy, i, memberCopy);
      }

      return copy;
    }

    private Object copyObject(Object original) {
      Object copy;
      try {
        copy = createObjectInstance(original);
      } catch (Exception e) {
        throw new IllegalStateException("Failed to create object instance", e);
      }
      copyContext.put(original, copy);
      try {
        copyFields(original, copy);
      } catch (Exception e) {
        throw new IllegalStateException("Failed to copy field values", e);
      }
      return copy;
    }

    private Object createObjectInstance(Object original)
        throws IllegalAccessException, InstantiationException, InvocationTargetException {
      try {
        Constructor<?> defaultConstructor = original.getClass().getDeclaredConstructor();
        makeMethodAccessible(defaultConstructor);
        return defaultConstructor.newInstance();
      } catch (NoSuchMethodException e) {
        // Expected
      }

      // Try every single constructor until throwing an exception
      Constructor<?>[] constructors = original.getClass().getDeclaredConstructors();
      for (Constructor<?> constructor : constructors) {
        Parameter[] parameters = constructor.getParameters();
        Object[] parameterValues = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
          Class<?> type = parameters[i].getType();
          parameterValues[i] = type.isPrimitive() ? getPrimitiveInitialValue(type) : null;
        }

        try {
          makeMethodAccessible(constructor);
          return constructor.newInstance(parameterValues);
        } catch (Exception e) {
          // Expected
        }
      }
      throw new IllegalArgumentException("No suitable constructor");
    }

    private void copyFields(Object original, Object copy) throws IllegalAccessException {
      Field[] fields = original.getClass().getDeclaredFields();
      for (Field field : fields) {
        if (Modifier.isStatic(field.getModifiers())) {
          continue;
        }
        copyField(original, copy, field);
      }
    }

    private void copyField(Object original, Object copy, Field field)
        throws IllegalAccessException {
      makeFieldAccessible(field);
      Object value = field.get(original);
      Object valueCopy = copy(value);
      field.set(copy, valueCopy);
    }

    private static void makeMethodAccessible(Executable executable) {
      if (Modifier.isPrivate(executable.getModifiers())) {
        executable.setAccessible(true);
      }
    }

    private static void makeFieldAccessible(Field field) throws IllegalAccessException {
      if (Modifier.isPrivate(field.getModifiers())) {
        field.setAccessible(true);
      }
      if (Modifier.isFinal(field.getModifiers())) {
        try {
          Field modifiersField = Field.class.getDeclaredField("modifiers");
          modifiersField.setAccessible(true);
          modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        } catch (NoSuchFieldException e) {
          // Impossible
        }
      }
    }

    private static Object getPrimitiveInitialValue(Class<?> clazz) {
      // Slower than a bunch of ifs, but prettier for sure
      return Array.get(Array.newInstance(clazz, 1), 0);
    }
  }
}
