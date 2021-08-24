package parse.example;

import parse.*;

import java.util.*;
import java.util.stream.Collectors;

public class StatementHeaderParser extends ParserBranch {

    private static final List<String> BANNED_FUNCTION_NAMES = Arrays.asList("if", "else", "else if", "elif", "for", "while");

    private LineParser lineParser;

    private final Parser methodDeclarationParser = (text, state) -> {
        StringBuilder buffer = new StringBuilder();
        boolean inParams = false;
        boolean ended = false;

        String name = null;
        List<String> params = new ArrayList<>();

        char[] characters = text.trim().toCharArray();
        for (int i = 0, length = characters.length; i < length; i++) {
            if (ended) {
                return Optional.empty();
            }
            char currentChar = characters[i];
            if (currentChar == '(') {
                if (inParams) {
                    return Optional.empty();
                }
                inParams = true;
                name = buffer.toString().trim();
                buffer = new StringBuilder();
                if (name.isEmpty()) {
                    return Optional.empty();
                }
                continue;
            }
            if (currentChar == ')') {
                if (!inParams || i + 1 != length) {
                    return Optional.empty();
                }
                if (!buffer.toString().isEmpty()) {
                    params.add(buffer.toString());
                }
                ended = true;
                continue;
            }
            if (currentChar == ',') {
                if (!inParams || buffer.toString().trim().isEmpty()) {
                    return Optional.empty();
                }
                params.add(buffer.toString().trim());
                buffer = new StringBuilder();
                continue;
            }
            buffer.append(currentChar);
        }
        if (name != null) {
            if (BANNED_FUNCTION_NAMES.contains(name)) {
                return Optional.empty();
            }
            List<ParseResult> prefixes = new ArrayList<>();
            // This if statement handles modifiers like private and public for method-declarations.
            if (name.contains(" ")) {
                String[] pieces = name.split(" ");
                for (int i = 0, piecesLength = pieces.length; i < piecesLength - 1; i++) {
                    String piece = pieces[i];
                    Optional<ParseResult> result = lineParser.parse(piece, state);
                    if (result.isPresent() && !result.get().getChildren().isEmpty()) {
                        prefixes.addAll(result.get().getChildren());
                    } else {
                        return Optional.empty();
                    }
                }
                name = pieces[pieces.length - 1];
            }
            ParseResult paramsResult = new ParseResult(TypeRegistry.get("parameters"), String.join(",", params), params.stream().map(param -> new ParseResult(TypeRegistry.get("parameter"), param)).collect(Collectors.toList()));
            ParseResult result = new ParseResult(TypeRegistry.get("method-declaration"), text, new ParseResult(TypeRegistry.get("method-name"), name), paramsResult);
            result.getChildren().addAll(0, prefixes);
            return Optional.of(result);
        } else {
            return Optional.empty();
        }
    };

    public StatementHeaderParser(MultiLineParser multiLineParser) {
        super("statement-header");
        this.lineParser = new LineParser(multiLineParser);
        this.lineParser.getPortions().add(0, new LineParser.Literal("if", TypeRegistry.get("if")));
        this.lineParser.getPortions().add(0, new LineParser.Literal("else", TypeRegistry.get("else")));
        this.lineParser.getPortions().add(0, new LineParser.Literal("else if", TypeRegistry.get("elif")));
        this.lineParser.getPortions().add(0, new LineParser.Literal("elif", TypeRegistry.get("elif")));
        this.lineParser.getPortions().add(0, new LineParser.Literal("for", TypeRegistry.get("for")));
        this.lineParser.getPortions().add(0, new LineParser.Literal("while", TypeRegistry.get("while")));
    }

    @Override
    public Optional<ParseResult> parse(String text, ParsePosition state) {
        Optional<ParseResult> methodDeclarationResult = methodDeclarationParser.parse(text, state);
        if (methodDeclarationResult.isPresent()) {
            return methodDeclarationResult;
        }

        return lineParser.parse(text, state);
    }

}
