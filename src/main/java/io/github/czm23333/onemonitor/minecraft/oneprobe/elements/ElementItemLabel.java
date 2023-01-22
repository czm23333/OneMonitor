package io.github.czm23333.onemonitor.minecraft.oneprobe.elements;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import guru.nidi.graphviz.model.Node;
import io.github.czm23333.onemonitor.minecraft.utils.GraphUtil;

import java.util.ArrayList;

public class ElementItemLabel extends Element {
    public CompoundTag item;
    public Integer count;

    public ElementItemLabel(CompoundTag item, Integer count) {
        this.item = item;
        this.count = count;
    }

    @Override
    public Node toTree() {
        ArrayList<Node> children = new ArrayList<>();
        if (item != null) GraphUtil.newNode(item + " (Item)");
        if (count != null) GraphUtil.newNode(count + " (Count)");
        return GraphUtil.newNode("ItemLabel (Element)").link(children);
    }
}