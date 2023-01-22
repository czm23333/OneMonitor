package io.github.czm23333.onemonitor.stats.expression.node;

public abstract class BiNode extends Node {
    public abstract Node getLeft();

    public abstract void setLeft(Node left);

    public abstract Node getRight();

    public abstract void setRight(Node right);
}