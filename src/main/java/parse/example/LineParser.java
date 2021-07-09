package parse.example;

import parse.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LineParser extends ParserBranch {

    public static final ParseType TYPE = TypeRegistry.get("line");

    private final MultiLineParser multiLineParser;

    protected final List<Portion> portions = new ArrayList<>();

    public LineParser(MultiLineParser multiLineParser) {
        super("Line Parser");
        this.multiLineParser = multiLineParser;

        this.portions.addAll(Arrays.asList(
                new Portion(TypeRegistry.get("number")) {
                    @Override
                    public int find(String text) {
                        StringBuilder valid = new StringBuilder();
                        boolean decimal = false;
                        for (String charPoint : text.split("")) {
                            if (!charPoint.toLowerCase().equals(charPoint.toUpperCase())) {
                                break;
                            }
                            if (charPoint.equals(".")) {
                                if (decimal) {
                                    break;
                                }
                                decimal = true;
                            } else if (!charPoint.matches("[0-9]")) {
                                break;
                            }
                            valid.append(charPoint);
                        }
                        String value = valid.toString();
                        if (value.isEmpty()) {
                            return -1;
                        }
                        return value.length();
                    }
                },

                new Literal("print", TypeRegistry.get("print")),

                new Literal("==", TypeRegistry.get("equals")), new Literal("!=", TypeRegistry.get("not-equal")), new Literal("=", TypeRegistry.get("assignment")),

                new Regex("[^ \n\r=!]+", TypeRegistry.get("variable"))
        ));
    }

    @Override
    public Optional<ParseResult> parse(String text, ParsePosition state) {
        List<ParseResult> results = new ArrayList<>();

        char[] characters = text.toCharArray();
        main: for (int at = 0, length = characters.length; at < length; at++) {
            char currentChar = characters[at];
            ParsePosition position = new ParsePosition(text, at);
            String substring = text.substring(at);

            for (Portion portion : portions) {
                int found = portion.find(substring);
                if (found != -1) {
                    results.add(new ParseResult(portion.type, substring.substring(0, found)));
                    at += found - 1;
                    continue main;
                }
            }
        }

        if (results.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(new ParseResult(TYPE, text, results));
        }
    }

    public abstract static class Portion {

        private final ParseType type;

        public Portion(ParseType type) {
            this.type = type;
        }

        public abstract int find(String text);

        public ParseType getType() {
            return type;
        }

    }

    public static class Regex extends Portion {

        private final Pattern regex;

        public Regex(String regex, ParseType type) {
            this(Pattern.compile(regex), type);
        }
        public Regex(Pattern regex, ParseType type) {
            super(type);
            this.regex = regex;
        }

        @Override
        public int find(String text) {
            Matcher matcher = regex.matcher(text);
            if (matcher.find() && matcher.start() == 0) {
                return matcher.end();
            }
            return -1;
        }

    }

    public static class Literal extends Portion {

        private final String text;

        public Literal(String text, ParseType type) {
            super(type);
            this.text = text;
        }

        public String getText() {
            return text;
        }

        @Override
        public int find(String text) {
            if (text.startsWith(this.text)) {
                return this.text.length();
            } else {
                return -1;
            }
        }

    }

}
