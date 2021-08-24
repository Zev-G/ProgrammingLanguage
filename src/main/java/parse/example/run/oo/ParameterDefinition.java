package parse.example.run.oo;

public class ParameterDefinition {

    private final String name;
    private final boolean varargs;

    public ParameterDefinition(String name, boolean varargs) {
        this.name = name;
        this.varargs = varargs;
    }

    public ParameterDefinition(String name) {
        this(name, false);
    }

}
