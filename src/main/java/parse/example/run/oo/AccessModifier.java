package parse.example.run.oo;

import java.lang.reflect.Modifier;

public enum AccessModifier {
    PRIVATE, PROTECTED, PACKAGE, PUBLIC;

    public static AccessModifier fromJavaModifiers(int modifiers) {
        if (Modifier.isPrivate(modifiers)) return PRIVATE;
        if (Modifier.isProtected(modifiers)) return PROTECTED;
        if (Modifier.isPublic(modifiers)) return PUBLIC;
        return PACKAGE;
    }
}
