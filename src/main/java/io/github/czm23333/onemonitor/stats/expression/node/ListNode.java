package io.github.czm23333.onemonitor.stats.expression.node;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ListNode extends Node {
    public List<Node> elements;

    public ListNode(List<Node> elements) {
        this.elements = List.copyOf(elements);
    }

    @Override
    public Object execute(Map<String, Object> env) {
        return elements.stream().map(e -> e.execute(env)).collect(Collectors.toList());
    }
}