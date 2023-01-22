package io.github.czm23333.onemonitor.stats.expression.node;

import java.util.Map;

public class SealedNode extends Node {
    public Node node;

    public SealedNode(Node node) {
        this.node = node;
    }

    @Override
    public Object execute(Map<String, Object> env) {
        return node.execute(env);
    }
}