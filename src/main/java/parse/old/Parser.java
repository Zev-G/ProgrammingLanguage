package parse.old;

import java.util.Optional;

class Parser {

    public static Optional<ParsedObject> parse(String text, StructureComponent structure) {
        return structure.parse(new Portion("root", text));
    }


}
