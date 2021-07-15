package parse.example.run;

public class RunIssue extends RuntimeException {

    public RunIssue() {
    }

    public RunIssue(String message) {
        super(message);
    }

    public RunIssue(String message, Throwable cause) {
        super(message, cause);
    }

    public RunIssue(Throwable cause) {
        super(cause);
    }

}
