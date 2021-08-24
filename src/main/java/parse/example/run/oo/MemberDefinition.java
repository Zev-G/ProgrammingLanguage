package parse.example.run.oo;

import parse.ParseResult;

public abstract class MemberDefinition {

    private final AccessModifier accessModifier;
    private final String name;
    private final boolean static_;
    private final boolean final_;
    private final ParseResult code;

    private ClassDefinitionBase classDefinition;

    public MemberDefinition(AccessModifier accessModifier, String name, boolean static_, boolean final_, ParseResult code) {
        this.accessModifier = accessModifier;
        this.name = name;
        this.static_ = static_;
        this.final_ = final_;
        this.code = code;
    }

    public AccessModifier getAccessModifier() {
        return accessModifier;
    }

    public String getName() {
        return name;
    }

    public boolean isStatic() {
        return static_;
    }

    public boolean isFinal() {
        return final_;
    }

    public ParseResult getCode() {
        return code;
    }

    public boolean isCodeNull() {
        return code == null;
    }

    public ClassDefinitionBase getClassDefinition() {
        return classDefinition;
    }

    void setClassDefinition(ClassDefinitionBase classDefinition) {
        this.classDefinition = classDefinition;
    }

}
