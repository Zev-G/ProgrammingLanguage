package parse.example.run;

import java.util.Arrays;
import java.util.Optional;

public class Import {

    private final String[] packagePath;
    private final String suffix;
    private final ImportType type;

    private final Object value;

    public Import(String[] packagePath, String suffix) {
        this.packagePath = packagePath;
        this.suffix = suffix;
        if (suffix.trim().equals("*")) {
            this.type = ImportType.PACKAGE_CONTENTS;
            this.value = String.join(".", packagePath);
        } else {
            this.type = ImportType.CLASS;
            try {
                this.value = Class.forName(String.join(".", packagePath) + "." + suffix);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException();
            }
        }
    }

    public static Import fromString(String s) {
        String[] parts = s.trim().split("\\.");
        if (parts.length == 0) {
            throw new IllegalArgumentException();
        }
        if (parts.length == 1) {
            return new Import(new String[0], parts[0]);
        }
        String[] packagePath = Arrays.copyOfRange(parts, 0, parts.length - 1);
        String name = parts[parts.length - 1];
        return new Import(packagePath, name);
    }

    public Optional<Class<?>> findClass(String name) {
        if (this.type == ImportType.CLASS) {
            if (suffix.trim().equals(name.trim())) {
                return Optional.of(getAsClass());
            }
        } else { // this.type == ImportType.PACKAGE_CONTENTS
            try {
                return Optional.of(Class.forName(getAsPackage() + "." + name));
            } catch (ClassNotFoundException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private Class<?> getAsClass() {
        if (this.type != ImportType.CLASS) {
            throw new IllegalStateException();
        }
        return (Class<?>) value;
    }
    private String getAsPackage() {
        if (this.type != ImportType.PACKAGE_CONTENTS) {
            throw new IllegalStateException();
        }
        return (String) value;
    }

    public String[] getPackagePath() {
        return packagePath;
    }

    public String getSuffix() {
        return suffix;
    }

    public ImportType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Import{" +
                "packagePath=" + Arrays.toString(packagePath) +
                ", suffix='" + suffix + '\'' +
                ", type=" + type +
                '}';
    }

    public enum ImportType {
        CLASS, PACKAGE_CONTENTS;
    }

}
