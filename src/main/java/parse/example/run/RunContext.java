package parse.example.run;

import java.util.*;
import java.util.function.Supplier;

public class RunContext {

    public static final boolean TRACK_CHILDREN = false;

    private final RunContext parent;
    private final List<RunContext> children = TRACK_CHILDREN ? new ArrayList<>() : null;

    private final Set<Import> imports = new HashSet<>();
    private final Map<String, Variable> variables = new HashMap<>();
    private final Map<String, Function> functions = new HashMap<>();

    private boolean readyForElse = false;

    public RunContext() {
        this(null);
    }
    public RunContext(RunContext parent) {
        this.parent = parent;
        if (parent != null) {
            this.imports.addAll(parent.getImports());
            if (TRACK_CHILDREN) {
                this.parent.children.add(this);
            }
        }
    }

    public Optional<Variable> getVariable(String text) {
        Variable var = variables.get(text);
        if (var == null) {
            if (parent == null) {
                return Optional.empty();
            }
            return parent.getVariable(text);
        }
        return Optional.of(var);
    }
    public Variable getOrCreateVariable(String text) {
        return getOrCreateVariable(text, () -> null);
    }
    public Variable getOrCreateVariable(String text, Object initialValue) {
        return getOrCreateVariable(text, () -> initialValue);
    }
    public Variable getOrCreateVariable(String text, Supplier<Object> initialValSupplier) {
        Optional<Variable> varCheck = getVariable(text);
        if (varCheck.isPresent()) return varCheck.get();
        Variable variable = Variable.of(initialValSupplier.get());
        variables.put(text, variable);
        return variable;
    }
    public Optional<Function> getFunction(String text) {
        Function function = functions.get(text);
        if (function == null) {
            if (parent == null) {
                return Optional.empty();
            }
            return parent.getFunction(text);
        }
        return Optional.of(function);
    }

    public Optional<Class<?>> findClass(String name) {
        for (Import loopImport : imports) {
            Optional<Class<?>> result = loopImport.findClass(name);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    Set<Import> getImports() {
        return imports;
    }

    public RunContext getParent() {
        return parent;
    }

    public Map<String, Variable> getVariables() {
        return variables;
    }

    public Map<String, Function> getFunctions() {
        return functions;
    }

    public List<RunContext> getChildren() {
        return children;
    }

    public void registerFunction(String name, Function function) {
        functions.put(name, function);
    }

    public void registerVariable(String name, Variable variable) {
        variables.put(name, variable);
    }

    public boolean isReadyForElse() {
        return readyForElse;
    }

    public void setReadyForElse(boolean readyForElse) {
        this.readyForElse = readyForElse;
    }

}
