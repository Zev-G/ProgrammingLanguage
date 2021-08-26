package parse.example;

import parse.ParseResult;
import parse.ParseResults;
import parse.example.run.oo.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClassParser {

    public static ClassDefinition parse(ParseResult parsedClass, ClassHeader header) {
        return new ClassParser(parsedClass, header).parse();
    }

    private final ParseResult definition;
    private final ClassHeader header;

    public ClassParser(ParseResult definition, ClassHeader header) {
        this.definition = definition;
        this.header = header;
    }

    public ClassDefinition parse() {
        List<FieldDefinition> fields = new ArrayList<>();
        List<MethodDefinition> methods = new ArrayList<>();
        List<ConstructorDefinition> constructors = new ArrayList<>();
        for (ParseResult parsed : definition.getChildren()) {
            if (parsed.typeOf("line")) {
                fieldCheck(parsed).ifPresent(fieldCheckResult -> fields.add(fieldCheckResult.toDefinition()));
            }
            if (parsed.typeOf("statement")) {
                var methodCheck = methodCheck(parsed);
                if (methodCheck.isPresent()) {
                    MethodCheckResult result = methodCheck.get();
                    if (result.constructor) {
                        constructors.add(result.toConstructorDefinition());
                    } else {
                        methods.add(result.toDefinition());
                    }
                }
            }
        }

        return new ClassDefinition(
                header.isAnonymous(), header.getName(), header.getSuperClass(), header.isAbstract(), header.isFinal(), header.getAccessModifier(),
                fields.toArray(new FieldDefinition[0]), methods.toArray(new MethodDefinition[0]), constructors.toArray(new ConstructorDefinition[0])
        );
    }

    private Optional<FieldCheckResult> fieldCheck(ParseResult line) {
        if (!line.typeOf("line")) return Optional.empty();

        boolean static_ = false;
        boolean final_ = false;
        AccessModifier accessModifier = null;
        ParseResult variable = null;
        ParseResults value = new ParseResults();

        boolean foundVariable = false;
        boolean foundAssignment = false;
        for (ParseResult parsed : line.getChildren()) {
            if (parsed.typeOf("variable")) {
                if (!foundVariable) {
                    foundVariable = true;
                    variable = parsed;
                    continue;
                } else if (!foundAssignment) {
                    return Optional.empty();
                }
            }
            if (parsed.typeOf("static")) {
                if (foundVariable) return Optional.empty();
                static_ = true;
                continue;
            }
            if (parsed.typeOf("final")) {
                if (foundVariable) return Optional.empty();
                final_ = true;
                continue;
            }
            if (parsed.typeOf("private")) {
                if (accessModifier != null) {
                    return Optional.empty();
                }
                accessModifier = AccessModifier.PRIVATE;
                continue;
            }
            if (parsed.typeOf("protected")) {
                if (accessModifier != null) {
                    return Optional.empty();
                }
                accessModifier = AccessModifier.PROTECTED;
                continue;
            }
            if (parsed.typeOf("public")) {
                if (accessModifier != null) {
                    return Optional.empty();
                }
                accessModifier = AccessModifier.PUBLIC;
                continue;
            }
            if (parsed.typeOf("package")) {
                if (accessModifier != null) {
                    return Optional.empty();
                }
                accessModifier = AccessModifier.PACKAGE;
                continue;
            }
            if (foundAssignment) {
                value.add(parsed);
            } else if (parsed.typeOf("assignment")) {
                if (!foundVariable) return Optional.empty();
                foundAssignment = true;
            }
        }

        if (accessModifier == null && static_) accessModifier = AccessModifier.PACKAGE;
        if (!foundVariable || accessModifier == null) return Optional.empty();
        return Optional.of(new FieldCheckResult(static_, final_, variable, value, line, accessModifier));
    }
    private Optional<MethodCheckResult> methodCheck(ParseResult statement) {
        if (!statement.typeOf("statement")) return Optional.empty();

        ParseResult header = statement.getChildren().get(0);
        if (header.getChildren().size() != 1) return Optional.empty();
        ParseResult declaration = header.getChildren().get(0);
        ParseResult body = statement.getChildren().get(1);

        boolean constructor = false;
        boolean static_ = false;
        boolean final_ = false;
        AccessModifier accessModifier = null;
        String name = null;

        for (ParseResult declarationPiece : declaration.getChildren()) {
            if (declarationPiece.typeOf("private")) {
                if (accessModifier != null) {
                    return Optional.empty();
                }
                accessModifier = AccessModifier.PRIVATE;
                continue;
            }
            if (declarationPiece.typeOf("protected")) {
                if (accessModifier != null) {
                    return Optional.empty();
                }
                accessModifier = AccessModifier.PROTECTED;
                continue;
            }
            if (declarationPiece.typeOf("public")) {
                if (accessModifier != null) {
                    return Optional.empty();
                }
                accessModifier = AccessModifier.PUBLIC;
                continue;
            }
            if (declarationPiece.typeOf("package")) {
                if (accessModifier != null) {
                    return Optional.empty();
                }
                accessModifier = AccessModifier.PACKAGE;
                continue;
            }
            if (declarationPiece.typeOf("static")) {
                if (static_) return Optional.empty();
                static_ = true;
                continue;
            }
            if (declarationPiece.typeOf("final")) {
                if (final_) return Optional.empty();
                final_ = true;
                continue;
            }
            if (declarationPiece.typeOf("method-name")) {
                name = declarationPiece.getText();
                break;
            }
        }

        boolean isConstructor = name != null && (name.equals("constructor") || name.equals(this.header.getName()));

        if (accessModifier == null && (isConstructor || static_)) accessModifier = AccessModifier.PACKAGE;
        if (name == null || accessModifier == null) return Optional.empty();
        if (name.equals("constructor") || name.equals(this.header.getName())) {
            constructor = true;
        }
        return Optional.of(new MethodCheckResult(constructor, static_, final_, header, body, statement, name, accessModifier, computerParams(declaration.getChildren().last())));
    }
    private ParameterDefinition[] computerParams(ParseResult params) {
        ParameterDefinition[] definitions = new ParameterDefinition[params.getChildren().size()];
        ParseResults children = params.getChildren();
        for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
            ParseResult parsed = children.get(i);
            if (parsed.typeOf("parameter")) {
                definitions[i] = new ParameterDefinition(parsed.getText());
            }
        }
        return definitions;
    }

    private static class FieldCheckResult {

        private final boolean static_;
        private final boolean final_;
        private final ParseResult variable;
        private final ParseResults value;
        private final ParseResult code;
        private final AccessModifier accessModifier;

        private FieldCheckResult(boolean static_, boolean final_, ParseResult variable, ParseResults value, ParseResult code, AccessModifier accessModifier) {
            this.static_ = static_;
            this.final_ = final_;
            this.variable = variable;
            this.value = value;
            this.code = code;
            this.accessModifier = accessModifier;
        }

        public FieldDefinition toDefinition() {
            return new FieldDefinition(accessModifier, variable.getText(), static_, final_, code);
        }

    }

    private static class MethodCheckResult {

        private final boolean constructor;
        private final boolean static_;
        private final boolean final_;
        private final ParseResult header;
        private final ParseResult body;
        private final ParseResult code;
        private final String name;
        private final AccessModifier accessModifier;
        private final ParameterDefinition[] parameterDefinitions;

        private MethodCheckResult(boolean constructor, boolean static_, boolean final_, ParseResult header, ParseResult body, ParseResult code, String name, AccessModifier accessModifier, ParameterDefinition[] parameterDefinitions) {
            this.constructor = constructor;
            this.static_ = static_;
            this.final_ = final_;
            this.header = header;
            this.body = body;
            this.code = code;
            this.name = name;
            this.accessModifier = accessModifier;
            this.parameterDefinitions = parameterDefinitions;
        }

        public MethodDefinition toDefinition() {
            return new MethodDefinition(accessModifier, name, static_, final_, parameterDefinitions, code);
        }
        public ConstructorDefinition toConstructorDefinition() {
            return new ConstructorDefinition(accessModifier, parameterDefinitions, code);
        }

    }

}
