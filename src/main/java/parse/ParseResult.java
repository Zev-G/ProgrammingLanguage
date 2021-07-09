package parse;

import java.util.*;

public class ParseResult {

    private final ParseType type;
    private final String text;

    private final List<ParseResult> children = new ArrayList<>();

    public ParseResult(ParseType type, String text) {
        this(type, text, Collections.emptyList());
    }
    public ParseResult(ParseType type, String text, ParseResult... children) {
        this(type, text, Arrays.asList(children));
    }
    public ParseResult(ParseType type, String text, Collection<ParseResult> children) {
        this.type = type;
        this.text = text;
        this.children.addAll(children);
    }

    public ParseType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public List<ParseResult> getChildren() {
        return children;
    }

    public void print() {
        System.out.println(toPrettyString());
    }
    public String toPrettyString() {
        return toPrettyString(0);
    }
    private String toPrettyString(int indentation) {
        String indent = "\t".repeat(indentation);
        boolean hasChildren = !children.isEmpty();
        StringBuilder print = new StringBuilder(indent + type.toString());
        if (hasChildren) {
            print.append(" {");
            for (ParseResult child : children) {
                print.append("\n").append(child.toPrettyString(indentation + 1));
            }
            print.append("\n").append(indent).append("}");
        } else {
            print.append(" [").append(text).append("]");
        }
        return print.toString();
    }

}
