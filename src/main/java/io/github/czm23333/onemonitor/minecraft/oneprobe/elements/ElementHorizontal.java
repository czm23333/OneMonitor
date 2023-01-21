package io.github.czm23333.onemonitor.minecraft.oneprobe.elements;

import org.barfuin.texttree.api.DefaultNode;

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
    public DefaultNode toTree() {
        ArrayList<DefaultNode> nc = new ArrayList<>();
        nc.add(new DefaultNode("children", null, null, null, children.stream().map(Element::toTree).collect(Collectors.toList())));
        if (borderColor != null) nc.add(new DefaultNode(borderColor.toString(), null, null, "borderColor", null));
        nc.add(new DefaultNode(String.valueOf(spacing), null, null, "spacing", null));
        nc.add(new DefaultNode(String.valueOf(alignment), null, null, "alignment", null));
        return new DefaultNode("Horizontal", null, null, "element", nc);
    }
}