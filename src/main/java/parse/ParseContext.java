package parse;


import java.util.*;

public class ParseContext {

    private static final boolean AUTO_ADD_CHILDREN = true;

    private final Set<ParseContext> children = new HashSet<>();
    private final Map<Object, Object> properties = new HashMap<>();
    private final ParseContext parent;
    private final int depth;

    private String name;

    public ParseContext(String name, ParseContext parent) {
        this(name, parent, parent.depth + 1);
    }
    public ParseContext(String name, ParseContext parent, int depth) {
        this.name = name;
        this.parent = parent;
        this.depth = depth;
        if (AUTO_ADD_CHILDREN && parent != null) {
            parent.children.add(this);
        }
    }
    public ParseContext(String name) {
        this.name = name;
        this.parent = null;
        this.depth = 0;
    }

    public Set<ParseContext> getChildren() {
        return children;
    }

    public Map<Object, Object> getProperties() {
        return properties;
    }

    public ParseContext getParent() {
        return parent;
    }

    public int getDepth() {
        return depth;
    }

    public String getName() {
        return name;
    }

}
