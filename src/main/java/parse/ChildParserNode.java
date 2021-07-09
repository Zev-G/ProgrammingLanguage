package parse;

import java.util.Optional;

public abstract class ChildParserNode implements ParserNode {

    private final String name;
    private final ParseType type;

    public ChildParserNode(String name, ParseType type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    public abstract boolean matches(String text, ParsePosition state);

    @Override
    public Optional<ParseResult> parse(String text, ParsePosition state) {
        if (matches(text, state)) {
            return Optional.of(new ParseResult(type, text));
        } else {
            return Optional.empty();
        }
    }

}
