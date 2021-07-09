package parse;


import parse.example.SimpleLang;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        String code = new BufferedReader(new InputStreamReader(Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream("code.txt")))).lines().collect(Collectors.joining("\n"));
        System.out.println(SimpleLang.run(code));
    }

}
