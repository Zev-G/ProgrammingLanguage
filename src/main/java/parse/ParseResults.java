package parse;

import java.util.*;
import java.util.function.Predicate;

public class ParseResults extends ArrayList<ParseResult> {

    public ParseResults() {}
    public ParseResults(ParseResult... results) {
        this(Arrays.asList(results));
    }

    public ParseResults(Collection<ParseResult> initialValue) {
        super(initialValue);
    }

    public ParseResult toGrouping(ParseType type) {
        return new ParseResult(type, toString(), this);
    }

    public ParseResults subList(int fromIndex) {
        return subList(fromIndex, size());
    }
    public ParseResults subList(int from, int to) {
        return new ParseResults(super.subList(from, to));
    }

    public boolean containsType(String type) {
        return firstTypeOf(type) != -1;
    }
    public boolean containsType(ParseType type) {
        return firstTypeOf(type) != -1;
    }

    public int firstTypeOf(String type) {
        return firstMatch(result -> result.typeOf(type));
    }
    public int firstTypeOf(ParseType type) {
        return firstMatch(result -> result.typeOf(type));
    }

    public int lastTypeOf(String type) {
        return lastMatch(result -> result.typeOf(type));
    }
    public int lastTypeOf(ParseType type) {
        return lastMatch(result -> result.typeOf(type));
    }

    public int firstMatch(Predicate<ParseResult> tester) {
        for (int i = 0, thisSize = this.size(); i < thisSize; i++) {
            ParseResult result = this.get(i);
            if (tester.test(result)) {
                return i;
            }
        }
        return -1;
    }
    public int lastMatch(Predicate<ParseResult> tester) {
        for (int i = size() - 1; i >= 0; i--) {
            ParseResult result = this.get(i);
            if (tester.test(result)) {
                return i;
            }
        }
        return -1;
    }

    public ParseResult last() {
        return get(size() - 1);
    }

}
