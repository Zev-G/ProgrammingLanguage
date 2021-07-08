package parse;

import java.util.Optional;

public interface StructureComponent {

    Optional<ParsedObject> parse(Portion portion);
    ParseType getType(Portion context);

}
