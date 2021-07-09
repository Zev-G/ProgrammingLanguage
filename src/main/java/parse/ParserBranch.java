package parse;

public abstract class ParserBranch implements ParserNode {

    private final String name;

    public ParserBranch(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String getName() {
        return name;
    }

}
