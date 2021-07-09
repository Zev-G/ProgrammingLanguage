package parse.example;

import parse.ParseType;

import java.util.HashMap;
import java.util.Map;

class TypeRegistry {

    static Map<String, ParseType> types = new HashMap<>();

    static ParseType get(String name) {
        return types.computeIfAbsent(name, ParseType::byName);
    }

}
