package parse.example.run.oo;

import java.util.Objects;

public class MethodSignature {

    private final String name;
    private final int args;

    public MethodSignature(String name, int args) {
        this.name = name;
        this.args = args;
    }

    public String getName() {
        return name;
    }

    public int getArgs() {
        return args;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodSignature that = (MethodSignature) o;
        return args == that.args && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, args);
    }

}
