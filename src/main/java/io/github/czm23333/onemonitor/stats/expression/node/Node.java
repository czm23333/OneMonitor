package io.github.czm23333.onemonitor.stats.expression.node;

import java.util.Map;

public abstract class Node {
    public abstract Object execute(Map<String, Object> env);
}