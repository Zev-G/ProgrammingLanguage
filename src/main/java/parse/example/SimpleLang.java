package parse.example;

import parse.ParsePosition;
import parse.ParseResult;
import parse.example.reflect.ReflectionUtils;
import parse.example.run.Runner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimpleLang {

    public static final String ENDING = "go";
    public static String x = "";

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

    public static void printParsed(String text) {
        new MultiLineParser().parse(text, new ParsePosition(text, 0)).ifPresent(ParseResult::print);
    }

    public static ParseResult parse(String text) {
        return new MultiLineParser().parse(text, new ParsePosition(text, 0)).orElseThrow();
    }

}
