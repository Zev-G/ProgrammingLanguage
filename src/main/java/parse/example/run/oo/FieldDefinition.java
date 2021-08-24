package parse.example.run.oo;

import parse.ParseResult;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class FieldDefinition extends MemberDefinition {

    public FieldDefinition(AccessModifier accessModifier, String name, boolean static_, boolean final_, ParseResult code) {
        super(accessModifier, name, static_, final_, code);
    }

    public static FieldDefinition[] fromJavaClass(Class<?> jClass) {
        Field[] fields = jClass.getFields();
        FieldDefinition[] fieldDefinitions = new FieldDefinition[fields.length];
        for (int i = 0, fieldsLength = fields.length; i < fieldsLength; i++) {
            fieldDefinitions[i] = fromJavaField(fields[i]);
        }
        return fieldDefinitions;
    }
    public static FieldDefinition fromJavaField(Field field) {
        return new FieldDefinition(AccessModifier.fromJavaModifiers(field.getModifiers()), field.getName(), Modifier.isStatic(field.getModifiers()), Modifier.isFinal(field.getModifiers()), null);
    }

}
