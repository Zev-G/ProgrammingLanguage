package parse.example.run.oo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

public abstract class VirtualFile<T> extends VirtualFileBase<T> {

    private boolean class_;
    private final String ending;
    private final String plainName;

    public static VirtualFile<Path> fromPath(Path path, Predicate<VirtualFile<Path>> classChecker) {
        VirtualFile<Path> virtualFile = new VirtualFile<>(false, path, path.getFileName().toString(), path.getParent()) {

            @Override
            public String getText() {
                try {
                    return Files.readString(path);
                } catch (IOException e) {
                    return null;
                }
            }

        };
        virtualFile.setClass(classChecker.test(virtualFile));
        return virtualFile;
    }

    public VirtualFile(boolean virtual, T location, String name, T parent) {
        super(virtual, location, parent, name);
        ending = deriveEnding(name);
        plainName = derivePlainName(name);
    }

    public VirtualFile(boolean virtual, T location, String name, boolean class_, T parent) {
        super(virtual, location, parent, name);
        this.class_ = class_;
        ending = deriveEnding(name);
        plainName = derivePlainName(name);
    }

    private static String deriveEnding(String name) {
        if (name.contains(".")) {
            return name.substring(name.lastIndexOf('.'));
        } else {
            return  "";
        }
    }
    private static String derivePlainName(String name) {
        if (name.contains(".")) {
            return name.substring(0, name.indexOf('.'));
        } else {
            return name;
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    public boolean isClass() {
        return class_;
    }

    public void setClass(boolean class_) {
        this.class_ = class_;
    }

    public String getEnding() {
        return ending;
    }

    public String getPlainName() {
        return plainName;
    }

    public abstract String getText();

}
