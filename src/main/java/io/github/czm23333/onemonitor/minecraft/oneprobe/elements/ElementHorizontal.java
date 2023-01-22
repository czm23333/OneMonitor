package io.github.czm23333.onemonitor.minecraft.oneprobe.elements;

import guru.nidi.graphviz.model.Node;
import io.github.czm23333.onemonitor.minecraft.utils.GraphUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class ElementHorizontal extends Element {
    public ArrayList<Element> children;
    public Integer borderColor;
    public short spacing;
    public short alignment;

    public ElementHorizontal(Collection<Element> children, Integer borderColor, short spacing, short alignment) {
        this.children = new ArrayList<>(children);
        this.borderColor = borderColor;
        this.spacing = spacing;
        this.alignment = alignment;
    }

    @Override
    public Node toTree() {
        ArrayList<Node> nc = new ArrayList<>();
        nc.add(GraphUtil.newNode("(Children)")
                .link(children.stream().map(Element::toTree).collect(Collectors.toList())));
        if (borderColor != null) nc.add(GraphUtil.newNode(borderColor + " (BorderColor)"));
        nc.add(GraphUtil.newNode(spacing + " (Spacing)"));
        nc.add(GraphUtil.newNode(alignment + " (Alignment)"));
        return GraphUtil.newNode("Horizontal (Element)").link(nc);
    }
}