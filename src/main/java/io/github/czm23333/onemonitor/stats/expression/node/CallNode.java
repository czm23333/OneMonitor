package io.github.czm23333.onemonitor.stats.expression.node;

import io.github.czm23333.onemonitor.stats.expression.FunctionMapping;

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

    @Override
    public Object execute(Map<String, Object> env) {
        return FunctionMapping.call(function,
                parameter.stream().map(node -> node.execute(env)).collect(Collectors.toList()));
    }
}