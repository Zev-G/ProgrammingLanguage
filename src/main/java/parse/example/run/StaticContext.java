package parse.example.run;

import parse.ParsePosition;
import parse.example.ClassParser;
import parse.example.MultiLineParser;
import parse.example.SimpleLang;
import parse.example.run.oo.AccessModifier;
import parse.example.run.oo.ClassHeader;
import parse.example.run.oo.VirtualFile;
import parse.example.run.oo.VirtualFolder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class StaticContext {

    private final MultiLineParser parser = new MultiLineParser();

    private final VirtualFolder<?> location;

    private final Map<VirtualFile<?>, ClassRunner> classes = new HashMap<>();

    public StaticContext(VirtualFolder<?> location) {
        this.location = location;
    }

    public ClassRunner registerClass(ClassRunner classDefinition) {
        classes.put(classDefinition.getLoc(), classDefinition);
        return classDefinition;
    }

    public ClassRunner registerClass(VirtualFile<?> unParsed) {
        String text = unParsed.getText();
        ClassHeader header = ClassHeader.basic(unParsed.getPlainName(), AccessModifier.PUBLIC);
        ClassRunner runner = new ClassRunner(this, parser.parse(text, new ParsePosition(text, 0)).orElseThrow(), header, unParsed);
        return registerClass(runner);
    }

    public VirtualFolder<?> getLocation() {
        return location;
    }

    public Optional<ClassRunner> findClass(String name) {
        if (location == null || name.isEmpty()) return Optional.empty();
        VirtualFile<?> file = location.getFile(name + "." + SimpleLang.ENDING);
        if (file == null) return Optional.empty();
        if (classes.containsKey(file)) return Optional.of(classes.get(file));
        return Optional.of(registerClass(file));
    }

}
