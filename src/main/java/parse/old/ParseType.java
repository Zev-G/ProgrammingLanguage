package parse.old;

import java.util.Arrays;
import java.util.stream.Collectors;

class ParseType {

    private final String val;

    public ParseType(String mark) {
        this.val = mark;
    }
    public ParseType(Object... marks) {
        this.val = Arrays.stream(marks).map(Object::toString).collect(Collectors.joining(" "));
    }

    @Override
    public String toString() {
        return val;
    }

}
