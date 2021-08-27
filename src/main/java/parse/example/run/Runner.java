package parse.example.run;

import jdk.dynalink.beans.StaticClass;
import parse.ParsePosition;
import parse.ParseResult;
import parse.ParseResults;
import parse.ParseType;
import parse.example.ImportParser;
import parse.example.LineParser;
import parse.example.MultiLineParser;
import parse.example.TypeRegistry;
import parse.example.reflect.ObjectsRunnableMethod;
import parse.example.reflect.ReflectionUtils;
import parse.example.reflect.RunnableMethod;
import parse.example.run.oo.ClassDefinition;
import parse.example.run.oo.ConstructorDefinition;
import parse.example.run.oo.InternalObject;
import parse.example.run.oo.MethodSignature;

import java.io.PrintStream;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static parse.example.TypeRegistry.get;

/**
 * <h1>To be implemented:</h1>
 * <ul>
 *     <li>(DONE) Modulo</li>
 *     <li>(DONE) While Loops</li>
 *     <li>(DONE) Times equal (*=), divide equals (/=), etc.</li>
 *     <li>Static imports for methods and fields.</li>
 *     <li>Lambdas.</li>
 *     <li>(DONE) For each loops.</li>
 *     <li>Support for arrays.</li>
 *     <li>(DONE) Methods of objects.</li>
 *     <li>(DONE) Fields of objects.</li>
 *     <li>(DONE) Accessing java classes.</li>
 *     <h2>Implementing OOP</h2>
 *     <ul>
 *         <li>Stop constructors from being runnable as normal functions inside of files.</li>
 *     </ul>
 * </ul>
 * <h1>To be fixed:</h1>
 * <ul>
 *     <li>(DONE) Negative numbers broken again. Seems to be due to negative signs being WRAP_SIDES_WITH_PARENS in {@link parse.example.LineParser}</li>
 * </ul>
 * <h1>Other goals:</h1>
 * <ul>
 *     <li>Improve performance. Should be able to run a loop from 0 to 1,000,000 in under a second.</li>
 *     <li>Better parsing errors.</li>
 * </ul>
 */
public class Runner {

    private final RunContext global = new RunContext();
    private final StaticContext staticContext;

    private PrintStream out = System.out;
    
    private boolean trackEvaluating = false;
    private boolean trackRunningLine = false;
    
    private boolean usingDelay = false;
    private long delay = 0;

    private Runnable currentLineInvalidated;
    private ParseResult currentLine;

    public Runner() {
        this(new StaticContext(null));
    }
    public Runner(StaticContext staticContext) {
        this.staticContext = staticContext;
        global.registerFunction("print", new Function(Collections.singleton("text")) {
            @Override
            public Object run(RunContext context, ERI eri, Object... params) {
                out.print(params[0]);
                return null;
            }
        });
        global.registerFunction("println", new Function(Collections.singleton("text")) {
            @Override
            public Object run(RunContext context, ERI eri, Object... params) {
                out.println(params[0]);
                return null;
            }
        });
        global.registerFunction("run", new Function(Collections.singleton("code")) {
            @Override
            public Object run(RunContext context, ERI eri, Object... params) {
                String text = String.valueOf(params[0]);
                Optional<ParseResult> parse = new MultiLineParser().parse(text, new ParsePosition(text, 0));
                return parse.map(result -> Runner.this.run(context, result, eri)).orElse(null);
            }
        });
        global.registerFunction("range", new Function(List.of("begin", "end")) {
            @Override
            public Object run(RunContext context, ERI eri, Object... params) {
                Object begin = params[0];
                Object end = params[1];
                if (!(begin instanceof Number) || !(end instanceof Number)) {
                    throw new IllegalArgumentException("Beginning and end of range must be integers.");
                }
                return IntStream.range(((Number) begin).intValue(), ((Number) end).intValue()).toArray();
            }
        });
        global.registerFunction("registerInstanceMethodsOf", new Function(Collections.singleton("object")) {
            @Override
            public Object run(RunContext context, ERI eri, Object... params) {
                Object obj = params[0];
                for (Method method : obj.getClass().getMethods()) {
                    if (!Modifier.isStatic(method.getModifiers())) {
                        context.getStaticMethods().add(new ObjectsRunnableMethod(method, obj));
                    }
                }
                return null;
            }
        });
        global.registerFunction("registerStaticMethodsOf", new Function(Collections.singleton("object")) {
            @Override
            public Object run(RunContext context, ERI eri, Object... params) {
                Object obj = params[0];
                for (Method method : (obj instanceof Class<?> ? (Class<?>) obj : obj.getClass()).getMethods()) {
                    if (Modifier.isStatic(method.getModifiers())) {
                        context.getStaticMethods().add(new ObjectsRunnableMethod(method, obj));
                    }
                }
                return null;
            }
        });
        global.getImports().add(Import.fromString("java.lang.*"));
        global.getImports().add(Import.fromString("java.util.*"));
    }

    public Object run(ParseResult result) {
        return run(result, ERI.DEFAULT);
    }
    public Object run(ParseResult result, ERI eri) {
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
        return run(global, result, eri);
    }
    public Object run(RunContext context, ParseResult result, ERI eri) {
        // Check for illegal types.
        if (result.getType().equals(TypeRegistry.get("method-declaration"))) {
            throw new RunIssue("Can't register function from this point.");
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
            return runStatement(context, header, body, eri);
        } else {
            context.setReadyForElse(false);
        }
        // Run all children if type is 'multi-lines' or 'body'
        if (type == get("multi-lines") || type == get("body")) {
            Object last = null;
            for (ParseResult child : result.getChildren()) {
                last = run(context, child, eri);
                if (last instanceof ReturnedObject) {
                    return last;
                }
            }
            return last;
        }
        // Run a singular line.
        if (type == get("line")) {
            if (trackRunningLine) {
                currentLine = result;
                currentLineInvalidated.run();
            }
            if (usingDelay && delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return evalMultiple(context, result.getChildren(), eri);
        }
        // Something has gone wrong if none of the previous checks have returned something.
        throw new RunIssue("Couldn't run: " + result);
    }
    private void registerFunction(RunContext context, ParseResult declaration, ParseResult body) {
        String methodName = declaration.getChildren().get(0).getText();
        context.registerFunction(methodName, createFunction(declaration, body, context));
    }

    public Function createFunction(ParseResult statement, RunContext context) {
        return createFunction(statement.getChildren().get(0).getChildren().get(0), statement.getChildren().get(1), context);
    }
    public Function createFunction(ParseResult declaration, ParseResult body, RunContext context) {
        String methodName = declaration.getChildren().get(0).getText();
        List<String> params = declaration.getChildren().get(1).getChildren().stream().map(ParseResult::getText).collect(Collectors.toList());
        return new Function(params) {
            @Override
            public Object run(RunContext context1, ERI eri, Object... vars) {
                if (params.size() != vars.length) {
                    throw new RunIssue("Invalid number of parameters in method call to method: " + methodName);
                }
                RunContext functionContext = new RunContext(context);
                for (int i = 0, paramsSize = params.size(); i < paramsSize; i++) {
                    String param = params.get(i);
                    functionContext.registerVariable(param, Variable.of(vars[i]));
                }
                Object returned = Runner.this.run(functionContext, body, eri);
                if (returned instanceof ReturnedObject) {
                    return ((ReturnedObject) returned).value;
                } else {
                    return returned;
                }
            }
        };
    }

    private Object runStatement(RunContext context, ParseResult header, ParseResult body, ERI eri) {
        RunContext newContext = new RunContext(context);
        if (header.getChildren().isEmpty()) {
            return run(newContext, body, eri);
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
                Object check = evalMultiple(context, headerLine.getChildren().subList(1, headerLine.getChildren().size()), eri);
                if (check instanceof Boolean) {
                    boolean checkVal = (Boolean) check;
                    if (checkVal) {
                        context.setReadyForElse(false);
                        return run(newContext, body, eri);
                    } else {
                        context.setReadyForElse(true);
                        return false;
                    }
                } else {
                    throw new RunIssue("Couldn't evaluate: \"" + headerLine.getText().trim() + "\" as boolean.");
                }
            }
            if (defining.typeOf("else")) {
                if (context.isReadyForElse()) {
                    context.setReadyForElse(false);
                    return run(newContext, body, eri);
                } else {
                    return null;
                }
            }
            if (defining.typeOf("for")) {
                if (headerLine.getChildren().size() != 2 || !headerLine.getChildren().get(1).typeOf("grouping")) {
                    throw new RunIssue("No parentheses used in for statement in: " + defining);
                }
                ParseResults internalChildren = headerLine.getChildren().get(1).getChildren();
                if (
                        internalChildren.size() >= 3 &&
                        (internalChildren.get(1).typeOf("for-each") ||
                                (internalChildren.size() >= 4 && internalChildren.get(1).typeOf("separator") && internalChildren.get(2).typeOf("variable") && internalChildren.get(3).typeOf("for-each")))
                ) {

                    Variable indexVar = null;
                    Variable var;
                    Object iterator;

                    // Validate variable.
                    if (!internalChildren.get(0).typeOf("variable")) {
                        throw new RunIssue("First item in for-each loop isn't a variable.");
                    }

                    // Check for iterator
                    if (internalChildren.size() >= 5 && internalChildren.get(1).typeOf("separator") && internalChildren.get(2).typeOf("variable")) {
                        indexVar = newContext.getOrCreateVariable(internalChildren.get(2).getText());

                        iterator = evalMultiple(newContext, internalChildren.subList(4, internalChildren.size()), eri);
                    } else {
                        iterator = evalMultiple(newContext, internalChildren.subList(2, internalChildren.size()), eri);
                    }
                    // Create main variable
                    var = newContext.getOrCreateVariable(internalChildren.get(0).getText());

                    if (iterator == null) {
                        throw new RunIssue("Can't iterate over null");
                    }
                    if (!(iterator instanceof Iterable) && !iterator.getClass().isArray()) {
                        throw new RunIssue("Tried to start for-each loop with non-iterable.");
                    } else if (iterator.getClass().isArray()) {
                        if (iterator instanceof Object[]) {
                            iterator = Arrays.asList((Object[]) iterator);
                        } else if (iterator instanceof byte[]) {
                            iterator = ReflectionUtils.primitiveArrayToIterable((byte[]) iterator);
                        } else if (iterator instanceof short[]) {
                            iterator = ReflectionUtils.primitiveArrayToIterable((short[]) iterator);
                        } else if (iterator instanceof int[]) {
                            iterator = ReflectionUtils.primitiveArrayToIterable((int[]) iterator);
                        } else if (iterator instanceof long[]) {
                            iterator = ReflectionUtils.primitiveArrayToIterable((long[]) iterator);
                        } else if (iterator instanceof float[]) {
                            iterator = ReflectionUtils.primitiveArrayToIterable((float[]) iterator);
                        } else if (iterator instanceof double[]) {
                            iterator = ReflectionUtils.primitiveArrayToIterable((double[]) iterator);
                        } else if (iterator instanceof char[]) {
                            iterator = ReflectionUtils.primitiveArrayToIterable((char[]) iterator);
                        } else if (iterator instanceof boolean[]) {
                            iterator = ReflectionUtils.primitiveArrayToIterable((boolean[]) iterator);
                        } else {
                            throw new RunIssue("The impossible has happened.");
                        }
                    }
                    Object last = null;
                    int i = 0;
                    for (Object obj : (Iterable<?>) iterator) {
                        if (indexVar != null) {
                            indexVar.set(i);
                            i++;
                        }
                        var.set(obj);
                        last = run(new RunContext(newContext), body, eri);
                        if (last instanceof ReturnedObject) {
                            break;
                        }
                    }
                    return last;
                }

                List<ParseResults> separatedSections = new ArrayList<>();
                ParseResults buffer = new ParseResults();
                for (ParseResult result : internalChildren) {
                    if (result.typeOf("semicolon")) {
                        separatedSections.add(new ParseResults(buffer));
                        buffer.clear();
                    } else {
                        buffer.add(result);
                    }
                }
                if (!buffer.isEmpty()) {
                    separatedSections.add(buffer);
                }
                if (separatedSections.size() == 3) {
                    ParseResults runFirst = separatedSections.get(0);
                    ParseResults checkEachTime = separatedSections.get(1);
                    ParseResults runAfter = separatedSections.get(2);

                    Object last = null;
                    for (evalMultiple(newContext, runFirst, eri); evalAsBoolean(newContext, checkEachTime, eri); evalMultiple(newContext, runAfter, eri)) {
                        last = run(new RunContext(newContext), body, eri);
                        if (last instanceof ReturnedObject) {
                            break;
                        }
                    }
                    return last;
                } else {
                    throw new RunIssue("Invalid inputs in for statement: " + header);
                }
            }
            if (defining.typeOf("while")) {
                ParseResults check = headerLine.getChildren().subList(1, headerLine.getChildren().size());
                Object last = null;
                while ((Boolean) evalMultiple(newContext, check, eri)) {
                    last = run(context, body, eri);
                    if (last instanceof ReturnedObject) {
                        break;
                    }
                }
                return last;
            }

        }
        throw new RunIssue("Couldn't run statement: " + header + " " + body);
    }

    private boolean evalAsBoolean(RunContext newContext, ParseResults checkEachTime, ERI eri) {
        if (checkEachTime.isEmpty()) return true;
        Object result = evalMultiple(newContext, checkEachTime, eri);
        if (result instanceof Boolean) return (boolean) result;
        throw new RunIssue("Can't use " + result + " as boolean.");
    }

    private Object eval(RunContext context, ParseResult result, ERI eri) {
        if (result instanceof ParseResultWrapper) {
            return ((ParseResultWrapper) result).getValue();
        }
        if (result.typeOf("square-bracket-grouping")) {
            if (result.getChildren().isEmpty()) {
                return new ArrayList<>();
            } else {
                List<Object> args = computeMethodParameters(result.getChildren(), "N/A", context, eri);
                return new ArrayList<>(args);
            }
        }
        if (result.typeOf("error")) {
            throw new RunIssue("Triggered by user code.");
        }
        if (result.typeOf("grouping")) {
            return evalMultiple(context, result.getChildren(), eri);
        }
        if (result.typeOf("variable")) {
            if (eri.isLookingForStaticClass()) {
                var searchResult = context.findClass(result.getText());
                if (searchResult.isPresent()) {
                    return StaticClass.forClass(searchResult.get());
                }
            }
            return context.getOrCreateVariable(result.getText()).get();
        }
        if (result.typeOf("number")) {
            return Double.parseDouble(result.getText());
        }
        if (result.typeOf("true")) {
            return true;
        }
        if (result.typeOf("false")) {
            return false;
        }
        if (result.typeOf("string")) {
            return result.getText();
        }
        return null;
    }

    private static final List<ParseType> STRING_TYPES = Arrays.asList(get("plus"), get("times"));
    private static final List<ParseType> MATH_TYPES = Arrays.asList(get("plus"), get("minus"), get("times"), get("division"), get("greater-or-equal"), get("greater"), get("smaller-or-equal"), get("smaller"), get("modulo"));
    private static final List<ParseType> COMPARATORS = Arrays.asList(get("equals"), get("not-equals"), get("or"), get("and"));
    private static final Object UNSET = new Object();

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object evalMultiple(RunContext context, ParseResults results, ERI eri) {
        eri.reachedCheckpoint(0);
        int size = results.size();
        if (size == 1) return eval(context, results.get(0), eri);
        eri.reachedCheckpoint(1);

        if (size >= 3) {
            ParseType type = results.get(1).getType();
            Object left = UNSET;
            Object right = UNSET;
            int equalPos;
            if ((equalPos = results.firstMatch(result -> result.typeOf("assignment") || LineParser.NUMERIC_ASSIGNMENTS.contains(result.getType()))) >= 1) {
                Variable var;
                String varName;
                ParseResult assignment = results.get(equalPos);
                if (results.get(0).typeOf("variable") && equalPos == 1) {
                    var = context.getOrCreateVariable(results.get(0).getText());
                    varName = results.get(0).getText();
                } else if (results.get(equalPos - 1).typeOf("variable") && results.get(equalPos - 2).typeOf("period")) {
                    Object upTo = evalMultiple(context, results.subList(0, equalPos - 2), eri.changedLookingForStaticClass(true).addAutoRemovingCheckpoint(1, eri1 -> eri1.setLookingForStaticClass(false)));
                    if (upTo instanceof InternalObject) {
                        InternalObject obj = (InternalObject) upTo;
                        varName = results.get(equalPos - 1).getText();
                        var = obj.getFields().get(varName);
                    } else {
                        if (upTo == null) throw new RunIssue("Tried to access field of null object.");
                        try {
                            Class<?> upToClass = upTo instanceof StaticClass ? ((StaticClass) upTo).getRepresentedClass() : upTo.getClass();

                            Field field = upToClass.getField(results.get(equalPos - 1).getText());
                            varName = field.getName();
                            var = Variable.fromJavaField(field, Modifier.isStatic(field.getModifiers()) ? null : upTo);
                        } catch (NoSuchFieldException e) {
                            throw new RunIssue("No field named \"" + results.get(equalPos - 1).getText() + "\" on object \"" + upTo + "\".");
                        }
                    }
                } else if (results.get(equalPos - 1).typeOf("square-bracket-grouping")) {
                    varName = "array";
                    Object array = evalMultiple(context, results.subList(0, equalPos - 1), eri);
                    boolean isArray = ReflectionUtils.isArray(array);
                    boolean isList = !isArray && array instanceof List<?>;
                    boolean isMap = !isArray && !isList && array instanceof Map<?, ?>;
                    Object insideGrouping = evalMultiple(context, results.get(equalPos - 1).getChildren(), eri);
                    if (!isArray && !isList && !isMap) {
                        throw new RunIssue("Tried to access " + array + " as an array, list, or map but its type isn't a valid type of either.");
                    }
                    if (insideGrouping == null) {
                        throw new RunIssue("Can't search " + array + " for key " + null);
                    }
                    if (!isMap) {
                        if (!ReflectionUtils.isNum(insideGrouping.getClass())) {
                            throw new RunIssue("Can't search " + array + " for non integer index");
                        }
                        int index = ((Number) insideGrouping).intValue();
                        if (isArray) {
                            var = new Variable() {
                                @Override
                                public void set(Object obj) {
                                    Array.set(array, index, obj);
                                }

                                @Override
                                public Object get() {
                                    return Array.get(array, index);
                                }
                            };
                        } else {
                            List list = (List) array;
                            var = new Variable() {
                                @Override
                                public void set(Object obj) {
                                    if (index == list.size()) {
                                        list.add(obj);
                                    } else {
                                        list.set(index, obj);
                                    }
                                }

                                @Override
                                public Object get() {
                                    return list.get(index);
                                }
                            };
                        }
                    } else {
                        throw new RunIssue("Support for maps not yet implemented.");
                    }
                } else {
                    throw new RunIssue("Illegal use of assignment.");
                }
                if (assignment.typeOf("assignment")) {
                    Object val = evalMultiple(context, results.subList(equalPos + 1), eri);
                    var.set(val);
                    return val;
                } else if (LineParser.NUMERIC_ASSIGNMENTS.contains(assignment.getType())) {
                    Object varVal = var.get();
                    if (varVal == null) {
                        varVal = 0;
                    }
                    if (!(varVal instanceof Number)) {
                        throw new RunIssue("Can't perform \"" + assignment.getText() + "\" on variable \"" + varName + "\" because its value isn't a number.");
                    }
                    if (right == UNSET) {
                        right = evalMultiple(context, results.subList(equalPos + 1), eri);
                    }
                    if (!(right instanceof Number)) {
                        throw new RunIssue("Can't perform \"" + assignment.getText() + "\" on variable \"" + varName + "\" because value \"" + right + "\" isn't a number.");
                    }
                    double varAsNum = ((Number) varVal).doubleValue();
                    double rightAsNum = ((Number) right).doubleValue();
                    Object newVal;
                    if (assignment.typeOf("plus-equals")) {
                        newVal = varAsNum + rightAsNum;
                    } else if (assignment.typeOf("minus-equals")) {
                        newVal = varAsNum - rightAsNum;
                    } else if (assignment.typeOf("times-equals")) {
                        newVal = varAsNum * rightAsNum;
                    } else if (assignment.typeOf("divide-equals")) {
                        newVal = varAsNum / rightAsNum;
                    } else if (assignment.typeOf("modulo-equals")) {
                        newVal = varAsNum % rightAsNum;
                    } else {
                        throw new IllegalStateException();
                    }
                    var.set(newVal);
                    return newVal;
                }
            }
            if (results.get(1).typeOf("period")) {
                ParseResult zero = results.get(0);
                if (zero.typeOf("variable") || zero.typeOf("for-each") || zero.typeOf("elif")) {
                    Optional<Class<?>> classLookupResult = context.findClass(zero.getText());
                    if (classLookupResult.isPresent()) {
                        left = StaticClass.forClass(classLookupResult.get());
                    } else if (staticContext != null) {
                        Optional<ClassRunner> internalClass = staticContext.findClass(zero.getText());
                        if (internalClass.isPresent()) {
                            left = internalClass.get();
                        }
                    }
                }
                if (left == UNSET) {
                    left = eval(context, zero, eri);
                }
                ParseResults member = results.subList(2, results.size());
                if (member.get(0).typeOf("for-each") || member.get(0).typeOf("elif")) {
                    member.set(0, new ParseResult(get("variable"), member.get(0).getText()));
                }
                if (member.size() >= 1 && member.get(0).typeOf("variable")) { // Field
                    String fieldName = member.get(0).getText();
                    // Handle length of arrays.
                    if (fieldName.equals("length") && ReflectionUtils.isArray(left)) {
                        return wrappedEvalMultiple(context, ReflectionUtils.length(left), member, 1, eri);
                    }
                    // Handle "System.class"
                    if (fieldName.equals("class")) {
                        if (left instanceof StaticClass) {
                            return wrappedEvalMultiple(context, ((StaticClass) left).getRepresentedClass(), member, 1, eri);
                        } else if (left instanceof Class<?>) {
                            return wrappedEvalMultiple(context, left, member, 1, eri);
                        } else if (left != null) {
                            return wrappedEvalMultiple(context, left.getClass(), member, 1, eri);
                        } else {
                            return null;
                        }
                    }
                    if (left instanceof InternalObject) {
                        InternalObject asInternalObject = (InternalObject) left;
                        return asInternalObject.getFields().get(fieldName).get();
                    }
                    try {
                        Field field;
                        if (left instanceof StaticClass) {
                            field = ((StaticClass) left).getRepresentedClass().getField(fieldName);
                        } else {
                            field = left.getClass().getField(fieldName);
                        }
                        try {
                            if (Modifier.isStatic(field.getModifiers())) {
                                left = null;
                            }
                            return wrappedEvalMultiple(context, field.get(left), member, 1, eri);
                        } catch (IllegalAccessException e) {
                            throw new RunIssue("Can't access field \"" + field + "\"");
                        }
                    } catch (NoSuchFieldException e) {
                        throw new RunIssue("No field named \"" + fieldName + "\" on " + left + ".");
                    }
                } else if (member.size() >= 2) { // Method
                    String methodName = member.get(0).getText();
                    List<Object> arguments = computeMethodParameters(member.get(1).getChildren(), methodName, context, eri);

                    // Check if the object who's method is trying to be accessed is an internal object.
                    if (left instanceof InternalObject) {
                        Object[] args = arguments.toArray();
                        return ((InternalObject) left).getMethod(methodName, args).getFunction().run(context, eri, args);
                    }

                    // Check if we are looking for the static method of an internal class.
                    if (left instanceof ClassRunner) {
                        Object[] args = arguments.toArray();
                        return ((ClassRunner) left).getStaticMethodsMap().get(new MethodSignature(methodName, args.length)).getFunction().run(context, eri, args);
                    }

                    Optional<Method> method;
                    if (left instanceof StaticClass) {
                        method = ReflectionUtils.findMethod(
                                Arrays.stream(((StaticClass) left).getRepresentedClass().getMethods()).filter(testMethod -> Modifier.isStatic(testMethod.getModifiers()))
                                , methodName, arguments.toArray()
                        );
                    } else {
                        method = ReflectionUtils.findAccessibleMethod(left, methodName, arguments.toArray());
                    }
                    if (method.isEmpty()) {
                        throw new RunIssue("No method exists on object: \"" + left + "\" (" + zero + ") named: \"" + methodName + "\" which matches arguments: " + arguments);
                    }
                    try {
                        Object use;
                        if (Modifier.isStatic(method.get().getModifiers())) {
                            use = null;
                        } else {
                            use = left;
                        }
                        if (!method.get().canAccess(use)) {
                            method.get().setAccessible(true);
                        }
                        return wrappedEvalMultiple(context, ReflectionUtils.invoke(method.get(), use, arguments.toArray()), member, 2, eri);
                    } catch (IllegalAccessException e) {
                        throw new RunIssue("Can't access method \"" + method.get().toGenericString() + "\".");
                    } catch (InvocationTargetException e) {
                        throw new RunIssue("No method exists on object: \"" + left + "\" (" + zero + ") named: \"" + methodName + "\" which matches arguments: " + arguments);
                    }
                }
            }
            int instanceOfLoc = results.firstTypeOf("instanceof");
            if (instanceOfLoc != -1) {
                if (instanceOfLoc + 1 < results.size()) {
                    ParseResults upToInstanceOf = results.subList(0, instanceOfLoc);
                    left = evalMultiple(context, upToInstanceOf, eri);
                    String checkType = results.get(instanceOfLoc + 1).getText();
                    Class<?> typeVal = context.findClass(checkType).orElseThrow(() -> new RunIssue("Couldn't find type \"" + checkType + "\""));
                    return typeVal.isInstance(left);
                } else {
                    throw new RunIssue(results.get(instanceOfLoc).getText().trim() + " isn't followed by a type.");
                }
            }
            if (results.get(0).typeOf("new")) {
                if (results.get(1).typeOf("method-name") && results.get(2).typeOf("method-arguments")) {
                    String name = results.get(1).getText();
                    Optional<Class<?>> referencedClass = context.findClass(name);
                    if (referencedClass.isPresent()) {
                        Object[] arguments = computeMethodParameters(results.get(2).getChildren(), "new " + name + "(...)", context, eri).toArray();
                        Optional<Constructor<Object>> constructor = ReflectionUtils.findConstructor(referencedClass.get(), arguments);
                        if (constructor.isPresent()) {
                            arguments = ReflectionUtils.convertArgs(constructor.get().getParameters(), arguments);
                            try {
                                return wrappedEvalMultiple(context, constructor.get().newInstance(arguments), results, 3, eri);
                            } catch (InstantiationException | InvocationTargetException e) {
                                throw new RunIssue("Couldn't instantiate " + name + ".");
                            } catch (IllegalAccessException e) {
                                throw new RunIssue("Couldn't access class " + referencedClass.get() + ".");
                            }
                        } else {
                            throw new RunIssue("No constructor for class \"" + referencedClass.get() + "\" with parameters that match " + Arrays.toString(arguments) + ".");
                        }
                    } else {
                        if (staticContext != null && staticContext.getLocation() != null) {
                            Optional<ClassRunner> internalClass = staticContext.findClass(name);
                            if (internalClass.isPresent()) {
                                ClassRunner classRunner = internalClass.get();
                                ClassDefinition definition = classRunner.getDefinition();

                                Object[] arguments = computeMethodParameters(results.get(2).getChildren(), "new " + name + "(...)", context, eri).toArray();
                                ConstructorDefinition constructor = definition.getConstructor(arguments);

                                try {
                                    return classRunner.newInstance(constructor, arguments);
                                } catch (IllegalArgumentException e) {
                                    throw new RunIssue("No constructor for class \"" + definition.getName() + "\" with parameters that match " + Arrays.toString(arguments) + ".");
                                }
                            }
                        }
                        throw new RunIssue("Couldn't find class named: " + name);
                    }
                } else {
                    throw new RunIssue("Keyword 'new' isn't followed by a constructor call.");
                }
            }
            if (STRING_TYPES.contains(type)) {
                if (left == UNSET) {
                    left = eval(context, results.get(0), eri);
                }
                right = evalMultiple(context, results.subList(2), eri);
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
                    left = eval(context, results.get(0), eri);
                }
                if (left instanceof Number) {
                    if (right == UNSET) {
                        right = evalMultiple(context, results.subList(2), eri);
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
                System.err.println(results.stream().map(ParseResult::toPrettyString).collect(Collectors.joining("\n")));
                throw new RunIssue("Couldn't calculate " + results);
            }
            if (COMPARATORS.contains(type)) {
                if (left == UNSET) {
                    left = eval(context, results.get(0), eri);
                }
                if (type.equals(get("equals")) || type.equals(get("not-equal"))) {
                    if (right == UNSET) {
                        right = evalMultiple(context, results.subList(2), eri);
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
                            right = evalMultiple(context, results.subList(2), eri);
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
                            right = evalMultiple(context, results.subList(2), eri);
                        }
                        Object rightObj = right;
                        if (rightObj instanceof Boolean) {
                            return leftVal || (Boolean) rightObj;
                        }
                    }
                }
            }
        }
        
        if (size >= 2) {
            ParseResult resultZero = results.get(0);
            if (results.get(1).typeOf("square-bracket-grouping")) {
                ParseResult arrayGrouping = results.get(1);
                Object left = eval(context, resultZero, eri);
                boolean isArray = ReflectionUtils.isArray(left);
                boolean isList = !isArray && left instanceof List<?>;
                boolean isMap = !isArray && !isList && left instanceof Map<?, ?>;
                Object insideGrouping = evalMultiple(context, arrayGrouping.getChildren(), eri);
                if (!isArray && !isList && !isMap) {
                    throw new RunIssue("Tried to access " + left + " as an array, list, or map but its type isn't a valid type of either.");
                }
                if (insideGrouping == null) {
                    throw new RunIssue("Can't search " + left + " for key " + null);
                }
                if (!isMap) {
                    if (!ReflectionUtils.isNum(insideGrouping.getClass())) {
                        throw new RunIssue("Can't search " + left + " for non integer index");
                    }
                    int index = ((Number) insideGrouping).intValue();
                    Object value;
                    if (isArray) {
                        value = Array.get(left, index);
                    } else /* (isList) */ {
                        value = ((List<?>) left).get(index);
                    }
                    return wrappedEvalMultiple(context, value, results, 2, eri);
                } else {
                    throw new RunIssue("Support for maps not yet implemented.");
                }
            }
            if (resultZero.typeOf("variable")) {
                ParseResult alteration = results.get(1);
                if (alteration.typeOf("increment")) {
                    Variable var = context.getOrCreateVariable(results.get(0).getText(), () -> 0D);
                    Object val = var.get();
                    if (val instanceof Number) {
                        var.set(((Number) val).doubleValue() + 1);
                        return wrappedEvalMultiple(context, val, results, 2, eri);
                    }
                }
                if (alteration.typeOf("decrement")) {
                    Variable var = context.getOrCreateVariable(results.get(0).getText(), () -> 0D);
                    Object val = var.get();
                    if (val instanceof Number) {
                        var.set(((Number) val).doubleValue() - 1);
                        return wrappedEvalMultiple(context, val, results, 2, eri);
                    }
                }
                if (alteration.typeOf("square")) {
                    Variable var = context.getOrCreateVariable(results.get(0).getText(), () -> 0D);
                    Object val = var.get();
                    if (val instanceof Number) {
                        double doubleVal = ((Number) val).doubleValue();
                        var.set(doubleVal * doubleVal);
                        return wrappedEvalMultiple(context, val, results, 2, eri);
                    } else if (val instanceof String) {
                        String newVal = ((String) val).repeat(2);
                        var.set(newVal);
                        return wrappedEvalMultiple(context, val, results, 2, eri);
                    }
                }
            }
            if (resultZero.typeOf("increment")) {
                if (results.get(1).typeOf("variable")) {
                    Variable var = context.getOrCreateVariable(results.get(1).getText(), () -> 0);
                    Object val = var.get();
                    if (val instanceof Number) {
                        double newVal = ((Number) val).doubleValue() + 1;
                        var.set(newVal);
                        return wrappedEvalMultiple(context, newVal, results, 2, eri);
                    }
                }
            }
            if (resultZero.typeOf("decrement")) {
                if (results.get(1).typeOf("variable")) {
                    Variable var = context.getOrCreateVariable(results.get(1).getText(), () -> 0);
                    Object val = var.get();
                    if (val instanceof Number) {
                        double newVal = ((Number) val).doubleValue() - 1;
                        var.set(newVal);
                        return wrappedEvalMultiple(context, newVal, results, 2, eri);
                    }
                }
            }
            if (resultZero.typeOf("square")) {
                if (results.get(1).typeOf("variable")) {
                    Variable var = context.getOrCreateVariable(results.get(1).getText(), () -> 0);
                    Object val = var.get();
                    if (val instanceof Number) {
                        double newVal = ((Number) val).doubleValue();
                        newVal = newVal * newVal;
                        var.set(newVal);
                        return wrappedEvalMultiple(context, newVal, results, 2, eri);
                    } else if (val instanceof String) {
                        String newVal = ((String) val).repeat(2);
                        var.set(newVal);
                        return wrappedEvalMultiple(context, newVal, results, 2, eri);
                    }
                }
            }
            if (resultZero.typeOf("method-name")) {
                if (results.get(1).typeOf("method-arguments")) {
                    String name = results.get(0).getText();
                    Optional<Function> function = context.getFunction(name);
                    if (function.isEmpty()) {
                        if (!context.isJavaMethodsEmpty()) {
                            List<Object> params = computeMethodParameters(results.get(1).getChildren(), name, context, eri);
                            Optional<RunnableMethod> result = ReflectionUtils.findExecutableInternal(
                                    context.getStaticMethods().stream().filter(runnableMethod -> runnableMethod.getName().equals(name)),
                                    params.toArray()
                            );
                            if (result.isPresent()) {
                                try {
                                    return result.get().run(params.toArray());
                                } catch (InvocationTargetException e) {
                                    throw new RunIssue("No method exists named: \"" + name + "\" which matches arguments: " + params);
                                } catch (IllegalAccessException e) {
                                    throw new RunIssue("Can't access method \"" + result.get().getMethod().toGenericString() + "\".");
                                }
                            }
                        }
                        throw new RunIssue("No function with name: \"" + results.get(0).getText() + "\" exists.");
                    }
                    return function.get().run(context, eri, computeMethodParameters(results.get(1).getChildren(), results.get(0).getText(), context, function.get().getParams().size(), eri).toArray());
                } else {
                    throw new RunIssue("No arguments for method call to method named: " + results.get(0).getText());
                }
            }
            if (resultZero.typeOf("return")) {
                ParseResults subList = results.subList(1);
                if (subList.isEmpty()) {
                    return new ReturnedObject(null);
                } else {
                    return new ReturnedObject(evalMultiple(context, subList, eri));
                }
            }
            if (resultZero.typeOf("minus")) {
                Object right = evalMultiple(context, results.subList(1), eri);
                if (right instanceof Number) {
                    return -(((Number) right).doubleValue());
                }
            }
            if (resultZero.typeOf("plus")) {
                Object right = evalMultiple(context, results.subList(1), eri);
                if (right instanceof Number) {
                    return Math.abs(((Number) right).doubleValue());
                }
            }
            if (resultZero.typeOf("negate")) {
                Object right = evalMultiple(context, results.subList(1), eri);
                if (right instanceof Boolean) {
                    return !(Boolean) right;
                }
            }
        }

        throw new RunIssue("Couldn't run: " + results);
    }

    private List<Object> computeMethodParameters(ParseResults params, String methodName, RunContext context, ERI eri) {
        return computeMethodParameters(params, methodName, context, -1, eri);
    }
    private List<Object> computeMethodParameters(ParseResults params, String methodName, RunContext context, int expected, ERI eri) {
        List<ParseResults> arguments = separateByType(params, get("separator"));
        if (expected > 0 && arguments.size() != expected) {
            throw new RunIssue("Invalid number of parameters in call to function named: " + methodName + ". Found " + arguments.size() + " expected " + expected);
        }
        return arguments.stream().map(results -> evalMultiple(context, results, eri)).collect(Collectors.toList());
    }
    private List<ParseResults> separateByType(ParseResults results, ParseType type) {
        List<ParseResults> arguments = new ArrayList<>();
        ParseResults buffer = new ParseResults();
        for (ParseResult param : results) {
            if (param.getType().equals(type)) {
                arguments.add(new ParseResults(buffer));
                buffer.clear();
            } else {
                buffer.add(param);
            }
        }
        if (!buffer.isEmpty()) {
            arguments.add(buffer);
        }
        return arguments;
    }

    private static final ParseType GROUPING = get("grouping");
    private static ParseResults collapseGrouping(ParseResult result) {
        if (result.typeOf(GROUPING)) {
            ParseResults children = result.getChildren();
            if (children.size() == 1 && children.get(0).typeOf(GROUPING)) {
                return collapseGrouping(children.get(0));
            } else {
                return children;
            }
        } else {
            throw new IllegalArgumentException("ParseResult's type doesn't equal typeOf grouping.");
        }
    }

    private Object wrappedEvalMultiple(RunContext context, Object val, ParseResults results, int from, ERI eri) {
        if (from >= results.size()) {
            return val;
        } else {
            ParseResultWrapper wrapper = new ParseResultWrapper("N/A", val);
            ParseResults subList = results.subList(from);
            subList.add(0, wrapper);
            return evalMultiple(context, subList, eri);
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

        @Override
        public String toPrettyString(int indentation) {
            if (getChildren().isEmpty()) {
                return "\t".repeat(indentation) + getType() + " (" + value + ")";
            } else {
                return super.toPrettyString(indentation);
            }
        }

    }

    private void error(ParseResult result) {
        error(Collections.singletonList(result));
    }
    private void error(List<?> at) {
        throw new RunIssue("Failed at: " + at);
    }

    public void setOut(PrintStream out) {
        this.out = out;
    }

    public PrintStream getOut() {
        return out;
    }

    public boolean isTrackEvaluating() {
        return trackEvaluating;
    }

    public void setTrackEvaluating(boolean trackEvaluating) {
        this.trackEvaluating = trackEvaluating;
    }

    public boolean isTrackRunningLine() {
        return trackRunningLine;
    }

    public void setTrackRunningLine(boolean trackRunningLine) {
        this.trackRunningLine = trackRunningLine;
    }

    public boolean isUsingDelay() {
        return usingDelay;
    }

    public void setUsingDelay(boolean usingDelay) {
        this.usingDelay = usingDelay;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public Runnable getCurrentLineInvalidated() {
        return currentLineInvalidated;
    }

    public void setCurrentLineInvalidated(Runnable currentLineInvalidated) {
        this.currentLineInvalidated = currentLineInvalidated;
    }

    public RunContext getGlobal() {
        return global;
    }

    public ParseResult getCurrentLine() {
        return currentLine;
    }

}
