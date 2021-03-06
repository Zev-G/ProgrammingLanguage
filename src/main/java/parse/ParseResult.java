package parse;

import java.util.*;

public class ParseResult {

    private final ParseType type;
    private final String text;

    private final ParseResults children = new ParseResults();

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
    public boolean typeOf(ParseType type) {
        return this.type.equals(type);
    }
    public boolean typeOf(String type) {
        return this.type.getName().equals(type);
    }

    public String getText() {
        return text;
    }

    public ParseResults getChildren() {
        return children;
    }

    public void print() {
        System.out.println(toPrettyString());
    }
    public String toPrettyString() {
        return toPrettyString(0);
    }
    protected String toPrettyString(int indentation) {
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
            print.append(" \"").append(text).append("\"");
        }
        return print.toString();
    }

    @Override
    public String toString() {
        return "ParseResult{" +
                "type=" + type +
                ", " + (children.isEmpty() ? "text=" + text : "children=" + children) +
                '}';
    }

}
