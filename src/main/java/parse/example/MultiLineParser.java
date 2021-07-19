package parse.example;

import parse.*;

import java.util.ArrayList;
import java.util.List;

public class MultiLineParser extends RedirectParserBranch {

    private final Parser importParser;
    private final Parser lineParser;

    private final Parser headerParser;
    private final StatementParser statementParser;

    public MultiLineParser() {
        super("Multi-line parser", TypeRegistry.get("multi-lines"));
        lineParser = new LineParser(this);
        headerParser = new StatementHeaderParser(this);
        statementParser = new StatementParser("Statement Parser", headerParser, this, TypeRegistry.get("statement")) {
            @Override
            public ToBeParsedStatement separate(String text, ParsePosition position) {
                char[] characters = text.toCharArray();

                StringBuilder upToOpenBracket = new StringBuilder();
                String postOpenBracket = null;
                int i = 0;
                for (int length = characters.length; i < length; i++) {
                    char at = characters[i];
                    if (at != '{') {
                        upToOpenBracket.append(at);
                    } else {
                        if (i + 1 < characters.length - 1) {
                            postOpenBracket = text.substring(i + 1, characters.length - 1);
                        } else {
                            postOpenBracket = "";
                        }
                        break;
                    }
                }

                return new ToBeParsedStatement(
                        new ParsePosition(text, 0), upToOpenBracket.toString(),
                        new ParsePosition(text, i), postOpenBracket
                );
            }
        };
        importParser = new ImportParser();
    }

    @Override
    public ToBeParsed[] split(String text, ParsePosition state) {
        return new Object() {

            // ================== Settings ===================

            final boolean ignoreNewLines = true;
            final char lineSeparatingChar = ';';

            // ===============================================

            // -------------- Loop Variables ----------------
            int at = 0;
            int within = 0;
            int withinParens = 0;
            int line = 0;
            int onLine = 0;

            boolean inSingleLineComment = false;
            boolean inMultiLineComment = false;
            boolean inString = false;
            boolean inChar = false;
            boolean escaped = false;

            String text;
            StringBuilder segmentBuilder = new StringBuilder();
            final List<ToBeParsed> portions = new ArrayList<>();

            @SuppressWarnings("ConstantConditions")
            ToBeParsed[] parse(String text) {
                this.text = text;

                char[] characters = text.toCharArray();
                int length = characters.length;
                for (; at < length; at++) {
                    // Variables for current position
                    char currentChar = characters[at];
                    boolean newLine = currentChar == '\n' || currentChar == '\r';
                    String afterInclusive = text.substring(at);

                    // Check if entering a single-line comment.
                    if (afterInclusive.startsWith("//") && !inBlockade()) {
                        inSingleLineComment = true;
                    }

                    // Check if entering a multi-line comment.
                    if (afterInclusive.startsWith("/*") && !inBlockade()) {
                        inMultiLineComment = true;
                    }

                    // Check if on a new line a single-line comment.
                    if (newLine) {

                        // Output parse errors.
                        if (inString || inChar) {
                            logError("Quote isn't closed by end of line.");
                        }

                        // Change variables.
                        onLine = 0;
                        line++;
                        inSingleLineComment = false;
                        if (ignoreNewLines && lineSeparatingChar != '\n' && lineSeparatingChar != '\r') {
                            continue;
                        }
                    } else {
                        // Increment position on line.
                        onLine++;
                    }

                    // Check if leaving a multi-line comment.
                    if (afterInclusive.startsWith("*/") && !inText()) {
                        inMultiLineComment = false;
                        at++;
                        continue;
                    }

                    // Skip if still in comment.
                    if (inComment()) {
                        continue;
                    }

                    // Keep track of if we have already used the currentCharacter's value and therefore shouldn't check it again.
                    // Doing this could be substituted for a long if-else statement chain but I've chosen not to do this to make the code:
                    //      A) Easier to maintain.
                    //      B) Easier to read.
                    boolean checkedChar = false;

                    // Handle strings.
                    if (currentChar == '"') {
                        if (!inString) {
                            inString = true;
                        } else if (!escaped) {
                            inString = false;
                        }
                    }

                    // Handle chars.
                    if (currentChar == '\'' && !inString) {
                        if (!inChar) {
                            inChar = true;
                        } else if (!escaped) {
                            inChar = false;
                        }
                    }

                    boolean inText = inString || inChar;

                    // Handle character escaping.
                    if ((inString || inChar) && currentChar == '\\') {
                        escaped = !escaped;
                    } else {
                        escaped = false;
                    }

                    if (!inText) {

                        // Handle open parentheses.
                        if (currentChar == '(') {
                            withinParens++;
                            checkedChar = true;
                        }

                        // Handle closing parentheses.
                        if (currentChar == ')') {
                            withinParens--;
                            checkedChar = true;
                        }

                        // Handle open brackets.
                        if (currentChar == '{') {
                            within++;
                            checkedChar = true;
                        }

                        // Handle closing brackets.
                        if (!checkedChar && currentChar == '}') {
                            checkedChar = true;
                            within--;
                            // Add current portion if the parser is returning to its original depth.
                            if (within == 0) {
                                segmentBuilder.append(currentChar);
                                addPortion(statementParser);
                                continue;
                            }

                            // Handle errors.
                            if (within < 0) {
                                logError("Closing bracket has no opening bracket.");
                            }
                        }

                        // If at original depth.
                        if (within == 0 && withinParens == 0) {
                            // Check if loop is on line separating character.
                            if (!checkedChar && currentChar == lineSeparatingChar) {
                                // Determine if the added line is a normal line or an import line. If it is an import line parse it as such, otherwise parse it as a normal line.
                                String line = segmentBuilder.toString();
                                if (line.trim().startsWith("import")) {
                                    addPortion(importParser);
                                } else {
                                    addPortion(lineParser);
                                }
                                continue;
                            }
                        }
                    }

                    // Append current character.
                    segmentBuilder.append(currentChar);
                }

                // Handle errors.
                if (within > 0) {
                    logError(within + " Opening bracket" + (within == 1 ? " is" : "s are") + " never closed.");
                }

                // Return result.
                return portions.toArray(ToBeParsed[]::new);
            }

            private void logError(String s) {
                System.err.println("[" + (line + 1) + ":" + (onLine + 1) + "] " + s);
            }

            private void addPortion(Parser parser) {
                portions.add(new ToBeParsed(segmentBuilder.toString(), new ParsePosition(this.text, at), parser));
                segmentBuilder = new StringBuilder();
            }

            private boolean inBlockade() {
                return inComment() || inText();
            }

            private boolean inText() {
                return inString || inChar;
            }

            private boolean inComment() {
                return inSingleLineComment || inMultiLineComment;
            }

        }.parse(text);
    }

}
