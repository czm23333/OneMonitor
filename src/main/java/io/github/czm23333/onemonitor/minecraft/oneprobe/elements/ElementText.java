package io.github.czm23333.onemonitor.minecraft.oneprobe.elements;

import guru.nidi.graphviz.model.Node;
import io.github.czm23333.onemonitor.minecraft.utils.GraphUtil;

public class ElementText extends Element {
    public String text;

    public ElementText(String text) {
        this.text = text;
    }

    @Override
    public Node toTree() {
        return GraphUtil.newNode("Text (Element)").link(GraphUtil.newNode(text + " (Text)"));
    }
}