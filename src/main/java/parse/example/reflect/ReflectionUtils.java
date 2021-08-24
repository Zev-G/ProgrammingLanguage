package parse.example.reflect;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
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
                Optional<Method> superClassResult = findAccessibleMethod(obj, search.getSuperclass(), name, arguments);
                if (superClassResult.isPresent()) {
                    return findAccessibleMethod(obj, search.getSuperclass(), name, arguments);
                } else {
                    for (Class<?> implementedInterface : search.getInterfaces()) {
                        Optional<Method> interfaceResult = findAccessibleMethod(obj, implementedInterface, name, arguments);
                        if (interfaceResult.isPresent()) return interfaceResult;
                    }
                    return Optional.empty();
                }
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
        return findExecutableInternal(executables.map(ExecutableWrapper::fromExecutable), arguments).map(ExecutableWrapper::getValue);
    }

    public static <T, E extends ExecutableWrapper<T>> Optional<E> findExecutableInternal(Stream<E> executables, Object[] arguments) {
        int length = arguments.length;
        methods: for (Iterator<E> it = executables.iterator(); it.hasNext(); ) {
            E method = it.next();
            Parameter[] params = method.getParameters();
            if (length == 0) {
                if (params.length == 0 || (params.length == 1 && params[0].isVarArgs())) {
                    return Optional.of(method);
                } else {
                    continue;
                }
            }
            int at = 0;
            for (Parameter parameter : params) {
                if (at >= length) {
                    if (parameter.isVarArgs()) {
                        continue;
                    }
                    continue methods;
                }
                if (parameter.isVarArgs()) {
                    Object argAt = arguments[at];
                    if (argAt != null && parameter.getType().isAssignableFrom(argAt.getClass())) {
                        at++;
                        continue;
                    }
                    at++;
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

    public static Object invoke(Method method, Object obj, Object[] args) throws InvocationTargetException, IllegalAccessException {
        return method.invoke(obj, convertArgs(method.getParameters(), args));
    }
    public static Object[] convertArgs(Parameter[] parameters, Object[] args) {
        List<Object> newArgs = new ArrayList<>();
        int at = 0;
        for (Parameter param : parameters) {
            if (param.isVarArgs()) {
                if (at == args.length) {
                    newArgs.add(Array.newInstance(param.getType().getComponentType(), 0));
                    break;
                }
                Object argAt = args[at];
                if (argAt != null && param.getType().isAssignableFrom(argAt.getClass())) {
                    newArgs.add(argAt);
                    at++;
                    continue;
                }
                Class<?> type = param.getType().getComponentType();
                List<Object> objects = new ArrayList<>();
                for (; at < args.length; at++) {
                    Object val = args[at];
                    if (val == null) {
                        objects.add(null);
                        continue;
                    }
                    if (ReflectionUtils.isNum(type) && ReflectionUtils.isNum(val.getClass())) {
                        objects.add(ReflectionUtils.castNumber((Number) val, type));
                        continue;
                    }
                    if (!type.isAssignableFrom(val.getClass())) {
                        break;
                    } else {
                        objects.add(val);
                    }
                }
                newArgs.add(objects.toArray((Object[]) Array.newInstance(type, 0)));
            } else {
                if (args[at] != null) {
                    Class<?> type = param.getType();
                    if (ReflectionUtils.isNum(type) && ReflectionUtils.isNum(args[at].getClass())) {
                        newArgs.add(ReflectionUtils.castNumber((Number) args[at], type));
                    } else {
                        newArgs.add(args[at]);
                    }
                } else {
                    newArgs.add(null);
                }
                at++;
            }
        }
        return newArgs.toArray();
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

    public static boolean isArray(Object obj) {
        return
            obj instanceof Object[] ||
            obj instanceof byte[] ||
            obj instanceof short[] ||
            obj instanceof int[] ||
            obj instanceof long[] ||
            obj instanceof float[] ||
            obj instanceof double[] ||
            obj instanceof char[] ||
            obj instanceof boolean[];
    }
    public static int length(Object obj) {
        return
            obj instanceof Object[] ? ((Object[]) obj).length :
            obj instanceof byte[] ? ((byte[]) obj).length :
            obj instanceof short[] ? ((short[]) obj).length :
            obj instanceof int[] ? ((int[]) obj).length :
            obj instanceof long[] ? ((long[]) obj).length :
            obj instanceof float[] ? ((float[]) obj).length :
            obj instanceof double[] ? ((double[]) obj).length :
            obj instanceof char[] ? ((char[]) obj).length :
            obj instanceof boolean[] ? ((boolean[]) obj).length : -1;
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static <T> T[] joinArrays(Class<T> type, T[]... arrays) {
        int newLength = 0;
        for (T[] array : arrays) {
            newLength += array.length;
        }
        T[] newArray = (T[]) Array.newInstance(type, newLength);
        int i = 0;
        for (T[] array : arrays) {
            for (T obj : array) {
                newArray[i] = obj;
                i++;
            }
        }
        return newArray;
    }

}
