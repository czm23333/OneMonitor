package io.github.czm23333.onemonitor.stats.expression.node;

import io.github.czm23333.onemonitor.stats.expression.exception.IllegalExpressionException;

import java.util.List;
import java.util.Map;

public class SpreadNode extends Node {
    public Node value;

    public SpreadNode(Node value) {
        this.value = value;
    }

    @Override
    public Object execute(Map<String, Object> env) {
        Object temp = value.execute(env);
        if (temp instanceof List) return temp;
        else throw new IllegalExpressionException("Tried to spread non-list value");
    }
}