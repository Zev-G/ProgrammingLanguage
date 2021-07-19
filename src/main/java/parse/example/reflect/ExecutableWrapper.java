package parse.example.reflect;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;

public interface ExecutableWrapper<T> {

    Parameter[] getParameters();
    String getName();
    T getValue();

    static <T extends Executable> ExecutableWrapper<T> fromExecutable(T executable) {
        return new ExecutableWrapper<>() {
            @Override
            public Parameter[] getParameters() {
                return executable.getParameters();
            }

            @Override
            public String getName() {
                return executable.getName();
            }

            @Override
            public T getValue() {
                return executable;
            }
        };
    }

}
