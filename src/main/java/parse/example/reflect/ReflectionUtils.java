package parse.example.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

public final class ReflectionUtils {

    public static Iterable<?> primitiveArrayToIterable(byte[] iterator) {
        return (Iterable<Object>) () -> new Iterator<>() {

            int at = 0;

            @Override
            public boolean hasNext() {
                return at < iterator.length;
            }

            @Override
            public Object next() {
                return iterator[at++];
            }
        };
    }

    public static Iterable<?> primitiveArrayToIterable(short[] iterator) {
        return (Iterable<Object>) () -> new Iterator<>() {

            int at = 0;

            @Override
            public boolean hasNext() {
                return at < iterator.length;
            }

            @Override
            public Object next() {
                return iterator[at++];
            }
        };
    }

    public static Iterable<?> primitiveArrayToIterable(int[] iterator) {
        return (Iterable<Object>) () -> new Iterator<>() {

            int at = 0;

            @Override
            public boolean hasNext() {
                return at < iterator.length;
            }

            @Override
            public Object next() {
                return iterator[at++];
            }
        };
    }

    public static Iterable<?> primitiveArrayToIterable(long[] iterator) {
        return (Iterable<Object>) () -> new Iterator<>() {

            int at = 0;

            @Override
            public boolean hasNext() {
                return at < iterator.length;
            }

            @Override
            public Object next() {
                return iterator[at++];
            }
        };
    }

    public static Iterable<?> primitiveArrayToIterable(float[] iterator) {
        return (Iterable<Object>) () -> new Iterator<>() {

            int at = 0;

            @Override
            public boolean hasNext() {
                return at < iterator.length;
            }

            @Override
            public Object next() {
                return iterator[at++];
            }
        };
    }

    public static Iterable<?> primitiveArrayToIterable(double[] iterator) {
        return (Iterable<Object>) () -> new Iterator<>() {

            int at = 0;

            @Override
            public boolean hasNext() {
                return at < iterator.length;
            }

            @Override
            public Object next() {
                return iterator[at++];
            }
        };
    }

    public static Iterable<?> primitiveArrayToIterable(char[] iterator) {
        return (Iterable<Object>) () -> new Iterator<>() {

            int at = 0;

            @Override
            public boolean hasNext() {
                return at < iterator.length;
            }

            @Override
            public Object next() {
                return iterator[at++];
            }
        };
    }

    public static Iterable<?> primitiveArrayToIterable(boolean[] iterator) {
        return (Iterable<Object>) () -> new Iterator<>() {

            int at = 0;

            @Override
            public boolean hasNext() {
                return at < iterator.length;
            }

            @Override
            public Object next() {
                return iterator[at++];
            }
        };
    }

    public static Optional<Method> findMethod(Object obj, String name, Object[] arguments) {
        return findMethod(Arrays.stream(obj.getClass().getMethods()), name, arguments);
    }
    public static Optional<Method> findMethod(Stream<Method> methods, String name, Object[] arguments) {
        return findExecutable(methods, name, arguments);
    }
    public static Optional<Method> findAccessibleMethod(Object obj, String name, Object[] arguments) {
        if (obj == null) return Optional.empty();
        return findAccessibleMethod(obj, obj.getClass(), name, arguments);
    }
    public static Optional<Method> findAccessibleMethod(Object obj, Class<?> search, String name, Object[] arguments) {
        if (search == null) return Optional.empty();
        Optional<Method> result = findMethod(Arrays.stream(search.getMethods()), name, arguments);
        if (result.isPresent()) {
            Method method = result.get();
            if (method.canAccess(obj)) {
                return Optional.of(method);
            } else {
                return findAccessibleMethod(obj, search.getSuperclass(), name, arguments);
            }
        } else {
            return Optional.empty();
        }
    }
    @SuppressWarnings("unchecked")
    public static <T> Optional<Constructor<T>> findConstructor(Class<?> obj, Object[] arguments) {
        return findExecutable(Arrays.stream((Constructor<T>[]) obj.getConstructors()), arguments);
    }
    public static <T> Optional<Constructor<T>> findConstructor(Stream<Constructor<T>> constructors, Object[] arguments) {
        return findExecutable(constructors, arguments);
    }
    public static <T extends Executable> Optional<T> findExecutable(Stream<T> executables, String name, Object[] arguments) {
        return findExecutable(executables.filter(executable -> executable.getName().equals(name)), arguments);
    }
    public static <T extends Executable> Optional<T> findExecutable(Stream<T> executables, Object[] arguments) {
        int length = arguments.length;
        methods: for (Iterator<T> it = executables.iterator(); it.hasNext(); ) {
            T method = it.next();
            Parameter[] params = method.getParameters();
            if (length == 0) {
                if (params.length == 0) {
                    return Optional.of(method);
                } else {
                    continue;
                }
            }
            int at = 0;
            for (Parameter parameter : params) {
                if (at >= length) {
                    continue methods;
                }
                if (parameter.isVarArgs()) {
                    Object argAt = arguments[at];
                    if (argAt != null && parameter.getType().isAssignableFrom(argAt.getClass())) {
                        at++;
                        continue;
                    }
                    for (; at < length && isAssignableFromOrNullOrBothNums(parameter.getType().getComponentType(), arguments[at]); at++);
                } else {
                    if (!isAssignableFromOrNullOrBothNums(parameter.getType(), arguments[at])) {
                        continue methods;
                    }
                    at++;
                }
            }
            if (at != arguments.length) {
                continue;
            }
            return Optional.of(method);
        }
        return Optional.empty();
    }

    public static boolean isAssignableFromOrNullOrBothNums(Class<?> aClass, Object check) {
        if (check == null) return true;
        else if (isNum(aClass) && isNum(check.getClass())) return true;
        else return aClass.isAssignableFrom(check.getClass());
    }

    public static Object castNumber(Number val, Class<?> type) {
        if (type == int.class || type == Integer.class) {
            return val.intValue();
        } else if (type == double.class || type == Double.class) {
            return val.doubleValue();
        } else if (type == long.class || type == Long.class) {
            return val.longValue();
        } else if (type == byte.class || type == Byte.class) {
            return val.byteValue();
        } else if (type == short.class || type == Short.class) {
            return val.shortValue();
        } else if (type == float.class || type == Float.class) {
            return val.floatValue();
        }
        throw new IllegalArgumentException();
    }

    public static boolean isNum(Class<?> type) {
        return Number.class.isAssignableFrom(type) || type == int.class || type == double.class || type == long.class || type == byte.class || type == short.class || type == float.class;
    }

}
