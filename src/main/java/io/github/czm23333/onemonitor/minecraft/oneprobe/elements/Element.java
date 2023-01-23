package io.github.czm23333.onemonitor.minecraft.oneprobe.elements;

import guru.nidi.graphviz.model.Node;
import org.barfuin.texttree.api.DefaultNode;

public abstract class Element {
    public abstract Node toGraph();

    public abstract DefaultNode toTree();
}