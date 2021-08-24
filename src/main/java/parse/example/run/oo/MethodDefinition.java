package parse.example.run.oo;

import parse.ParseResult;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class MethodDefinition extends MemberDefinition {

    private final ParameterDefinition[] parameters;

    public MethodDefinition(AccessModifier accessModifier, String name, boolean static_, boolean final_, ParameterDefinition[] parameters, ParseResult code) {
        super(accessModifier, name, static_, final_, code);
        this.parameters = parameters;
    }

    public static MethodDefinition[] fromJavaClass(Class<?> jClass) {
        Method[] methods = jClass.getMethods();
        MethodDefinition[] methodDefinitions = new MethodDefinition[methods.length];
        for (int i = 0, length = methods.length; i < length; i++) {
            methodDefinitions[i] = fromJavaMethod(methods[i]);
        }
        return methodDefinitions;
    }
    public static MethodDefinition fromJavaMethod(Method method) {
        return new MethodDefinition(
                AccessModifier.fromJavaModifiers(method.getModifiers()), method.getName(), Modifier.isStatic(method.getModifiers()), Modifier.isFinal(method.getModifiers()),
                Arrays.stream(method.getParameters()).map(param -> new ParameterDefinition(param.getName(), param.isVarArgs())).toArray(ParameterDefinition[]::new), null
                );
    }

    public ParameterDefinition[] getParameters() {
        return parameters;
    }

}
