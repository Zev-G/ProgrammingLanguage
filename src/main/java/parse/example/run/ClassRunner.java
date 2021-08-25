package parse.example.run;

import parse.ParseResult;
import parse.ParseType;
import parse.example.ClassParser;
import parse.example.run.oo.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static parse.example.TypeRegistry.get;

public class ClassRunner {

    public static final List<ParseType> ACCESS_MODIFIERS = Arrays.asList(get("private"), get("protected"), get("public"), get("package"));

    private final StaticContext staticContext;
    private final ParseResult parsedClass;
    private final Runner runner;

    private final ClassDefinition definition;

    public ClassRunner(StaticContext staticContext, ParseResult parsedClass, ClassHeader header) {
        this.staticContext = staticContext;
        this.parsedClass = parsedClass;

        definition = ClassParser.parse(parsedClass, header);
        strip();
        staticContext.registerClass(this);
        runner = new Runner(staticContext);
    }

    public Object run() {
        return runner.run(parsedClass);
    }

    public InternalObject newInstance(ConstructorDefinition constructor, Object... params) {
        if (constructor != null) {
            if (constructor.getClassDefinition() != null && constructor.getClassDefinition() != definition) throw new IllegalArgumentException();
            if (!definition.containsConstructor(constructor)) throw new IllegalArgumentException();
        } else if (definition.getConstructors().length != 0) throw new IllegalArgumentException();

        RunContext instanceContext = new RunContext(runner.getGlobal());
        final InternalObject internalThis = new InternalObject(this, instanceContext);
        instanceContext.getOrCreateVariable("this", internalThis);

        createInstanceFields(instanceContext, internalThis);

        if (constructor != null) {
            Function runnableConstructor = runner.createFunction(constructor.getCode(), instanceContext);
            runnableConstructor.run(instanceContext, ERI.DEFAULT, params);
        }

        runner.run(instanceContext, parsedClass, ERI.DEFAULT);

        return internalThis;
    }

    private void createInstanceFields(RunContext instanceContext, InternalObject object) {
        for (FieldDefinition field : definition.getFields()) {
            if (!field.isStatic()) {
                Variable fieldVar = createField(field, instanceContext);
                object.getFields().put(field.getName(), fieldVar);
            }
        }
    }

    private Variable createField(FieldDefinition field, RunContext context) {
        ParseResult code = field.getCode();

        List<ParseResult> codeChildrenCopy = new ArrayList<>(code.getChildren());
        for (int i = codeChildrenCopy.size() - 1; i >= 0; i--) {
            if (ACCESS_MODIFIERS.contains(codeChildrenCopy.get(i).getType())) {
                codeChildrenCopy.remove(codeChildrenCopy.get(i));
            }
        }

        ParseResult runCopy = new ParseResult(code.getType(), code.getText(), codeChildrenCopy);

        runner.run(context, runCopy, ERI.DEFAULT);
        Variable created = context.getVariables().get(field.getName());
        if (created == null) throw new IllegalStateException();
        Variable fieldVar = new Variable() {
            Object val = created.get();

            @Override
            public void set(Object obj) {
                val = obj;
            }

            @Override
            public Object get() {
                return val;
            }

            @Override
            public AccessModifier getAccessModifier() {
                return field.getAccessModifier();
            }
        };
        context.getVariables().put(field.getName(), fieldVar);
        return fieldVar;
    }

    private void strip() {
        strip(parsedClass);
    }
    private void strip(ParseResult result) {
        for (int i = result.getChildren().size() - 1; i >= 0; i--) {
            ParseResult child = result.getChildren().get(i);
            if (Arrays.stream(definition.getFields()).map(MemberDefinition::getCode).anyMatch(child::equals)) {
                result.getChildren().remove(i);
            } else if (!child.getChildren().isEmpty()) {
                strip(child);
            }
        }
    }

    public ClassDefinition getDefinition() {
        return definition;
    }

}
