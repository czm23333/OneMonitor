package io.github.czm23333.onemonitor.minecraft.oneprobe.elements;

import guru.nidi.graphviz.model.Node;
import io.github.czm23333.onemonitor.minecraft.utils.GraphUtil;
import org.barfuin.texttree.api.DefaultNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class ElementVertical extends Element {
    public ArrayList<Element> children;
    public Integer borderColor;
    public short spacing;
    public short alignment;

    public ElementVertical(Collection<Element> children, Integer borderColor, short spacing, short alignment) {
        this.children = new ArrayList<>(children);
        this.borderColor = borderColor;
        this.spacing = spacing;
        this.alignment = alignment;
    }

    @Override
    public Node toGraph() {
        ArrayList<Node> nc = new ArrayList<>();
        nc.add(GraphUtil.newNode("(Children)")
                .link(children.stream().map(Element::toGraph).collect(Collectors.toList())));
        if (borderColor != null) nc.add(GraphUtil.newNode(borderColor + " (BorderColor)"));
        nc.add(GraphUtil.newNode(spacing + " (Spacing)"));
        nc.add(GraphUtil.newNode(alignment + " (Alignment)"));
        return GraphUtil.newNode("Vertical (Element)").link(nc);
    }

    @Override
    public DefaultNode toTree() {
        ArrayList<DefaultNode> nc = new ArrayList<>();
        nc.add(new DefaultNode("children", null, null, null,
                children.stream().map(Element::toTree).collect(Collectors.toList())));
        if (borderColor != null) nc.add(new DefaultNode(borderColor.toString(), null, null, "borderColor", null));
        nc.add(new DefaultNode(String.valueOf(spacing), null, null, "spacing", null));
        nc.add(new DefaultNode(String.valueOf(alignment), null, null, "alignment", null));
        return new DefaultNode("Vertical", null, null, "element", nc);
    }
}