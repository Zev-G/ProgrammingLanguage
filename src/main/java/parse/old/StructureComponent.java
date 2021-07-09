package parse.old;

import java.util.Optional;

interface StructureComponent {

    Optional<ParsedObject> parse(Portion portion);
    ParseType getType(Portion context);

}
