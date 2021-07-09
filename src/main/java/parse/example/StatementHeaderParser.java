package parse.example;

public class StatementHeaderParser extends LineParser {

    public StatementHeaderParser(MultiLineParser multiLineParser) {
        super(multiLineParser);

        portions.add(0, new Literal("if", TypeRegistry.get("if")));
    }

}
