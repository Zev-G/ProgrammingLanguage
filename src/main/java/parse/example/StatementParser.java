package parse.example;

import parse.*;

import java.util.Optional;

public abstract class StatementParser extends ParserBranch {

    private static final ParseType HEADER = TypeRegistry.get("header");
    private static final ParseType BODY = TypeRegistry.get("body");

    private final Parser headerParser;
    private final Parser bodyParser;

    private final ParseType type;

    public StatementParser(String name, Parser headerParser, Parser bodyParser, ParseType type) {
        super(name);
        this.headerParser = headerParser;
        this.bodyParser = bodyParser;
        this.type = type;
    }

    public Parser getHeaderParser() {
        return headerParser;
    }

    public Parser getBodyParser() {
        return bodyParser;
    }

    public abstract ToBeParsedStatement separate(String text, ParsePosition position);

    @Override
    public Optional<ParseResult> parse(String text, ParsePosition state) {
        ToBeParsedStatement separated = separate(text, state);
        if (separated != null) {
            Optional<ParseResult> header = headerParser.parse(separated.getHeader(), separated.getHeaderPos());
            Optional<ParseResult> body = bodyParser.parse(separated.getBody(), separated.getBodyPos());

            boolean headerIsPresent = header.isPresent();
            boolean bodyIsPresent = body.isPresent();
            if (!headerIsPresent && !bodyIsPresent) {
                return Optional.empty();
            }

            ParseResult result = new ParseResult(type, text);

            if (headerIsPresent) {
                ParseResult headerVal = header.get();
                result.getChildren().add(new ParseResult(
                        HEADER, headerVal.getText(), headerVal
                ));
            }
            if (bodyIsPresent) {
                ParseResult bodyVal = body.get();
                result.getChildren().add(new ParseResult(
                        BODY, bodyVal.getText(), bodyVal.getChildren() // maybe this should just add the entire multi-line? idk.
                ));
            }

            return Optional.of(result);
        } else {
            return Optional.empty();
        }
    }

    public static class ToBeParsedStatement {

        private final ParsePosition headerPos;
        private final String header;
        private final ParsePosition bodyPos;
        private final String body;


        public ToBeParsedStatement(ParsePosition textPos, String text, ParsePosition linesPosition, String lines) {
            this.headerPos = textPos;
            this.header = text;
            this.bodyPos = linesPosition;
            this.body = lines;
        }

        public ParsePosition getHeaderPos() {
            return headerPos;
        }

        public String getHeader() {
            return header;
        }

        public ParsePosition getBodyPos() {
            return bodyPos;
        }

        public String getBody() {
            return body;
        }

        @Override
        public String toString() {
            return "ToBeParsedStatement{" +
                    "headerPos=" + headerPos +
                    ", header='" + header + '\'' +
                    ", bodyPos=" + bodyPos +
                    ", body='" + body + '\'' +
                    '}';
        }

    }

}
