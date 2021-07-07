package parse;

import java.util.function.Function;

public class ParseStructure extends SubStructure {

    public static final Function<Portion, Portion[]> LINE_DISSECTOR = s -> s.getText().lines().map(s1 -> new Portion("line", s1)).toArray(Portion[]::new);

    private Function<Portion, Portion[]> dissector = LINE_DISSECTOR;

    private ParseType type;

    @Override
    public Portion[] dissect(Portion text) {
        return dissector.apply(text);
    }

    @Override
    public ParseType getType(Portion context) {
        return type == null ? new ParseType("root") : type;
    }

    public void setDissector(Function<Portion, Portion[]> dissector) {
        this.dissector = dissector;
    }

    public Function<Portion, Portion[]> getDissector() {
        return dissector;
    }

    public void setType(ParseType type) {
        this.type = type;
    }

    public ParseType getType() {
        return type;
    }

}
