package parse.example.run.oo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

public abstract class VirtualFileBase<T> {

    private final boolean virtual;
    private final T location;
    private final T parent;
    private final String name;

    public static VirtualFileBase<Path> fromPath(Path path, Predicate<VirtualFile<Path>> classChecker) {
        if (Files.isDirectory(path)) {
            return VirtualFolder.fromPath(path, classChecker);
        } else {
            return VirtualFile.fromPath(path, classChecker);
        }
    }

    public VirtualFileBase(boolean virtual, T location, T parent, String name) {
        this.virtual = virtual;
        this.location = location;
        this.parent = parent;
        this.name = name;
    }

    public T getLocation() {
        return location;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public String getName() {
        return name;
    }

    public T getParent() {
        return parent;
    }

}
