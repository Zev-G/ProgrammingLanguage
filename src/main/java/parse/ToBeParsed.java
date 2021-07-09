package parse;

public final class ToBeParsed {

    private final String text;
    private final Parser parser;
    private final ParsePosition position;

    public ToBeParsed(String text, ParsePosition position, Parser parser) {
        this.text = text;
        this.parser = parser;
        this.position = position;
    }

    public ParsePosition getPosition() {
        return position;
    }

    public String getText() {
        return text;
    }

    public Parser getParser() {
        return parser;
    }

}
