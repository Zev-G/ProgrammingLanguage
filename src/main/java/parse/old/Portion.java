package parse.old;

class Portion {

    private final Object type;
    private final String text;

    public Portion(Object type, String text) {
        this.type = type;
        this.text = text;
    }

    public Object getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "Portion{" +
                "type=" + type +
                ", text='" + text + '\'' +
                '}';
    }

}
