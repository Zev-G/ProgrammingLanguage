package parse.example.run.oo;

import parse.ParseResult;

import java.lang.reflect.Constructor;
import java.util.Arrays;

public class ConstructorDefinition extends MethodDefinition {

    private ConstructorDefinition redirect;

    public ConstructorDefinition(AccessModifier accessModifier, ParameterDefinition[] parameters, ParseResult code) {
        super(accessModifier, "constructor", true, true, parameters, code);
    }

    public ConstructorDefinition getRedirect() {
        return redirect;
    }

    public void setRedirect(ConstructorDefinition redirect) {
        this.redirect = redirect;
    }

    public static ConstructorDefinition[] fromJavaClass(Class<?> jClass) {
        Constructor<?>[] constructors = jClass.getConstructors();
        ConstructorDefinition[] constructorDefinitions = new ConstructorDefinition[constructors.length];
        for (int i = 0, length = constructors.length; i < length; i++) {
            constructorDefinitions[i] = fromJavaConstructor(constructors[i]);
        }
        return constructorDefinitions;
    }
    public static ConstructorDefinition fromJavaConstructor(Constructor<?> constructor) {
        return new ConstructorDefinition(AccessModifier.fromJavaModifiers(constructor.getModifiers()), Arrays.stream(constructor.getParameters()).map(param -> new ParameterDefinition(param.getName(), param.isVarArgs())).toArray(ParameterDefinition[]::new), null);
    }

}
