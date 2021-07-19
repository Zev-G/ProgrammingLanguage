package parse.example.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ObjectsRunnableMethod extends RunnableMethod {

    private final Object obj;

    public ObjectsRunnableMethod(Method method, Object obj) {
        super(method);
        this.obj = obj;
    }

    @Override
    public Object run(Object[] args) throws InvocationTargetException, IllegalAccessException {
        return ReflectionUtils.invoke(method, obj, args);
    }

}
