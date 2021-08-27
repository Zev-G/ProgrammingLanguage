package parse.example.run.oo;

import parse.example.run.Function;

public class InternalMethod {

    private final String name;
    private final boolean static_;

    private final Function function;
    private final ClassDefinition classDefinition;
    private final MethodDefinition definition;
    private final MethodSignature signature;

    public InternalMethod(ClassDefinition classDefinition, MethodDefinition definition, Function function) {
        this.name = definition.getName();
        this.static_ = definition.isStatic();

        this.function = function;
        this.classDefinition = classDefinition;
        this.definition = definition;
        this.signature = new MethodSignature(name, function.getParams().size());
    }

    public String getName() {
        return name;
    }

    public Function getFunction() {
        return function;
    }

    public ClassDefinition getClassDefinition() {
        return classDefinition;
    }

    public MethodDefinition getDefinition() {
        return definition;
    }

    public boolean isStatic() {
        return static_;
    }

    public MethodSignature getSignature() {
        return signature;
    }

}
