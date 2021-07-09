package parse.old;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class ParsedObject {

    private final List<ParsedObject> children = new ArrayList<>();
    private final String text;
    private final ParseType type;
    private final Object[] tokens;

    public ParsedObject(String text, ParseType type, Object... tokens) {
        this(text, type, Collections.emptyList(), tokens);
    }
    public ParsedObject(String text, ParseType type, Collection<ParsedObject> children, Object... tokens) {
        this.children.addAll(children);
        this.text = text;
        this.type = type;
        this.tokens = tokens;
    }

    public String getText() {
        return text;
    }
    public List<ParsedObject> getChildren() {
        return children;
    }
    public Object[] getTokens() {
        return tokens;
    }
    public Object getType() {
        return type;
    }

    public void print() {
        System.out.println(print(0));
    }
    private String print(int indentation) {
        String indent = "\t".repeat(indentation);
        boolean hasChildren = !children.isEmpty();
        StringBuilder print = new StringBuilder(indent + type.toString());
        if (hasChildren) {
            print.append(" {");
            for (ParsedObject child : children) {
                print.append("\n").append(child.print(indentation + 1));
            }
            print.append("\n").append(indent).append("}");
        } else {
            print.append(" [").append(text).append("]");
        }
        return print.toString();
    }

    @Override
    public String toString() {
        return "ParseResult{" +
               "children=" + children +
               ", type=" + type +
               '}';
    }

}