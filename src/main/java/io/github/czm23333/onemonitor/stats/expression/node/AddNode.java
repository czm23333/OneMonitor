package io.github.czm23333.onemonitor.stats.expression.node;

import io.github.czm23333.onemonitor.stats.expression.exception.IllegalExpressionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AddNode extends BiNode {
    private Node leftOp;
    private Node rightOp;

    public AddNode(Node leftOp, Node rightOp) {
        this.leftOp = leftOp;
        this.rightOp = rightOp;
    }

    @Override
    public Object execute(Map<String, Object> env) {
        Object lop = leftOp.execute(env);
        Object rop = rightOp.execute(env);
        switch (lop) {
            case Double l && rop instanceof Double r:
                return l + r;
            case List<?> l && rop instanceof Double r:
                return l.stream().map(v -> {
                    if (v instanceof Double d) return d + r;
                    else throw new IllegalExpressionException("Tried adding non-double values");
                }).collect(Collectors.toList());
            case Double l && rop instanceof List<?> r:
                return r.stream().map(v -> {
                    if (v instanceof Double d) return l + d;
                    else throw new IllegalExpressionException("Tried adding non-double values");
                }).collect(Collectors.toList());
            case List<?> l && rop instanceof List<?> r:
                if (l.size() != r.size())
                    throw new IllegalExpressionException("Tried adding two lists with different length");
                ArrayList<Double> result = new ArrayList<>();
                for (int i = 0, bound = l.size(); i < bound; ++i) {
                    if (l.get(i) instanceof Double lv && r.get(i) instanceof Double rv) result.add(lv + rv);
                    else throw new IllegalExpressionException("Tried adding non-double values");
                }
                return result;
            default:
                throw new IllegalExpressionException("Tried adding non-double values");
        }
    }

    @Override
    public Node getLeft() {
        return leftOp;
    }

    @Override
    public void setLeft(Node left) {
        leftOp = left;
    }

    @Override
    public Node getRight() {
        return rightOp;
    }

    @Override
    public void setRight(Node right) {
        rightOp = right;
    }
}