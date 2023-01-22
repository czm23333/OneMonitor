package io.github.czm23333.onemonitor.stats.expression.node;

import java.util.Map;

public class StringNode extends Node {
    public String str;

    public StringNode(String str) {
        this.str = str;
    }

    @Override
    public Object execute(Map<String, Object> env) {
        return str;
    }
}