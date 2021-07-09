package parse;

public interface ParseType {

    default boolean isParser() {
        return false;
    }
    String getName();

    static ParseType byName(String text) {
        return new ParseType() {
            @Override
            public String getName() {
                return text;
            }

            @Override
            public String toString() {
                return text;
            }
        };
    }

}
