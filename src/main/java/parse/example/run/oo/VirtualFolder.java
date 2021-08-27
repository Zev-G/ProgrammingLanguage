package parse.example.run.oo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class VirtualFolder<T> extends VirtualFileBase<T> {

    private final VirtualFileBase<T>[] children;

    private final List<VirtualFile<T>> files = new ArrayList<>();
    private final List<VirtualFolder<T>> folders = new ArrayList<>();

    public static VirtualFolder<Path> fromPath(Path path) {
        return fromPath(path, obj -> true);
    }
    @SuppressWarnings("unchecked")
    public static VirtualFolder<Path> fromPath(Path path, Predicate<VirtualFile<Path>> classChecker) {
        if (!Files.isDirectory(path)) throw new IllegalArgumentException();
        try {
            return new VirtualFolder<>(
                    false, path, path.getFileName().toString(),
                    Files.list(path).map(loopPath -> VirtualFileBase.fromPath(loopPath, classChecker)).toArray(VirtualFileBase[]::new),
                    path.getParent()
            );
        } catch (IOException e) {
            throw new IllegalArgumentException();
        }
    }

    public VirtualFolder(boolean virtual, T location, String name, VirtualFileBase<T>[] children, T parent) {
        super(virtual, location, parent, name);
        this.children = children;

        for (VirtualFileBase<T> child : children) {
            if (child instanceof VirtualFile) {
                this.files.add((VirtualFile<T>) child);
            } else if (child instanceof VirtualFolder) {
                this.folders.add((VirtualFolder<T>) child);
            }
        }
    }

    public VirtualFileBase<T>[] getChildren() {
        return children;
    }

    public List<VirtualFile<T>> getFiles() {
        return files;
    }

    public List<VirtualFolder<T>> getFolders() {
        return folders;
    }

    public VirtualFile<T> getFile(String s) {
        for (VirtualFile<T> file : files) {
            if (file.getName().equals(s)) return file;
        }
        return null;
    }

}
