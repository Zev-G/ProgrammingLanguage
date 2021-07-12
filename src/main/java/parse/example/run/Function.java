package parse.example.run;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class Function {

    private final List<String> params = new ArrayList<>();

    public Function(Collection<String> params) {
        this.params.addAll(params);
    }

    public abstract Object run(RunContext context, Object... params);

}
