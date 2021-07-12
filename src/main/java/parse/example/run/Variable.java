package parse.example.run;

public interface Variable {

    void set(Object obj);
    Object get();

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
