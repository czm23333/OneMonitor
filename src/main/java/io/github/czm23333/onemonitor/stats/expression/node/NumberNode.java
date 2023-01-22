package io.github.czm23333.onemonitor.stats.expression.node;

import java.util.Map;

public class NumberNode extends Node {
    public double num;

    public NumberNode(double num) {
        this.num = num;
    }

    @Override
    public Object execute(Map<String, Object> env) {
        return num;
    }
}