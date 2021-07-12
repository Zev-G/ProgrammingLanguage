package parse.example;

import parse.ParsePosition;
import parse.ParseResult;
import parse.example.run.Runner;

import java.util.Optional;

public class SimpleLang {

    public static Object run(String text) {
        Optional<ParseResult> result =  new MultiLineParser().parse(text, new ParsePosition(text, 0));
        if (result.isPresent()) {
            result.get().print();
            return new Runner().run(result.get());
        }
        return null;
    }

}
