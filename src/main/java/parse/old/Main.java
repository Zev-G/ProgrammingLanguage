package parse.old;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import static parse.old.StructureComponents.*;

public final class Main {

    public static final ParseType PLUS_EQUALS = new ParseType("plus equals");
    public static final ParseType MINUS_EQUALS = new ParseType("minus equals");
    public static final ParseType TIMES_EQUALS = new ParseType("times equals");
    public static final ParseType DIVIDE_EQUALS = new ParseType("divide equals");

    public static final ParseType PLUS = new ParseType("plus");
    public static final ParseType MINUS = new ParseType("minus");
    public static final ParseType TIMES = new ParseType("times");
    public static final ParseType DIVIDE = new ParseType("divide");

    public static final ParseType INCREMENT = new ParseType("increment");
    public static final ParseType DECREMENT = new ParseType("decrement");

    public static final ParseType EQUALS = new ParseType("equals");
    public static final ParseType AND = new ParseType("and");
    public static final ParseType OR = new ParseType("or");
    public static final ParseType NEGATE = new ParseType("negate");

    public static final ParseType IF = new ParseType("if");

    public static final ParseType VARIABLE = new ParseType("variable");
    public static final ParseType ACTION = new ParseType("action");
    public static final ParseType ASSIGNMENT = new ParseType("assignment");

    public static final ParseType NUMBER = new ParseType("number");
    public static final ParseType BOOLEAN = new ParseType("boolean");

    public static final ParseType STATEMENT = new ParseType("statement");
    public static final ParseType RUNNABLE = new ParseType("runnable");

    public static final List<ParseType> VALUE_TYPES = Arrays.asList(NUMBER, BOOLEAN, VARIABLE);

    private static final String[] PORTION_SEPARATORS = { " " };
    private static final String[] PORTIONS = { "!", "&&", "&", "||", "|", "==", "+=", "-=", "*=", "/=", "=", "--", "++", "+", "-", "*", "=" };

    public static void main(String[] args) {
        String code = new BufferedReader(new InputStreamReader(Objects.requireNonNull(parse.Main.class.getClassLoader().getResourceAsStream("code.txt")))).lines().collect(Collectors.joining("\n"));
        System.out.println("RUNNING:\n" + code.indent(4));

        SimpleSubStructure root = new SimpleSubStructure(new ParseType("root"), text -> {
            List<Portion> portions = new ArrayList<>();
            if (text.getType().equals("multi-line")) {
                int openBracket = text.getText().indexOf("{");
                int closeBracket = text.getText().lastIndexOf("}");
                Portion statement = new Portion("statement", text.getText().substring(0, openBracket));
                portions.add(statement);
                text = new Portion(text.getType(), text.getText().substring(openBracket + 1, closeBracket));
            }
            int within = 0;
            int line = 0;
            int onLine = 0;
            boolean inSingleLineComment = false;
            boolean inMultiLineComment = false;
            boolean inString = false;
            StringBuilder builder = new StringBuilder();
            char[] charArray = text.getText().toCharArray();
            for (int i = 0, charArrayLength = charArray.length; i < charArrayLength; i++) {
                char c = charArray[i];
                String after = text.getText().substring(i);
                if (after.startsWith("//") && !inString) {
                    inSingleLineComment = true;
                }
                if (c != '\n' && c != '\r') {
                    onLine++;
                } else if (inSingleLineComment) {
                    inSingleLineComment = false;
                    onLine = 0;
                    line++;
                } else {
                    onLine = 0;
                    line++;
                }
                if (inSingleLineComment) {
                    continue;
                }
                if (c == '{') {
                    within++;
                } else if (c == '}') {
                    within--;
                    if (within == 0) {
                        builder.append(c);
                        portions.add(new Portion("multi-line", builder.toString()));
                        builder = new StringBuilder();
                        continue;
                    }
                    if (within < 0) {
                        System.err.println("Closing bracket at: " + line + ":" + onLine + " has no opening bracket.");
                    }
                } else if (within == 0) {
                    if (c == ';') {
                        portions.add(new Portion("line", builder.toString()));
                        builder = new StringBuilder();
                        continue;
                    }
                }

                if (c != '\n' && c != '\r') {
                    builder.append(c);
                }
            }
            System.out.println(portions);
            return portions.toArray(new Portion[0]);
        });
        root.setParseValidator(portion -> portion != null && (portion.getType().equals("multi-line") || portion.getType().equals("root")));
        root.getChildren().add(root);

        SimpleSubStructure statementStructure = new SimpleSubStructure(
                STATEMENT,
                new Dissectors.PortionDissector(Collections.singletonList(" "), Collections.singletonList("if"))
        );
        statementStructure.setParseValidator(portion -> portion != null && portion.getType().equals("statement"));
        statementStructure.getChildren().add(literal("if", IF));
        root.getChildren().add(statementStructure);

        SimpleSubStructure lineStructure = new SimpleSubStructure(RUNNABLE, new Dissectors.PortionDissector(Arrays.asList(PORTION_SEPARATORS), Arrays.asList(PORTIONS)));
        lineStructure.setParseValidator(portion -> portion == null || portion.getType() == null || !portion.getType().equals("multi-line"));
        lineStructure.getChildren().addAll(Arrays.asList(
                literal("!", NEGATE), literal("==", EQUALS), literal("&&", AND), literal("&", AND), literal("||", OR), literal("|", OR),
                literal("=", ASSIGNMENT), literal("--", DECREMENT), literal("++", INCREMENT), literal("+=", PLUS_EQUALS), literal("-=", MINUS_EQUALS), literal("*=", TIMES_EQUALS), literal("/=", DIVIDE_EQUALS),
                literal("+", PLUS), literal("-", MINUS), literal("*", TIMES), literal("/", DIVIDE),
                literal("true", BOOLEAN), literal("false", BOOLEAN),
                fromPredicate(Main::isNum, NUMBER), literal("print", ACTION), matches("[^=+\\-\n ]+", VARIABLE)
        ));
        root.getChildren().add(lineStructure);
        statementStructure.getChildren().add(lineStructure);

        ParsedObject result = Parser.parse(code, root).orElseThrow();
        result.print();
        run(result);
    }

    public static void run(ParsedObject object) {
        Map<String, Object> variables = new HashMap<>();
        run(object, variables);
    }

    private static void run(ParsedObject object, Map<String, Object> vars) {
        if (object.getType().equals(STATEMENT)) {
            runStatement(object, vars);
            return;
        }
        if (runChildrenOf(object)) {
            if (!object.getChildren().isEmpty()) {
                evalMultiple(object.getChildren(), vars);
            }
        } else {
            if (object.getChildren().isEmpty()) {
                return;
            }
            if (object.getChildren().get(0).getType().equals(STATEMENT)) {
                runStatement(object, vars);
                return;
            }
            for (ParsedObject child : object.getChildren()) {
                if (runChildrenOf(child)) {
                    if (!child.getChildren().isEmpty()) {
                        evalMultiple(child.getChildren(), vars);
                    }
                } else {
                    run(child, vars);
                }
            }
        }
    }

    private static void runStatement(ParsedObject object, Map<String, Object> vars) {
        if (object.getChildren().size() == 0) {
            return;
        }
        ParsedObject header = object.getChildren().get(0);
        if (!header.getChildren().isEmpty()) {
            ParsedObject statementDefiner = header.getChildren().get(0);
            if (statementDefiner.getType() == IF) {
                List<ParsedObject> leaves = leaves(header.getChildren().subList(1, header.getChildren().size()));
                Object result = evalMultiple(leaves, vars);
                if (!((Boolean) result)) {
                    return;
                }
            } else {
                run(statementDefiner, vars);
            }
        }
        for (ParsedObject run : object.getChildren().subList(1, object.getChildren().size())) {
            run(run, vars);
        }
    }

    private static List<ParsedObject> leaves(Collection<ParsedObject> objects) {
        List<ParsedObject> result = new ArrayList<>();
        for (ParsedObject child : objects) {
            result.addAll(leaves(child));
        }
        return result;
    }
    private static List<ParsedObject> leaves(ParsedObject object) {
        if (object.getChildren().isEmpty()) {
            return Collections.singletonList(object);
        }
        List<ParsedObject> objects = new ArrayList<>();
        for (ParsedObject child : object.getChildren()) {
            objects.addAll(leaves(child));
        }
        return objects;
    }

    private static boolean runChildrenOf(ParsedObject object) {
        for (ParsedObject child : object.getChildren()) {
            if (!child.getChildren().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static Number getNumVar(String name, Map<String, Object> vars) {
        if (!vars.containsKey(name)) {
            return 0;
        } else {
            return (Number) vars.get(name);
        }
    }

    private static boolean canEval(ParsedObject obj) {
        Object type = obj.getType();
        return type == VARIABLE || type == NUMBER || type == BOOLEAN;
    }
    private static Object evalLeaf(ParsedObject obj, Map<String, Object> vars) {
        if (obj.getType().equals(VARIABLE)) {
            return vars.get(obj.getText());
        }
        if (obj.getType().equals(NUMBER)) {
            return Long.parseLong(obj.getText().trim());
        }
        if (obj.getType().equals(BOOLEAN)) {
            String text = obj.getText().trim();
            if (text.equals("true")) {
                return true;
            } else if (text.equals("false")) {
                return false;
            }
        }
        throw new IllegalArgumentException();
    }

    private static Object evalMultiple(List<ParsedObject> sequence, Map<String, Object> vars) {
        if (sequence.size() == 1) {
            return evalLeaf(sequence.get(0), vars);
        }
        if (sequence.size() >= 2) {
            if (sequence.get(0).getType().equals(NEGATE)) {
                return !((Boolean) evalMultiple(sequence.subList(1, sequence.size()), vars));
            }
            if (sequence.get(0).getType().equals(ACTION)) {
                System.out.println(evalMultiple(sequence.subList(1, sequence.size()), vars));
                return null;
            }
            if (sequence.size() == 2) {
                if (sequence.get(0).getType().equals(VARIABLE)) {
                    String varName = sequence.get(0).getText();
                    if (sequence.get(1).getType().equals(INCREMENT)) {
                        vars.put(varName, getNumVar(varName, vars).longValue() + 1);
                        return null;
                    }
                    if (sequence.get(2).getType().equals(DECREMENT)) {
                        vars.put(varName, getNumVar(varName, vars).longValue() - 1);
                        return null;
                    }
                }
            }
        }
        if (sequence.size() >= 3) {
            Object typeOne = sequence.get(1).getType();
            String sequenceOneText = sequence.get(1).getText();
            if (sequence.get(0).getType().equals(VARIABLE)) {
                String varName = sequence.get(0).getText();
                Object varVal = vars.get(varName);
                if (typeOne.equals(ASSIGNMENT)) {
                    vars.put(varName, evalMultiple(sequence.subList(2, sequence.size()), vars));
                    return vars.get(varName);
                }
                if (typeOne.equals(PLUS_EQUALS)) {
                    Object post = evalMultiple(sequence.subList(2, sequence.size()), vars);
                    vars.put(varName, ((Number) varVal).longValue() + ((Number) post).longValue());
                    return vars.get(varName);
                }
                if (typeOne.equals(MINUS_EQUALS)) {
                    Object post = evalMultiple(sequence.subList(2, sequence.size()), vars);
                    vars.put(varName, ((Number) varVal).longValue() - ((Number) post).longValue());
                    return vars.get(varName);
                }
                if (typeOne.equals(TIMES_EQUALS)) {
                    Object post = evalMultiple(sequence.subList(2, sequence.size()), vars);
                    vars.put(varName, ((Number) varVal).longValue() * ((Number) post).longValue());
                    return vars.get(varName);
                }
                if (typeOne.equals(DIVIDE_EQUALS)) {
                    Object post = evalMultiple(sequence.subList(2, sequence.size()), vars);
                    vars.put(varName, ((Number) varVal).longValue() / ((Number) post).longValue());
                    return vars.get(varName);
                }
            }
            if (canEval(sequence.get(0))) {
                Object val = evalLeaf(sequence.get(0), vars);
                if (typeOne.equals(PLUS)) {
                    Number numAsVal = (Number) val;
                    return numAsVal.longValue() + ((Number) evalMultiple(sequence.subList(2, sequence.size()), vars)).longValue();
                }
                if (typeOne.equals(MINUS)) {
                    Number numAsVal = (Number) val;
                    return numAsVal.longValue() - ((Number) evalMultiple(sequence.subList(2, sequence.size()), vars)).longValue();
                }
                if (typeOne.equals(TIMES)) {
                    Number numAsVal = (Number) val;
                    return numAsVal.longValue() * ((Number) evalMultiple(sequence.subList(2, sequence.size()), vars)).longValue();
                }
                if (typeOne.equals(DIVIDE)) {
                    Number numAsVal = (Number) val;
                    return numAsVal.longValue() / ((Number) evalMultiple(sequence.subList(2, sequence.size()), vars)).longValue();
                }

                if (typeOne.equals(OR)) {
                    Boolean valAsBool = (Boolean) val;
                    if (sequenceOneText.equals("|")) {
                        return valAsBool | (Boolean) evalMultiple(sequence.subList(2, sequence.size()), vars);
                    } else if (sequenceOneText.equals("||")) {
                        return valAsBool || (Boolean) evalMultiple(sequence.subList(2, sequence.size()), vars);
                    }
                }
                if (typeOne.equals(AND)) {
                    Boolean valAsBool = (Boolean) val;
                    if (sequenceOneText.equals("&")) {
                        return valAsBool & (Boolean) evalMultiple(sequence.subList(2, sequence.size()), vars);
                    } else if (sequenceOneText.equals("&&")) {
                        return valAsBool && (Boolean) evalMultiple(sequence.subList(2, sequence.size()), vars);
                    }
                }
            }
        }
        throw new RuntimeException("Failed to run sequence: " + sequence + " variables: " + vars);
    }

    public static boolean isNum(String text) {
        try {
            Long.parseLong(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
