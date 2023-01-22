package io.github.czm23333.onemonitor.stats.expression.node;

import io.github.czm23333.onemonitor.stats.expression.exception.IllegalExpressionException;

import java.util.Map;

public class AddNode extends BiNode {
    private Node leftOp;
    private Node rightOp;

    public AddNode(Node leftOp, Node rightOp) {
        this.leftOp = leftOp;
        this.rightOp = rightOp;
    }

    @Override
    public Object execute(Map<String, Object> env) {
        if (leftOp.execute(env) instanceof Double l && rightOp.execute(env) instanceof Double r) return l + r;
        else throw new IllegalExpressionException("Tried adding non-double values");
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