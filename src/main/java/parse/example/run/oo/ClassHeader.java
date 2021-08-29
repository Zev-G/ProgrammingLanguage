package parse.example.run.oo;

public class ClassHeader {

    private final boolean anonymous;
    private final String name;
    private final boolean final_;
    private final ClassDefinition superClass;
    private final boolean abstract_;
    private final AccessModifier accessModifier;

    public static ClassHeader basic(String name, AccessModifier accessModifier) {
        return new ClassHeader(false, name, false, false, ClassDefinition.OBJECT, accessModifier);
    }

    public ClassHeader(boolean anonymous, String name, boolean final_, boolean abstract_, ClassDefinition superClass, AccessModifier accessModifier) {
        this.anonymous = anonymous;
        this.name = name;
        this.final_ = final_;
        this.superClass = superClass;
        this.abstract_ = abstract_;
        this.accessModifier = accessModifier;
    }

    public AccessModifier getAccessModifier() {
        return accessModifier;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public String getName() {
        return name;
    }

    public boolean isFinal() {
        return final_;
    }

    public ClassDefinition getSuperClass() {
        return superClass;
    }

    public boolean isAbstract() {
        return abstract_;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (accessModifier != AccessModifier.PACKAGE) builder.append(accessModifier.toString().toLowerCase()).append(" ");
        if (isAbstract()) builder.append("abstract ");
        if (isFinal()) builder.append("final ");
        builder.append(name).append(" ");
        if (superClass != null && superClass != ClassDefinition.OBJECT) builder.append("extends ").append(superClass.getName());
        return builder.toString();
    }

}
