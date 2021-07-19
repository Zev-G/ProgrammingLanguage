package parse.example.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class StaticRunnableMethod extends RunnableMethod {

    public StaticRunnableMethod(Method method) {
        super(method);
    }

    @Override
    public Object run(Object[] args) throws InvocationTargetException, IllegalAccessException {
        return ReflectionUtils.invoke(method, null, args);
    }

}
