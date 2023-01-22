package io.github.czm23333.onemonitor.minecraft.oneprobe.elements;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import guru.nidi.graphviz.model.Node;
import io.github.czm23333.onemonitor.minecraft.utils.GraphUtil;

import java.util.ArrayList;

public class ElementItemStack extends Element {
    public CompoundTag item;
    public Integer count;
    public int width;
    public int height;

    public ElementItemStack(CompoundTag item, Integer count, int width, int height) {
        this.item = item;
        this.count = count;
        this.width = width;
        this.height = height;
    }

    @Override
    public Node toTree() {
        ArrayList<Node> children = new ArrayList<>();
        if (item != null) children.add(GraphUtil.newNode(item + " (Item)"));
        if (count != null) children.add(GraphUtil.newNode(count + " (Count)"));
        children.add(GraphUtil.newNode(width + " (Width)"));
        children.add(GraphUtil.newNode(height + " (Height)"));
        return GraphUtil.newNode("ItemStack (Element)").link(children);
    }
}