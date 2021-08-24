package parse.example.run.oo;

import parse.example.reflect.ReflectionUtils;

import java.lang.reflect.Modifier;
import java.util.Arrays;

public class ClassDefinition extends ClassDefinitionBase {

    public static final ClassDefinition OBJECT = ClassDefinition.fromJavaClass(Object.class);

    private static ClassDefinition fromJavaClass(Class<?> jClass) {
        if (jClass == null) return null;
        return new ClassDefinition(
                jClass.isAnonymousClass(), jClass.getSimpleName(), fromJavaClass(jClass.getSuperclass()),
                Modifier.isAbstract(jClass.getModifiers()), Modifier.isFinal(jClass.getModifiers()), AccessModifier.fromJavaModifiers(jClass.getModifiers()),
                FieldDefinition.fromJavaClass(jClass), MethodDefinition.fromJavaClass(jClass), ConstructorDefinition.fromJavaClass(jClass)
                );
    }

    private final FieldDefinition[] fields;
    private final MethodDefinition[] methods;
    private final ConstructorDefinition[] constructors;

    public ClassDefinition(boolean anonymous, String name, ClassDefinition superClass, boolean abstract_, boolean final_, AccessModifier accessModifier, FieldDefinition[] fields, MethodDefinition[] methods, ConstructorDefinition[] constructors) {
        super(anonymous, name, superClass, abstract_, final_, accessModifier, ReflectionUtils.joinArrays(MemberDefinition.class, fields, methods, constructors));
        this.fields = fields;
        this.methods = methods;
        this.constructors = constructors;
    }

    public FieldDefinition[] getFields() {
        return fields;
    }

    public MethodDefinition[] getMethods() {
        return methods;
    }

    public ConstructorDefinition[] getConstructors() {
        return constructors;
    }

    public boolean containsConstructor(ConstructorDefinition constructor) {
        for (ConstructorDefinition loopConstructor : constructors) {
            if (loopConstructor == constructor) return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "ClassDefinition{" +
                "fields=" + Arrays.toString(fields) +
                ", methods=" + Arrays.toString(methods) +
                ", constructors=" + Arrays.toString(constructors) +
                '}';
    }

}
