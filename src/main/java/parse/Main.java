package parse;


import parse.example.SimpleLang;
import parse.example.run.ClassRunner;
import parse.example.run.StaticContext;
import parse.example.run.oo.AccessModifier;
import parse.example.run.oo.ClassHeader;
import parse.example.run.oo.InternalObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException {
        String code = new BufferedReader(new InputStreamReader(Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream("code.go")))).lines().collect(Collectors.joining("\n"));
//        SimpleLang.printParsed(code);
//        SimpleLang.run(code);

        StaticContext staticContext = new StaticContext();
        ClassRunner classRunner = new ClassRunner(staticContext, SimpleLang.parse(code), ClassHeader.basic("Main", AccessModifier.PUBLIC));
        InternalObject result = classRunner.newInstance(null);
    }

}
