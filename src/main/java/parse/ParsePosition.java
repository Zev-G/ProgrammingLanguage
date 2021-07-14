package parse;

public final class ParsePosition {

    private final int line;
    private final char currentChar;
    private final int pos;

    private final String fileText;
    /**
     * Worth remembering that this defines lines as being separated by the '\n' or '\r' characters.
     */
    private final String[] lines;

    public ParsePosition(String fileText, int pos) {
        this.fileText = fileText;
        this.lines = fileText.lines().toArray(String[]::new);
        this.pos = pos;
        this.currentChar = fileText.charAt(pos);
        line = fileText.substring(0, pos).lines().toArray().length;
    }

    public ParsePosition add(int delta) {
        if (delta == 0) return this;
        return new ParsePosition(fileText, delta + pos);
    }
    public ParsePosition at(int pos) {
        return new ParsePosition(fileText, pos);
    }

    public int getLine() {
        return line;
    }
    public int getPos() {
        return pos;
    }
    public String getFileText() {
        return fileText;
    }
    public char getCurrentChar() {
        return currentChar;
    }

    public String subString() {
        return fileText.substring(pos);
    }

    /**
     * Returns the text of the line for the inputted number.
     * @param lineNum the line number being queried. Index starts at 0 (first line is at 0, second line at 1, etc.)
     * @return the text of the line at the inputted line number.
     * @throws ArrayIndexOutOfBoundsException if lineNum isn't a valid line number.
     * @see ParsePosition#lines
     */
    public String getLineText(int lineNum) {
        return lines[lineNum];
    }

    public String toPosString() {
        return "[" + (line + 1) + ":" + (pos + 1) + "]";
    }

}
