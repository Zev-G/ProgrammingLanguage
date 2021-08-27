package parse;


import parse.example.SimpleLang;
import parse.example.run.ClassRunner;
import parse.example.run.StaticContext;
import parse.example.run.oo.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException {
        try {
            String code = new BufferedReader(new InputStreamReader(Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream("Main.go")))).lines().collect(Collectors.joining("\n"));
//        SimpleLang.printParsed(code);
//        SimpleLang.run(code);

            SimpleLang.run(
                    new File(Objects.requireNonNull(Main.class.getClassLoader().getResource("Main.go")).getFile()).toPath()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
