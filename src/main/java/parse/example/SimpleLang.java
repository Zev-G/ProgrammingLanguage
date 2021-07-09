package parse.example;

import parse.ParsePosition;
import parse.ParseResult;

public class SimpleLang {

    public static Object run(String text) {
        new MultiLineParser().parse(text, new ParsePosition(text, 0)).ifPresent(ParseResult::print);
        return null;
    }

}
