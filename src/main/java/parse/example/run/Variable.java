package parse.example.run;

import parse.example.reflect.ReflectionUtils;
import parse.example.run.oo.AccessModifier;

import java.lang.reflect.Field;

public interface Variable {

    void set(Object obj);
    Object get();
    default AccessModifier getAccessModifier() {
        return AccessModifier.PRIVATE;
    }

    static Variable fromJavaField(Field field, Object obj) {
        final AccessModifier accessModifier = AccessModifier.fromJavaModifiers(field.getModifiers());
        return new Variable() {
            @Override
            public void set(Object val) {
                try {
                    field.set(obj, val);
                } catch (IllegalAccessException e) {
                    throw new RunIssue("Can't access field \"" + field + "\" on object \"" + obj + "\".");
                }
            }

            @Override
            public Object get() {
                try {
                    return field.get(obj);
                } catch (IllegalAccessException e) {
                    throw new RunIssue("Can't access field \"" + field + "\" on object \"" + obj + "\".");
                }
            }

            @Override
            public AccessModifier getAccessModifier() {
                return accessModifier;
            }
        };
    }

    static Variable empty() {
        return new Variable() {

            Object obj;

            @Override
            public void set(Object obj) {
                this.obj = obj;
            }

            @Override
            public Object get() {
                return obj;
            }
        };
    }
    static Variable of(Object initialValue) {
        return new Variable() {

            Object obj = initialValue;

            @Override
            public void set(Object obj) {
                this.obj = obj;
            }

            @Override
            public Object get() {
                return obj;
            }
        };
    }

}
