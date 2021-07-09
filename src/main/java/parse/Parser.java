package parse;

import java.util.Optional;

@FunctionalInterface
public interface Parser {

    Optional<ParseResult> parse(String text, ParsePosition state);

}
