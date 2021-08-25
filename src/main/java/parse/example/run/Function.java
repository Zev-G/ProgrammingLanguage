package parse.example.run;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class Function {

    private final List<String> params = new ArrayList<>();
    private boolean isUsingAnyArguments = false;

    public Function(Collection<String> params) {
        this.params.addAll(params);
    }

    public abstract Object run(RunContext context, ERI eri, Object... params);

    public List<String> getParams() {
        return params;
    }

    public void setUsingAnyArguments(boolean usingAnyArguments) {
        isUsingAnyArguments = usingAnyArguments;
    }

    public boolean isUsingAnyArguments() {
        return isUsingAnyArguments;
    }

}
