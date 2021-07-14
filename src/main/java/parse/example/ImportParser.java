package parse.example;

import parse.ParsePosition;
import parse.ParseResult;
import parse.ParseType;
import parse.ParserBranch;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ImportParser extends ParserBranch {

    public static final ParseType TYPE = TypeRegistry.get("import");
    public static final ParseType PACKAGE = TypeRegistry.get("package");
    public static final ParseType IMPORT_SUFFIX = TypeRegistry.get("import-suffix");

    public ImportParser() {
        super("Import parser");
    }

    @Override
    public Optional<ParseResult> parse(String text, ParsePosition state) {
        text = text.trim();
        if (!text.startsWith("import ")) {
            return Optional.empty();
        }
        text = text.substring(text.indexOf("import ") + "import ".length());

        List<ParseResult> parts = new ArrayList<>();
        StringBuilder partBuffer = new StringBuilder();
        char[] characters = text.toCharArray();
        for (int i = 0, length = characters.length; i < length; i++) {
            char currentChar = characters[i];
            if (currentChar == '.') {
                String part = partBuffer.toString().trim();
                if (part.isEmpty()) {
                    System.err.println("Period at " + state.add(i).toPosString() + " isn't preceded by a package.");
                    return Optional.empty();
                }
                parts.add(new ParseResult(PACKAGE, part));
                partBuffer = new StringBuilder();
            } else {
                partBuffer.append(currentChar);
            }
        }
        String finalPart = partBuffer.toString();
        if (finalPart.isEmpty()) {
            System.err.println("Import on line " + state.getLine() + " has no suffix.");
            return Optional.empty();
        }
        parts.add(new ParseResult(IMPORT_SUFFIX, finalPart));

        return Optional.of(new ParseResult(TYPE, text, parts));
    }

}
