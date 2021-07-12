package parse.example;

import parse.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static parse.example.TypeRegistry.get;

public class LineParser extends ParserBranch {

    public static final ParseType TYPE = get("line");

    private final MultiLineParser multiLineParser;

    protected final List<Portion> portions = new ArrayList<>();

    public LineParser(MultiLineParser multiLineParser) {
        super("Line Parser");
        this.multiLineParser = multiLineParser;

        this.portions.addAll(Arrays.asList(
                new Literal(",", get("separator")),

                new Portion(get("number")) {
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

                new Literal("return", get("return")),
                new Literal("true", get("true")), new Literal("false", get("false")),

                new Literal("+", get("plus")), new Literal("-", get("minus")), new Literal("*", get("times")), new Literal("/", get("division")),

                new Literal(">=", get("greater-or-equal")), new Literal(">", get("greater")), new Literal("<=", get("smaller-or-equal")), new Literal("<", get("smaller")),
                new Literal("||", get("or")), new Literal("|", get("or")), new Literal("&&", get("and")), new Literal("&", get("and")),
                new Literal("==", get("equals")), new Literal("!=", get("not-equal")), new Literal("=", get("assignment")),
                new Literal("!", get("negate")), new Literal(";", get("semicolon")),

                new Regex("[^ \n\r=!(),*-+/.;{}]+", get("variable"))
        ));
    }

    @Override
    public Optional<ParseResult> parse(String text, ParsePosition state) {
        List<ParseResult> results = new ArrayList<>();
        int withinParens = 0;
        StringBuilder buffer = new StringBuilder();
        boolean inString = false;
        boolean inChar = false;
        boolean escaped = false;

        char[] characters = text.toCharArray();
        main: for (int at = 0, length = characters.length; at < length; at++) {
            char currentChar = characters[at];
            ParsePosition position = new ParsePosition(text, at);
            String substring = text.substring(at);

            // Handle strings.
            if (currentChar == '"') {
                if (!inString) {
                    inString = true;
                    if (withinParens == 0) {
                        buffer = new StringBuilder();
                        continue;
                    }
                } else if (!escaped) {
                    inString = false;
                    if (withinParens == 0) {
                        results.add(new ParseResult(get("string"), buffer.toString()));
                        buffer = new StringBuilder();
                        continue;
                    }
                }
            }

            // Handle chars.
            if (currentChar == '\'') {
                if (!inChar) {
                    inChar = true;
                    if (withinParens == 0) {
                        buffer = new StringBuilder();
                        continue;
                    }
                } else if (!escaped) {
                    inChar = false;
                    if (withinParens == 0) {
                        results.add(new ParseResult(get("char"), buffer.toString()));
                        continue;
                    }
                }
            }

            boolean inText = inString || inChar;

            if (inText) {
                // Handle character escaping.
                if (currentChar == '\\' && !escaped) {
                    escaped = true;
                    if (withinParens != 0) {
                        buffer.append(currentChar);
                    }
                    continue;
                }

                if (withinParens == 0) {

                    // Handle certain special cases.
                    if (escaped) {
                        if (currentChar == 'n') {
                            currentChar = '\n';
                        } else if (currentChar == 'r') {
                            currentChar = '\r';
                        } else if (currentChar == 't') {
                            currentChar = '\t';
                        } else if (currentChar == 'b') {
                            currentChar = '\b';
                        } else if (currentChar == 'f') {
                            currentChar = '\f';
                        }
                    }
                }

                escaped = false;
                buffer.append(currentChar);
                continue;
            }

            if (currentChar == '(') {
                if (withinParens++ == 0) {
                    continue;
                }
            } else if (currentChar == ')') {
                withinParens--;
                if (withinParens == 0) {
                    Optional<ParseResult> result = parse(buffer.toString(), position);
                    if (result.isPresent()) {
                        results.add(new ParseResult(get("grouping"), buffer.toString(), result.get().getChildren()));
                    } else {
                        results.add(new ParseResult(get("grouping"), buffer.toString()));
                    }
                    buffer = new StringBuilder();
                    continue;
                }
            }

            if (withinParens > 0) {
                buffer.append(currentChar);
                continue;
            }

            for (Portion portion : portions) {
                int found = portion.find(substring);
                if (found != -1) {
                    results.add(new ParseResult(portion.type, substring.substring(0, found)));
                    at += found - 1;
                    continue main;
                }
            }
        }

        System.out.println(results);

        if (inString || inChar) {
            throw new RuntimeException("Text wasn't closed at pos: " + state);
        }

        // Map all variables in front of groupings to method-names.

        if (results.isEmpty()) {
            return Optional.empty();
        } else {
            for (int i = 0, length = results.size() - 1; i < length; i++) {
                ParseResult at = results.get(i);
                ParseResult after = results.get(i + 1);
                if (at.typeOf(get("variable")) && after.typeOf(get("grouping"))) {
                    results.set(i, new ParseResult(get("method-name"), at.getText(), at.getChildren()));
                    results.set(i + 1, new ParseResult(get("method-arguments"), after.getText(), after.getChildren()));
                    i++;
                }
            }
            return Optional.of(new ParseResult(TYPE, text, results));
        }
    }

    public List<Portion> getPortions() {
        return portions;
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
