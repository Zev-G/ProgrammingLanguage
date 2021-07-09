package parse;

import java.util.*;

public abstract class RedirectParserBranch extends ParserBranch {

    protected final ParseType type;

    public RedirectParserBranch(String name, ParseType type) {
        super(name);
        this.type = type;
    }

    public abstract ToBeParsed[] split(String text, ParsePosition state);

    @Override
    public Optional<ParseResult> parse(String text, ParsePosition state) {
        List<ParseResult> results = new ArrayList<>();
        for (ToBeParsed toBeParsed : split(text, state)) {
            Optional<ParseResult> result = toBeParsed.getParser().parse(toBeParsed.getText(), state);
            result.ifPresent(results::add);
        }
        return Optional.of(new ParseResult(type, text, results));
    }

}
