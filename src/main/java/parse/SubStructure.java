package parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class SubStructure implements StructureComponent {

    protected final List<StructureComponent> children = new ArrayList<>();

    public abstract Portion[] dissect(Portion text);

    @Override
    public Optional<ParsedObject> parse(Portion text) {
        Portion[] portions = dissect(text);
        List<ParsedObject> results = new ArrayList<>();
        for (Portion portion : portions) {
            ParsedObject result = null;
            Optional<ParsedObject> internalResult = internalParse(portion);
            if (internalResult.isPresent()) {
                result = internalResult.get();
            } else {
                for (StructureComponent child : children) {
                    Optional<ParsedObject> childResult = child.parse(portion);
                    if (childResult.isPresent()) {
                        result = childResult.get();
                        break;
                    }
                }
            }
            if (result == null) {
                System.err.println("Couldn't parse section: " + portion + " in text: " + text);
            } else {
                results.add(result);
            }
        }
        if (results.isEmpty()) {
            return Optional.of(new ParsedObject(text.getText(), getType(text)));
        } else {
            return Optional.of(new ParsedObject(text.getText(), getType(text), results));
        }
    }

    protected Optional<ParsedObject> internalParse(Portion portion) {
        return Optional.empty();
    }

    public List<StructureComponent> getChildren() {
        return children;
    }

}
