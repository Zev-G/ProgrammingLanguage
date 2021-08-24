package parse.example.run;

import parse.example.run.oo.AccessModifier;

public interface Variable {

    void set(Object obj);
    Object get();
    default AccessModifier getAccessModifier() {
        return AccessModifier.PRIVATE;
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
