package parse.example;

import parse.ParsePosition;
import parse.ParseResult;
import parse.example.reflect.ReflectionUtils;
import parse.example.run.Runner;
import parse.example.run.StaticContext;
import parse.example.run.oo.InternalObject;
import parse.example.run.oo.VirtualFile;
import parse.example.run.oo.VirtualFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimpleLang {

    public static String x = "";

    public static Object run(String text) {
        Optional<ParseResult> result =  new MultiLineParser().parse(text, new ParsePosition(text, 0));
        if (result.isPresent()) {
            Runner runner = new Runner();
            return runner.run(result.get());
        }
        return null;
    }

    public static InternalObject run(Path loc) {
        VirtualFolder<Path> folder = VirtualFolder.fromPath(loc.getParent());
        String fileName = loc.getFileName().toString();
        VirtualFile<Path> file = folder.getFile(fileName.substring(0, fileName.lastIndexOf('.')));
        return run(folder, file);
    }
    public static InternalObject run(VirtualFolder<?> folder, VirtualFile<?> file) {
        StaticContext staticContext = new StaticContext(folder);
        return staticContext.findClass(file).orElseThrow().newInstance(null);
    }

    public static void printParsed(String text) {
        new MultiLineParser().parse(text, new ParsePosition(text, 0)).ifPresent(ParseResult::print);
    }

    public static ParseResult parse(String text) {
        return new MultiLineParser().parse(text, new ParsePosition(text, 0)).orElseThrow();
    }

}
