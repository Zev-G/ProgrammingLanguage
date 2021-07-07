package parse;

public abstract class ContextAction implements Runnable {

    private final int clearDepth;

    public static ContextAction run(int clearDepth, Runnable impl) {
        return new ContextAction(clearDepth) {
            @Override
            public void run() {
                impl.run();
            }
        };
    }

    public ContextAction(int clearDepth) {
        this.clearDepth = clearDepth;
    }

    public int getClearDepth() {
        return clearDepth;
    }

}
