package parse.example.run;

import parse.ParsePosition;
import parse.ParseResult;
import parse.ParseType;
import parse.example.MultiLineParser;
import parse.example.TypeRegistry;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static parse.example.TypeRegistry.get;

public class Runner {

    private final RunContext global = new RunContext();

    public Runner() {
        global.registerFunction("print", new Function(Collections.singleton("text")) {
            @Override
            public Object run(RunContext context, Object... params) {
                System.out.println(params[0]);
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
    }

    public Object run(ParseResult result) {
        return run(global, result);
    }
    private Object run(RunContext context, ParseResult result) {
        // Check for illegal types.
        if (result.getType().equals(TypeRegistry.get("method-declaration"))) {
            throw new RuntimeException("Can't register function from this point.");
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
            if (defining.typeOf(get("if")) || defining.typeOf(get("elif"))) {
                if (defining.typeOf(get("elif")) && !context.isReadyForElse()) {
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
            if (defining.typeOf(get("else"))) {
                if (context.isReadyForElse()) {
                    context.setReadyForElse(false);
                    return run(newContext, body);
                } else {
                    return null;
                }
            }
            if (defining.typeOf(get("for"))) {
                if (headerLine.getChildren().size() != 2 || !headerLine.getChildren().get(1).typeOf(get("grouping"))) {
                    throw new RuntimeException("No parentheses used in for statement in: " + defining);
                }
                List<List<ParseResult>> separatedSections = new ArrayList<>();
                List<ParseResult> buffer = new ArrayList<>();
                for (ParseResult result : headerLine.getChildren().get(1).getChildren()) {
                    if (result.typeOf(get("semicolon"))) {
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
            return context.getOrCreateVariable(result.getText()).get();
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
    private static final List<ParseType> MATH_TYPES = Arrays.asList(get("plus"), get("minus"), get("times"), get("division"), get("greater-or-equal"), get("greater"), get("smaller-or-equal"), get("smaller"));
    private static final List<ParseType> COMPARATORS = Arrays.asList(get("equals"), get("not-equals"), get("or"), get("and"));

    private Object evalMultiple(RunContext context, List<ParseResult> results) {
        if (results.size() == 1) return eval(context, results.get(0));

        if (results.size() >= 2) {
            ParseType type = results.get(0).getType();
            if (type.equals(get("variable"))) {
                ParseResult alteration = results.get(1);
                if (alteration.typeOf(get("increment"))) {
                    Variable var = context.getOrCreateVariable(results.get(0).getText(), () -> 0D);
                    Object val = var.get();
                    if (val instanceof Number) {
                        var.set(((Number) val).doubleValue() + 1);
                        return wrappedEvalMultiple(context, val, results, 2);
                    }
                }
                if (alteration.typeOf(get("decrement"))) {
                    Variable var = context.getOrCreateVariable(results.get(0).getText(), () -> 0D);
                    Object val = var.get();
                    if (val instanceof Number) {
                        var.set(((Number) val).doubleValue() - 1);
                        return wrappedEvalMultiple(context, val, results, 2);
                    }
                }
                if (alteration.typeOf(get("square"))) {
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
                if (results.get(1).typeOf(get("variable"))) {
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
                if (results.get(1).typeOf(get("variable"))) {
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
                if (results.get(1).typeOf(get("variable"))) {
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
                if (results.get(1).typeOf(get("method-arguments"))) {
                    Optional<Function> function = context.getFunction(results.get(0).getText());
                    if (function.isEmpty()) {
                        throw new RuntimeException("No function with name: \"" + results.get(0).getText() + "\" exists.");
                    }
                    List<Object> arguments = new ArrayList<>();
                    List<ParseResult> buffer = new ArrayList<>();
                    for (ParseResult result : results.get(1).getChildren()) {
                        if (result.typeOf(get("separator"))) {
                            arguments.add(evalMultiple(context, buffer));
                            buffer.clear();
                        } else {
                            buffer.add(result);
                        }
                    }
                    if (!buffer.isEmpty()) {
                        arguments.add(evalMultiple(context, buffer));
                    }
                    return function.get().run(context, arguments.toArray());
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

        if (results.size() >= 3) {
            ParseType type = results.get(1).getType();
            if (STRING_TYPES.contains(type)) {
                Object left = eval(context, results.get(0));
                if (left instanceof String) {
                    if (type.equals(get("plus"))) {
                        String right = String.valueOf(evalMultiple(context, results.subList(2, results.size())));
                        return left + right;
                    }
                    if (type.equals(get("times"))) {
                        Object right = evalMultiple(context, results.subList(2, results.size()));
                        if (right instanceof Number) {
                            return ((String) left).repeat(((Number) right).intValue());
                        }
                    }
                }
            }
            if (MATH_TYPES.contains(type)) {
                Object left = eval(context, results.get(0));
                if (left instanceof Number) {
                    Object right = evalMultiple(context, results.subList(2, results.size()));
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
                    }
                }
                error(results);
            }
            if (COMPARATORS.contains(type)) {
                Object left = eval(context, results.get(0));
                Supplier<Object> right = () -> evalMultiple(context, results.subList(2, results.size()));
                if (type.equals(get("equals")) || type.equals(get("not-equal"))) {
                    boolean equal = left == right.get();
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
                        Object rightObj = right.get();
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
                        Object rightObj = right.get();
                        if (rightObj instanceof Boolean) {
                            return leftVal || (Boolean) rightObj;
                        }
                    }
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

        throw new RuntimeException("Couldn't run: " + results + "");
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
