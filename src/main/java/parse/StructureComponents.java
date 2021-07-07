package parse;

import java.util.Optional;
import java.util.function.Predicate;

public final class StructureComponents {

    public static LeafStructureComponent literal(String text, ParseType type) {
        return fromPredicate(s -> s.equals(text), type);
    }
    public static LeafStructureComponent literalIgnoreCase(String text, ParseType type) {
        return fromPredicate(s -> s.equalsIgnoreCase(text), type);
    }

    public static LeafStructureComponent matches(String pattern, ParseType type) {
        return fromPredicate(s -> s.matches(pattern), type);
    }

    public static LeafStructureComponent fromPredicate(Predicate<String> matcher, ParseType type) {
        return new LeafStructureComponent() {
            @Override
            public Optional<ParsedObject> parse(Portion portion) {
                String text = portion.getText();
                if (matcher.test(text)) {
                    return Optional.of(new ParsedObject(text, type));
                } else {
                    return Optional.empty();
                }
            }

            @Override
            public ParseType getType(Portion context) {
                return type;
            }
        };
    }

}
