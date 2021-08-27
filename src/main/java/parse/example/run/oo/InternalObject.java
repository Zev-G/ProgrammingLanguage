package parse.example.run.oo;

import parse.example.run.ClassRunner;
import parse.example.run.RunContext;
import parse.example.run.Runner;
import parse.example.run.Variable;

import java.util.HashMap;
import java.util.Map;

public class InternalObject {

    private final ClassRunner classRunner;
    private final ClassDefinition definition;
    private final RunContext instanceContext;

    private final Map<String, Variable> fields = new HashMap<>();
    private final Map<MethodSignature, InternalMethod> methods = new HashMap<>();

    public InternalObject(ClassRunner classRunner, RunContext instanceContext) {
        this.classRunner = classRunner;
        this.definition = classRunner.getDefinition();
        this.instanceContext = instanceContext;
    }

    public ClassRunner getClassRunner() {
        return classRunner;
    }

    public ClassDefinition getDefinition() {
        return definition;
    }

    public Map<String, Variable> getFields() {
        return fields;
    }

    public InternalMethod getMethod(String methodName, Object[] args) {
        return methods.get(new MethodSignature(methodName, args.length));
    }

    public Map<MethodSignature, InternalMethod> getMethods() {
        return methods;
    }

}
