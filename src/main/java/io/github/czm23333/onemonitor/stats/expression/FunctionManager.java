package io.github.czm23333.onemonitor.stats.expression;

import io.github.czm23333.onemonitor.stats.expression.exception.IllegalExpressionException;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class FunctionManager {
    public static final Set<Character> ID_BEGIN_CHARS = Set.of('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
            'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F',
            'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '_');
    private static final HashMap<String, Function<List<Object>, Object>> functions = new HashMap<>();

    public static void register(String id, Function<List<Object>, Object> function) {
        if (id == null || id.length() == 0) throw new IllegalArgumentException("Illegal id");
        if (!ID_BEGIN_CHARS.contains(id.charAt(0))) throw new IllegalArgumentException("Illegal id");
        if (functions.containsKey(id))
            throw new IllegalArgumentException("Function id " + id + " has already been taken");
        functions.put(id, function);
    }

    public static Object call(String id, List<Object> parameter) {
        if (functions.containsKey(id)) {
            try {
                return functions.get(id).apply(parameter);
            } catch (Exception t) {
                throw new IllegalExpressionException("Error calling function " + id + " : ", t);
            }
        } else throw new IllegalExpressionException("Unknown function: " + id);
    }
}