package parse.example.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

public final class ReflectionUtils {

    public static Optional<Method> findMethod(Object obj, String name, Object[] arguments) {
        return findMethod(Arrays.stream(obj.getClass().getMethods()), name, arguments);
    }
    public static Optional<Method> findMethod(Stream<Method> methods, String name, Object[] arguments) {
        Stream<Method> matchingName = methods.filter(method -> method.getName().equals(name));
        int length = arguments.length;
        methods: for (Iterator<Method> it = matchingName.iterator(); it.hasNext(); ) {
            Method method = it.next();
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
                if (parameter.isVarArgs()) {
                    for (; at < arguments.length && isAssignableFromOrNullOrBothNums(parameter.getType(), arguments[at]); at++);
                } else {
                    if (!isAssignableFromOrNullOrBothNums(parameter.getType(), arguments[at])) {
                        continue methods;
                    }
                    at++;
                }
            }
            if (at != arguments.length) {
                System.out.println("at = " + at);
                System.out.println("arguments.length = " + arguments.length);
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
