package parse;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class SimpleSubStructure extends SubStructure {

    private final ParseType type;
    private final Function<Portion, Portion[]> dissector;

    private Predicate<Portion> parseValidator = portion -> true;

    public SimpleSubStructure(ParseType type, Function<Portion, Portion[]> dissector) {
        this(type, dissector, Collections.emptyList());
    }
    public SimpleSubStructure(ParseType type, Function<Portion, Portion[]> dissector, StructureComponent... children) {
        this(type, dissector, Arrays.asList(children));
    }
    public SimpleSubStructure(ParseType type, Function<Portion, Portion[]> dissector, Collection<StructureComponent> children) {
        this.type = type;
        this.dissector = dissector;
        this.children.addAll(children);
    }

    @Override
    public Portion[] dissect(Portion text) {
        return dissector.apply(text);
    }

    @Override
    public Optional<ParsedObject> parse(Portion text) {
        if (parseValidator.test(text)) {
            return super.parse(text);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public ParseType getType(Portion context) {
        return type;
    }

    public ParseType getType() {
        return type;
    }

    public Function<Portion, Portion[]> getDissector() {
        return dissector;
    }

    public void setParseValidator(Predicate<Portion> parseValidator) {
        this.parseValidator = parseValidator;
    }

}
