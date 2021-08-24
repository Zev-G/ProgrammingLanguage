package parse.example.run;

import java.util.ArrayList;
import java.util.List;

public class StaticContext {

    private final List<ClassRunner> classes = new ArrayList<>();

    public StaticContext() {

    }

    public void registerClass(ClassRunner classDefinition) {
        classes.add(classDefinition);
    }

}
