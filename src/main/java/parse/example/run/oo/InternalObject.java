package parse.example.run.oo;

import parse.example.run.ClassRunner;

public class InternalObject {

    private final ClassRunner classRunner;
    private final ClassDefinition definition;

    public InternalObject(ClassRunner classRunner) {
        this.classRunner = classRunner;
        this.definition = classRunner.getDefinition();
    }

    public ClassRunner getClassRunner() {
        return classRunner;
    }

    public ClassDefinition getDefinition() {
        return definition;
    }

}
