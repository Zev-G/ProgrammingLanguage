package parse.example.run;

import jdk.dynalink.beans.StaticClass;
import parse.ParsePosition;
import parse.ParseResult;
import parse.ParseType;
import parse.example.ImportParser;
import parse.example.MultiLineParser;
import parse.example.TypeRegistry;
import parse.example.reflect.ReflectionUtils;

import java.io.PrintStream;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

import static parse.example.TypeRegistry.get;

/**
 * <h1>To be implemented:</h1>
 * <ul>
 *     <li>(DONE) Modulo</li>
 *     <li>(DONE) While Loops</li>
 *     <li>Times equal (*=), divide equals (/=), etc.</li>
 *     <li>(DONE) Methods of objects.</li>
 *     <li>Fields of objects.</li>
 *     <li>(DONE) Accessing java classes.</li>
 * </ul>
 * <h1>To be fixed:</h1>
 * <ul>
 *     <li>(DONE) Negative numbers broken again. Seems to be due to negative signs being WRAP_SIDES_WITH_PARENS in {@link parse.example.LineParser}</li>
 * </ul>
 */
public class Runner {

    private final RunContext global = new RunContext();
    private PrintStream out = System.out;

    public Runner() {
        global.registerFunction("print", new Function(Collections.singleton("text")) {
            @Override
            public Object run(RunContext context, Object... params) {
                out.print(params[0]);
                return null;
            }
        });
        global.registerFunction("println", new Function(Collections.singleton("text")) {
            @Override
            public Object run(RunContext context, Object... params) {
                out.println(params[0]);
                return null;
            }
        });
        global.registerFunction("run", new Function(Collections.singleton("code")) {
            @Override
            public Object run(RunContext context, Object... params) {
                String text = String.valueOf(params[0]);
                Optional<ParseResult> parse = new MultiLineParser().parse(text, new ParsePosition(text, 0));
                return parse.map(result -> Runner.this.run(context, result)).orElse(null);
            }
        });
        global.getImports().add(Import.fromString("java.lang.*"));
    }

    public Object run(ParseResult result) {
        // Look for imports
        for (ParseResult child : result.getChildren()) {
            if (child.typeOf(ImportParser.TYPE)) {
                List<String> packages = new ArrayList<>();
                for (ParseResult importElement : child.getChildren()) {
                    if (importElement.typeOf(ImportParser.PACKAGE)) {
                        packages.add(importElement.getText());
                    } else if (importElement.typeOf(ImportParser.IMPORT_SUFFIX)) {
                        global.getImports().add(new Import(packages.toArray(new String[0]), importElement.getText()));
                        break;
                    }
                }
            }
        }

        // Run
        return run(global, result);
    }
    private Object run(RunContext context, ParseResult result) {
        // Check for illegal types.
        if (result.getType().equals(TypeRegistry.get("method-declaration"))) {
            throw new RuntimeException("Can't register function from this point.");
        }
        // Check for ignored types.
        if (result.typeOf(ImportParser.TYPE)) {
            return null;
        }
        // Check for method-declarations.
        for (ParseResult child : result.getChildren()) {
            if (child.typeOf(TypeRegistry.get("statement"))) {
                ParseResult header = child.getChildren().get(0);
                ParseResult body = child.getChildren().get(1);
                if (!header.getChildren().isEmpty() && header.getChildren().get(0).typeOf(TypeRegistry.get("method-declaration"))) {
                    ParseResult declaration = header.getChildren().get(0);
                    registerFunction(context, declaration, body);
                }
            }
        }
        // Get type for further use.
        ParseType type = result.getType();
        // Run a statement.
        if (type == get("statement")) {
            ParseResult header = result.getChildren().get(0);
            ParseResult body = result.getChildren().get(1);
            return runStatement(context, header, body);
        } else {
            context.setReadyForElse(false);
        }
        // Run all children if type is 'multi-lines' or 'body'
        if (type == get("multi-lines") || type == get("body")) {
            Object last = null;
            for (ParseResult child : result.getChildren()) {
                last = run(context, child);
                if (last instanceof ReturnedObject) {
                    return last;
                }
            }
            return last;
        }
        // Run a singular line.
        if (type == get("line")) {
            return evalMultiple(context, result.getChildren());
        }
        // Something has gone wrong if none of the previous checks have returned something.
        throw new RuntimeException("Couldn't run: " + result);
    }
    private void registerFunction(RunContext context, ParseResult declaration, ParseResult body) {
        String methodName = declaration.getChildren().get(0).getText();
        List<String> params = declaration.getChildren().get(1).getChildren().stream().map(ParseResult::getText).collect(Collectors.toList());
        context.registerFunction(methodName, new Function(params) {
            @Override
            public Object run(RunContext context1, Object... vars) {
                if (params.size() != vars.length) {
                    throw new RuntimeException("Invalid number of parameters in method call to method: " + methodName);
                }
                RunContext functionContext = new RunContext(context);
                for (int i = 0, paramsSize = params.size(); i < paramsSize; i++) {
                    String param = params.get(i);
                    functionContext.registerVariable(param, Variable.of(vars[i]));
                }
                Object returned = Runner.this.run(functionContext, body);
                if (returned instanceof ReturnedObject) {
                    return ((ReturnedObject) returned).value;
                } else {
                    return returned;
                }
            }
        });
    }

    public void setOut(PrintStream out) {
        this.out = out;
    }

    public PrintStream getOut() {
        return out;
    }

    private Object runStatement(RunContext context, ParseResult header, ParseResult body) {
        RunContext newContext = new RunContext(context);
        if (header.getChildren().isEmpty()) {
            return run(newContext, body);
        } else {
            ParseResult headerLine = header.getChildren().get(0);
            if (headerLine.getType().equals(TypeRegistry.get("method-declaration"))) {
                return null;
            }
            ParseResult defining = headerLine.getChildren().get(0);
            if (defining.typeOf("if") || defining.typeOf("elif")) {
                if (defining.typeOf("elif") && !context.isReadyForElse()){
                    return null;
                }
                Object check = evalMultiple(context, headerLine.getChildren().subList(1, headerLine.getChildren().size()));
                if (check instanceof Boolean) {
                    boolean checkVal = (Boolean) check;
                    if (checkVal) {
                        context.setReadyForElse(false);
                        return run(newContext, body);
                    } else {
                        context.setReadyForElse(true);
                        return false;
                    }
                } else {
                    throw new RuntimeException("Couldn't evaluate: \"" + headerLine.getText().trim() + "\" as boolean.");
                }
            }
            if (defining.typeOf("else")) {
                if (context.isReadyForElse()) {
                    context.setReadyForElse(false);
                    return run(newContext, body);
                } else {
                    return null;
                }
            }
            if (defining.typeOf("for")) {
                if (headerLine.getChildren().size() != 2 || !headerLine.getChildren().get(1).typeOf("grouping")) {
                    throw new RuntimeException("No parentheses used in for statement in: " + defining);
                }
                List<List<ParseResult>> separatedSections = new ArrayList<>();
                List<ParseResult> buffer = new ArrayList<>();
                for (ParseResult result : headerLine.getChildren().get(1).getChildren()) {
                    if (result.typeOf("semicolon")) {
                        separatedSections.add(new ArrayList<>(buffer));
                        buffer.clear();
                    } else {
                        buffer.add(result);
                    }
                }
                if (!buffer.isEmpty()) {
                    separatedSections.add(buffer);
                }
                if (separatedSections.size() == 3) {
                    List<ParseResult> runFirst = separatedSections.get(0);
                    List<ParseResult> checkEachTime = separatedSections.get(1);
                    List<ParseResult> runAfter = separatedSections.get(2);

                    Object last = null;
                    for (evalMultiple(newContext, runFirst); (Boolean) evalMultiple(newContext, checkEachTime); evalMultiple(newContext, runAfter)) {
                        last = run(new RunContext(newContext), body);
                    }
                    return last;
                } else {
                    throw new RuntimeException("Invalid inputs in for statement: " + header);
                }
            }
            if (defining.typeOf("while")) {
                List<ParseResult> check = headerLine.getChildren().subList(1, headerLine.getChildren().size());
                Object last = null;
                while ((Boolean) evalMultiple(newContext, check)) {
                    last = run(context, body);
                    if (last instanceof ReturnedObject) {
                        break;
                    }
                }
                return last;
            }

        }
        throw new RuntimeException("Couldn't run statement: " + header + " " + body);
    }

    private Object eval(RunContext context, ParseResult result) {
        ParseType type = result.getType();
        if (result instanceof ParseResultWrapper) {
            return ((ParseResultWrapper) result).getValue();
        }
        if (type.equals(get("grouping"))) {
            return evalMultiple(context, result.getChildren());
        }
        if (type.equals(get("variable"))) {
            Optional<Class<?>> asClass = context.findClass(result.getText());
            if (asClass.isPresent()) {
                return StaticClass.forClass(asClass.get());
            } else {
                return context.getOrCreateVariable(result.getText()).get();
            }
        }
        if (type.equals(get("number"))) {
            return Double.parseDouble(result.getText());
        }
        if (type.equals(get("true"))) {
            return true;
        }
        if (type.equals(get("false"))) {
            return false;
        }
        if (type.equals(get("string"))) {
            return result.getText();
        }
        return null;
    }

    private static final List<ParseType> STRING_TYPES = Arrays.asList(get("plus"), get("times"));
    private static final List<ParseType> MATH_TYPES = Arrays.asList(get("plus"), get("minus"), get("times"), get("division"), get("greater-or-equal"), get("greater"), get("smaller-or-equal"), get("smaller"), get("modulo"));
    private static final List<ParseType> COMPARATORS = Arrays.asList(get("equals"), get("not-equals"), get("or"), get("and"));
    private static final Object UNSET = new Object();

    private Object evalMultiple(RunContext context, List<ParseResult> results) {
        if (results.size() == 1) return eval(context, results.get(0));
        
        if (results.size() >= 3) {
            ParseType type = results.get(1).getType();
            Object left = UNSET;
            Object right = UNSET;
            if (results.get(0).typeOf("new")) {
                if (results.get(1).typeOf("method-name") && results.get(2).typeOf("method-arguments")) {
                    String name = results.get(1).getText();
                    Optional<Class<?>> referencedClass = context.findClass(name);
                    if (referencedClass.isPresent()) {
                        Object[] arguments = computeMethodParameters(results.get(2).getChildren(), "new " + name + "(...)", context).toArray();
                        Optional<Constructor<Object>> constructor = ReflectionUtils.findConstructor(referencedClass.get(), arguments);
                        if (constructor.isPresent()) {
                            convertArgs(constructor.get().getParameters(), arguments);
                            try {
                                return wrappedEvalMultiple(context, constructor.get().newInstance(arguments), results, 3);
                            } catch (InstantiationException | InvocationTargetException e) {
                                throw new RuntimeException("Couldn't instantiate " + name + ".");
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException("Couldn't access class " + referencedClass.get() + ".");
                            }
                        } else {
                            throw new RuntimeException("No constructor for class \"" + referencedClass.get() + "\" with parameters that match " + Arrays.toString(arguments) + ".");
                        }
                    } else {
                        throw new RuntimeException("Couldn't find class named: " + name);
                    }
                } else {
                    throw new RuntimeException("Keyword 'new' isn't followed by a constructor call.");
                }
            }
            if (STRING_TYPES.contains(type)) {
                left = eval(context, results.get(0));
                right = evalMultiple(context, results.subList(2, results.size()));
                if (left instanceof String || right instanceof String) {
                    if (type.equals(get("plus"))) {
                        return left + String.valueOf(right);
                    }
                    if (left instanceof String) {
                        if (type.equals(get("times"))) {
                            if (right instanceof Number) {
                                return ((String) left).repeat(((Number) right).intValue());
                            }
                        }
                    }
                }
            }
            if (MATH_TYPES.contains(type)) {
                if (left == UNSET) {
                    left = eval(context, results.get(0));
                }
                if (left instanceof Number) {
                    if (right == UNSET) {
                        right = evalMultiple(context, results.subList(2, results.size()));
                    }
                    if (right instanceof Number) {
                        Number leftNum = (Number) left;
                        Number rightNum = (Number) right;
                        if (type.equals(get("greater-or-equal"))) {
                            return leftNum.doubleValue() >= rightNum.doubleValue();
                        }
                        if (type.equals(get("greater"))) {
                            return leftNum.doubleValue() > rightNum.doubleValue();
                        }
                        if (type.equals(get("smaller-or-equal"))) {
                            return leftNum.doubleValue() <= rightNum.doubleValue();
                        }
                        if (type.equals(get("smaller"))) {
                            return leftNum.doubleValue() < rightNum.doubleValue();
                        }
                        if (type.equals(get("plus"))) {
                            return leftNum.doubleValue() + rightNum.doubleValue();
                        }
                        if (type.equals(get("minus"))) {
                            return leftNum.doubleValue() - rightNum.doubleValue();
                        }
                        if (type.equals(get("times"))) {
                            return leftNum.doubleValue() * rightNum.doubleValue();
                        }
                        if (type.equals(get("division"))) {
                            return leftNum.doubleValue() / rightNum.doubleValue();
                        }
                        if (type.equals(get("modulo"))) {
                            return leftNum.doubleValue() % rightNum.doubleValue();
                        }
                    }
                }
                error(results);
            }
            if (COMPARATORS.contains(type)) {
                if (left == UNSET) {
                    left = eval(context, results.get(0));
                }
                if (type.equals(get("equals")) || type.equals(get("not-equal"))) {
                    if (right == UNSET) {
                        right = evalMultiple(context, results.subList(2, results.size()));
                    }
                    boolean equal = Objects.equals(left, right);
                    if (type.equals(get("equals"))) {
                        return equal;
                    } else {
                        return !equal;
                    }
                }
                if (type.equals(get("and"))) {
                    if (left instanceof Boolean) {
                        boolean leftVal = (Boolean) left;
                        if (results.get(1).getText().equals("&&") && !leftVal) {
                            return false;
                        }
                        if (right == UNSET) {
                            right = evalMultiple(context, results.subList(2, results.size()));
                        }
                        Object rightObj = right;
                        if (rightObj instanceof Boolean) {
                            return leftVal && (Boolean) rightObj;
                        }
                    }
                }
                if (type.equals(get("or"))) {
                    if (left instanceof Boolean) {
                        boolean leftVal = (Boolean) left;
                        if (results.get(1).getText().equals("|") && leftVal) {
                            return true;
                        }
                        if (right == UNSET) {
                            right = evalMultiple(context, results.subList(2, results.size()));
                        }
                        Object rightObj = right;
                        if (rightObj instanceof Boolean) {
                            return leftVal || (Boolean) rightObj;
                        }
                    }
                }
            }
            if (type.equals(get("period"))) {
                if (left == UNSET) {
                    left = eval(context, results.get(0));
                }
                List<ParseResult> member = collapseGrouping(results.get(2));
                if (member.size() == 2) {
                    // Method
                    String methodName = member.get(0).getText();
                    List<Object> arguments = computeMethodParameters(member.get(1).getChildren(), methodName, context);

                    Optional<Method> method;
                    if (left instanceof StaticClass) {
                        method = ReflectionUtils.findMethod(
                                Arrays.stream(((StaticClass) left).getRepresentedClass().getMethods()).filter(testMethod -> Modifier.isStatic(testMethod.getModifiers()))
                                , methodName, arguments.toArray()
                        );
                    } else {
                        method = ReflectionUtils.findMethod(left, methodName, arguments.toArray());
                    }
                    if (method.isEmpty()) {
                        throw new RuntimeException("No method exists on object: \"" + left + "\" named: \"" + methodName + "\" which matches arguments: " + arguments);
                    }
                    try {
                        return invoke(method.get(), left, arguments.toArray());
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Can't access method " + methodName + ".");
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException("No method exists on object: \"" + left + "\" named: \"" + methodName + "\" which matches arguments: " + arguments);
                    }
                } else {
                    // Field
                }
            }
            if (results.get(0).getType().equals(get("variable"))) {
                Variable var = context.getOrCreateVariable(results.get(0).getText());
                if (results.get(1).getType().equals(get("assignment"))) {
                    Object val = evalMultiple(context, results.subList(2, results.size()));
                    var.set(val);
                    return val;
                }
            }
        }
        
        if (results.size() >= 2) {
            ParseType type = results.get(0).getType();
            if (type.equals(get("variable"))) {
                ParseResult alteration = results.get(1);
                if (alteration.typeOf("increment")) {
                    Variable var = context.getOrCreateVariable(results.get(0).getText(), () -> 0D);
                    Object val = var.get();
                    if (val instanceof Number) {
                        var.set(((Number) val).doubleValue() + 1);
                        return wrappedEvalMultiple(context, val, results, 2);
                    }
                }
                if (alteration.typeOf("decrement")) {
                    Variable var = context.getOrCreateVariable(results.get(0).getText(), () -> 0D);
                    Object val = var.get();
                    if (val instanceof Number) {
                        var.set(((Number) val).doubleValue() - 1);
                        return wrappedEvalMultiple(context, val, results, 2);
                    }
                }
                if (alteration.typeOf("square")) {
                    Variable var = context.getOrCreateVariable(results.get(0).getText(), () -> 0D);
                    Object val = var.get();
                    if (val instanceof Number) {
                        double doubleVal = ((Number) val).doubleValue();
                        var.set(doubleVal * doubleVal);
                        return wrappedEvalMultiple(context, val, results, 2);
                    } else if (val instanceof String) {
                        String newVal = ((String) val).repeat(2);
                        var.set(newVal);
                        return wrappedEvalMultiple(context, val, results, 2);
                    }
                }
            }
            if (type.equals(get("increment"))) {
                if (results.get(1).typeOf("variable")) {
                    Variable var = context.getOrCreateVariable(results.get(1).getText(), () -> 0);
                    Object val = var.get();
                    if (val instanceof Number) {
                        double newVal = ((Number) val).doubleValue() + 1;
                        var.set(newVal);
                        return wrappedEvalMultiple(context, newVal, results, 2);
                    }
                }
            }
            if (type.equals(get("decrement"))) {
                if (results.get(1).typeOf("variable")) {
                    Variable var = context.getOrCreateVariable(results.get(1).getText(), () -> 0);
                    Object val = var.get();
                    if (val instanceof Number) {
                        double newVal = ((Number) val).doubleValue() - 1;
                        var.set(newVal);
                        return wrappedEvalMultiple(context, newVal, results, 2);
                    }
                }
            }
            if (type.equals(get("square"))) {
                if (results.get(1).typeOf("variable")) {
                    Variable var = context.getOrCreateVariable(results.get(1).getText(), () -> 0);
                    Object val = var.get();
                    if (val instanceof Number) {
                        double newVal = ((Number) val).doubleValue();
                        newVal = newVal * newVal;
                        var.set(newVal);
                        return wrappedEvalMultiple(context, newVal, results, 2);
                    } else if (val instanceof String) {
                        String newVal = ((String) val).repeat(2);
                        var.set(newVal);
                        return wrappedEvalMultiple(context, newVal, results, 2);
                    }
                }
            }
            if (type.equals(get("method-name"))) {
                if (results.get(1).typeOf("method-arguments")) {
                    Optional<Function> function = context.getFunction(results.get(0).getText());
                    if (function.isEmpty()) {
                        throw new RuntimeException("No function with name: \"" + results.get(0).getText() + "\" exists.");
                    }
                    return function.get().run(context, computeMethodParameters(results.get(1).getChildren(), results.get(0).getText(), context, function.get().getParams().size()).toArray());
                } else {
                    throw new RuntimeException("No arguments for method call to method named: " + results.get(0).getText());
                }
            }
            if (type.equals(get("return"))) {
                return new ReturnedObject(evalMultiple(context, results.subList(1, results.size())));
            }
            if (type.equals(get("minus"))) {
                Object right = evalMultiple(context, results.subList(1, results.size()));
                if (right instanceof Number) {
                    return -(((Number) right).doubleValue());
                }
            }
            if (type.equals(get("plus"))) {
                Object right = evalMultiple(context, results.subList(1, results.size()));
                if (right instanceof Number) {
                    return Math.abs(((Number) right).doubleValue());
                }
            }
            if (type.equals(get("negate"))) {
                Object right = evalMultiple(context, results.subList(1, results.size()));
                if (right instanceof Boolean) {
                    return !(Boolean) right;
                }
            }
        }



        throw new RuntimeException("Couldn't run: " + results);
    }

    private Object invoke(Method method, Object obj, Object[] args) throws InvocationTargetException, IllegalAccessException {
        convertArgs(method.getParameters(), args);
        return method.invoke(obj, args);
    }
    private void convertArgs(Parameter[] parameters, Object[] args) {
        int at = 0;
        for (Parameter param : parameters) {
            if (param.isVarArgs()) {
                Class<?> type = param.getType();
                for (; at < args.length; at++) {
                    Object val = args[at];
                    if (val == null) continue;
                    if (ReflectionUtils.isNum(type) && ReflectionUtils.isNum(val.getClass())) {
                        args[at] = ReflectionUtils.castNumber((Number) val, type);
                        continue;
                    }
                    if (!type.isAssignableFrom(val.getClass())) {
                        break;
                    }
                }
            } else {
                if (args[at] != null) {
                    Class<?> type = param.getType();
                    if (ReflectionUtils.isNum(type) && ReflectionUtils.isNum(args[at].getClass())) {
                        args[at] = ReflectionUtils.castNumber((Number) args[at], type);
                    }
                }
                at++;
            }
        }
    }

    private List<Object> computeMethodParameters(List<ParseResult> params, String methodName, RunContext context) {
        return computeMethodParameters(params, methodName, context, -1);
    }
    private List<Object> computeMethodParameters(List<ParseResult> params, String methodName, RunContext context, int expected) {
        List<List<ParseResult>> arguments = new ArrayList<>();
        List<ParseResult> buffer = new ArrayList<>();
        for (ParseResult result : params) {
            if (result.typeOf("separator")) {
                arguments.add(buffer);
                buffer.clear();
            } else {
                buffer.add(result);
            }
        }
        if (!buffer.isEmpty()) {
            arguments.add(buffer);
        }
        if (expected > 0 && arguments.size() != expected) {
            throw new RuntimeException("Invalid number of parameters in call to function named: " + methodName + ". Found " + arguments.size() + " expected " + expected);
        }
        return arguments.stream().map(results -> evalMultiple(context, results)).collect(Collectors.toList());
    }

    private static final ParseType GROUPING = get("grouping");
    private static List<ParseResult> collapseGrouping(ParseResult result) {
        if (result.typeOf(GROUPING)) {
            List<ParseResult> children = result.getChildren();
            if (children.size() == 1 && children.get(0).typeOf(GROUPING)) {
                return collapseGrouping(children.get(0));
            } else {
                return children;
            }
        } else {
            throw new IllegalArgumentException("ParseResult's type doesn't equal typeOf grouping.");
        }
    }

    private Object wrappedEvalMultiple(RunContext context, Object val, List<ParseResult> results, int from) {
        if (from >= results.size()) {
            return val;
        } else {
            ParseResultWrapper wrapper = new ParseResultWrapper("N/A", val);
            List<ParseResult> subList = new ArrayList<>(results.subList(from, results.size()));
            subList.add(0, wrapper);
            return evalMultiple(context, subList);
        }
    }

    private static class ReturnedObject {

        private final Object value;

        public ReturnedObject(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

    }

    private static class ParseResultWrapper extends ParseResult {

        private static final ParseType TYPE = get("object-wrapper");

        private final Object value;

        public ParseResultWrapper(String text, Object value) {
            super(TYPE, text);
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

    }

    private void error(ParseResult result) {
        error(Collections.singletonList(result));
    }
    private void error(List<ParseResult> at) {
        throw new RuntimeException("Failed at: " + at);
    }

}
