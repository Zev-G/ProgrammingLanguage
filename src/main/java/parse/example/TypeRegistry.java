package parse.example;

import parse.ParseType;

import java.util.HashMap;
import java.util.Map;

public class TypeRegistry {

    static Map<String, ParseType> types = new HashMap<>();

    public static ParseType get(String name) {
        return types.computeIfAbsent(name, ParseType::byName);
    }

}
