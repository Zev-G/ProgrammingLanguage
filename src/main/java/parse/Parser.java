package parse;

import java.util.Optional;

public class Parser {

    public static Optional<ParsedObject> parse(String text, StructureComponent structure) {
        return structure.parse(new Portion("root", text));
    }


}
