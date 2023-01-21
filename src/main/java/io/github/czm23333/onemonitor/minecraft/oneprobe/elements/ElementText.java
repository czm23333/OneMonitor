package io.github.czm23333.onemonitor.minecraft.oneprobe.elements;

import org.barfuin.texttree.api.DefaultNode;

import java.util.List;

public class ElementText extends Element {
    public String text;

    public ElementText(String text) {
        this.text = text;
    }

    @Override
    public DefaultNode toTree() {
        return new DefaultNode("Text", null, null, "element",
                List.of(new DefaultNode(text, null, null, "text", null)));
    }
}