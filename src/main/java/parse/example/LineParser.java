package parse.example;

import parse.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static parse.example.TypeRegistry.get;

public class LineParser extends ParserBranch {

    public static final ParseType TYPE = get("line");
    private static final ParseType GROUPING = get("grouping");

    public static final List<ParseType> WRAP_SIDES_WITH_PARENS = Arrays.asList(get("equals"), get("not-equal"), get("plus"), get("minus"), get("times"), get("division"), get("modulo")/*, get("period")*/);
    public static final List<ParseType> NUMERIC_ASSIGNMENTS = Arrays.asList(get("plus-equals"), get("minus-equals"), get("times-equals"), get("division-equals"), get("modulo-equals"));
    public static final List<ParseType> SEPARATORS = new ArrayList<>(Arrays.asList(get("semicolon"), get("separator"), get("assignment"), get("return")));
    static {
        SEPARATORS.addAll(NUMERIC_ASSIGNMENTS);
    }

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
                        boolean negative = false;
                        if (text.startsWith("-")) {
                            negative = true;
                            text = text.substring(1);
                        }
                        boolean decimal = false;
                        boolean foundNum = false;
                        for (String charPoint : text.split("")) {
                            if (!charPoint.toLowerCase().equals(charPoint.toUpperCase())) {
                                break;
                            }
                            if (charPoint.equals(".")) {
                                if (decimal || !foundNum) {
                                    break;
                                }
                                decimal = true;
                                foundNum = false;
                            } else if (!charPoint.matches("[0-9]")) {
                                break;
                            } else {
                                foundNum = true;
                            }
                            valid.append(charPoint);
                        }
                        String value = valid.toString();
                        if (value.isEmpty() || !foundNum) {
                            return -1;
                        }
                        return negative ? value.length() + 1 : value.length();
                    }
                },
                new Literal(".", get("period")),

                new Literal(" : ", get("for-each")), new Literal(" in ", get("for-each")),
                new Literal(" typeof ", get("instanceof")), new Literal(" instanceof ", get("instanceof")),

                new Literal("return", get("return")), new Literal("new", get("new")),
                new Literal("true", get("true")), new Literal("false", get("false")),

                new Literal("+=", get("plus-equals")), new Literal("-=", get("minus-equals")), new Literal("*=", get("times-equals")), new Literal("/=", get("division-equals")),  new Literal("%=", get("modulo-equals")),

                new Literal("++", get("increment")), new Literal("--", get("decrement")), new Literal("**", get("square")),
                new Literal("+", get("plus")), new Literal("-", get("minus")), new Literal("*", get("times")), new Literal("/", get("division")),  new Literal("%", get("modulo")),

                new Literal(">=", get("greater-or-equal")), new Literal(">", get("greater")), new Literal("<=", get("smaller-or-equal")), new Literal("<", get("smaller")),
                new Literal("||", get("or")), new Literal("|", get("or")), new Literal("&&", get("and")), new Literal("&", get("and")),
                new Literal("==", get("equals")), new Literal("!=", get("not-equal")), new Literal("=", get("assignment")),
                new Literal("!", get("negate")), new Literal(";", get("semicolon")),

                new Regex("[$_A-z][$_A-z0-9]*", get("variable")) // This is matches the specification for a valid java variable name excluding the no-keyword rule.
        ));
    }

    @Override
    public Optional<ParseResult> parse(String text, ParsePosition state) {

        List<ParseResult> results = new ArrayList<>();
        int withinParens = 0;
        int withinSquareBrackets = 0;
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
                    if (withinParens == 0 && withinSquareBrackets == 0) {
                        buffer = new StringBuilder();
                        continue;
                    }
                } else if (!escaped) {
                    inString = false;
                    if (withinParens == 0 && withinSquareBrackets == 0) {
                        results.add(new ParseResult(get("string"), buffer.toString()));
                        buffer = new StringBuilder();
                        continue;
                    }
                }
            }

            // Handle chars.
            if (currentChar == '\'' && !inString) {
                if (!inChar) {
                    inChar = true;
                    if (withinParens == 0 && withinSquareBrackets == 0) {
                        buffer = new StringBuilder();
                        continue;
                    }
                } else if (!escaped) {
                    inChar = false;
                    if (withinParens == 0 && withinSquareBrackets == 0) {
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
                    if (withinParens != 0 || withinSquareBrackets != 0) {
                        buffer.append(currentChar);
                    }
                    continue;
                }

                if (withinParens == 0 && withinSquareBrackets == 0) {

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

            // Handle open and closing square brackets.
            if (currentChar == '[') {
                if (withinSquareBrackets++ == 0 && withinParens == 0) {
                    continue;
                }
            } else if (currentChar == ']') {
                if (--withinSquareBrackets == 0 && withinParens == 0) {
                    Optional<ParseResult> result = parse(buffer.toString(), position);
                    if (result.isPresent()) {
                        results.add(new ParseResult(get("square-bracket-grouping"), buffer.toString(), result.get().getChildren()));
                    } else {
                        results.add(new ParseResult(get("square-bracket-grouping"), buffer.toString()));
                    }
                    buffer = new StringBuilder();
                    continue;
                }
            }

            // Handle open and closing parentheses
            if (currentChar == '(') {
                if (withinParens++ == 0 && withinSquareBrackets == 0) {
                    continue;
                }
            } else if (currentChar == ')') {
                if (--withinParens == 0 && withinSquareBrackets == 0) {
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

            if (withinParens > 0 || withinSquareBrackets > 0) {
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

        if (inString || inChar) {
            throw new RuntimeException("Text wasn't closed at pos: " + state.toPosString());
        }

        if (results.isEmpty()) {
            return Optional.empty();
        } else {

            // Map all variables in front of groupings to method-names.
            for (int i = 0, length = results.size() - 1; i < length; i++) {
                ParseResult at = results.get(i);
                ParseResult after = results.get(i + 1);
                if (at.typeOf(get("variable")) && after.typeOf(get("grouping"))) {
                    results.set(i, new ParseResult(get("method-name"), at.getText(), at.getChildren()));
                    results.set(i + 1, new ParseResult(get("method-arguments"), after.getText(), after.getChildren()));
                    i++;
                }
            }

            return Optional.of(new ParseResult(TYPE, text, group(results)));
        }
    }

    private List<ParseResult> group(List<ParseResult> results) {
        List<ParseResult> newResults = new ArrayList<>();

        int lastAdded = 0;
        boolean wrapIntoBuffer = false;
        List<ParseResult> buffer = new ArrayList<>();

        for (int i = 0, length = results.size(); i < length; i++) {
            ParseResult result = results.get(i);

            if (WRAP_SIDES_WITH_PARENS.contains(result.getType())) {
                List<ParseResult> subList = results.subList(lastAdded, i);
                if (!subList.isEmpty()) {
                    buffer.add(wrapWithGrouping(subList));
                }
                if (wrapIntoBuffer) {
                    ParseResult add = wrapWithGrouping(buffer);
                    buffer.clear();
                    buffer.add(add);
                }
                buffer.add(result);
                wrapIntoBuffer = true;
                lastAdded = i + 1;
                continue;
            }

            if (SEPARATORS.contains(result.getType())) {
                List<ParseResult> subList = results.subList(lastAdded, i);
                if (wrapIntoBuffer) {
                    buffer.add(wrapWithGrouping(subList));
                    newResults.addAll(buffer);
                } else {
                    newResults.addAll(buffer);
                    newResults.addAll(subList);
                }
                buffer.clear();
                newResults.add(result);
                wrapIntoBuffer = false;
                lastAdded = i + 1;
            }

        }

        if (lastAdded != results.size() || !buffer.isEmpty()) {
            List<ParseResult> subList = results.subList(lastAdded, results.size());
            if (wrapIntoBuffer) {
                if (!subList.isEmpty()) {
                    buffer.add(wrapWithGrouping(subList));
                }
                newResults.addAll(buffer);
            } else {
                newResults.addAll(buffer);
                newResults.addAll(subList);
            }
        }

        return newResults;
    }

    private static ParseResult wrapWithGrouping(List<ParseResult> results) {
        if (results.size() == 1 && results.get(0).typeOf(GROUPING)) {
            return results.get(0);
        }
        return new ParseResult(GROUPING, "(" + results.stream().map(Objects::toString).collect(Collectors.joining(" ")) + ")", results);
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
