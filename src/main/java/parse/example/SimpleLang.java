package parse.example;

import parse.ParsePosition;
import parse.ParseResult;
import parse.example.run.Runner;

import java.util.Optional;

public class SimpleLang {

    public static Object run(String text) {
        Optional<ParseResult> result =  new MultiLineParser().parse(text, new ParsePosition(text, 0));
        if (result.isPresent()) {
//            result.get().print();
            Runner runner = new Runner();
//            runner.setDelay(1000);
//            runner.setUsingDelay(true);
//            runner.setTrackRunningLine(true);
//            runner.setCurrentLineInvalidated(() -> System.out.println(runner.getCurrentLine().getText()));
            return runner.run(result.get());
        }
        return null;
    }

}
