package parse;


import parse.example.SimpleLang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException {
        String code = new BufferedReader(new InputStreamReader(Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream("code.go")))).lines().collect(Collectors.joining("\n"));
        SimpleLang.run(code);
    }

}
