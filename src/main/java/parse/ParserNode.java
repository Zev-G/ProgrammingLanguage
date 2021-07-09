package parse;

public interface ParserNode extends ParseType, Parser {

    @Override
    default boolean isParser() {
        return true;
    }

}
