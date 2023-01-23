package io.github.czm23333.onemonitor.stats.expression.node;

import io.github.czm23333.onemonitor.stats.expression.FunctionMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CallNode extends Node {
    public String function;
    public List<Node> parameter;

    public CallNode(String function, List<Node> parameter) {
        this.function = function;
        this.parameter = List.copyOf(parameter);
    }

    private static Object call(String function, List<Node> parameter, Map<String, Object> env) {
        for (int i = 0, parameterSize = parameter.size(); i < parameterSize; i++) {
            Node node = parameter.get(i);
            if (node instanceof SpreadNode) {
                ArrayList<Node> newParameter = new ArrayList<>(parameter);
                List<?> spread = (List<?>) node.execute(env);
                ArrayList<Object> result = new ArrayList<>();
                for (Object v : spread) {
                    newParameter.set(i, new TempNode(v));
                    result.add(call(function, newParameter, env));
                }
                return result;
            }
        }
        return FunctionMapping.call(function,
                parameter.stream().map(node -> node.execute(env)).collect(Collectors.toList()));
    }

    @Override
    public Object execute(Map<String, Object> env) {
        return call(function, parameter, env);
    }

    private static class TempNode extends Node {
        private final Object value;

        public TempNode(Object value) {
            this.value = value;
        }

        @Override
        public Object execute(Map<String, Object> env) {
            return value;
        }
    }
}