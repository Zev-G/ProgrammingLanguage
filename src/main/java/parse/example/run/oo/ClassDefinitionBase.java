package parse.example.run.oo;

public abstract class ClassDefinitionBase {

    private final boolean anonymous;
    private final String name;
    private final ClassDefinition superClass;

    private final boolean abstract_;
    private final boolean final_;

    private final AccessModifier accessModifier;

    private final MemberDefinition[] members;

    public ClassDefinitionBase(boolean anonymous, String name, ClassDefinition superClass, boolean abstract_, boolean final_, AccessModifier accessModifier, MemberDefinition[] members) {
        this.anonymous = anonymous;
        this.name = name;
        this.superClass = superClass;
        this.abstract_ = abstract_;
        this.final_ = final_;
        this.accessModifier = accessModifier;
        this.members = members;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public String getName() {
        return name;
    }

    public ClassDefinition getSuperClass() {
        return superClass;
    }

    public boolean isAbstract() {
        return abstract_;
    }

    public boolean isFinal() {
        return final_;
    }

    public AccessModifier getAccessModifier() {
        return accessModifier;
    }

    public MemberDefinition[] getMembers() {
        return members;
    }

}
