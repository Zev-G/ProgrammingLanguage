package parse.example.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public abstract class RunnableMethod implements ExecutableWrapper<RunnableMethod> {

    protected final Method method;

    public RunnableMethod(Method method) {
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }

    public abstract Object run(Object[] args) throws InvocationTargetException, IllegalAccessException;

    @Override
    public Parameter[] getParameters() {
        return method.getParameters();
    }

    @Override
    public String getName() {
        return method.getName();
    }

    @Override
    public RunnableMethod getValue() {
        return this;
    }

}
