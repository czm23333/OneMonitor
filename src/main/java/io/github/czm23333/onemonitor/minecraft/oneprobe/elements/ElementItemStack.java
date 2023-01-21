package io.github.czm23333.onemonitor.minecraft.oneprobe.elements;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import org.barfuin.texttree.api.DefaultNode;

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
    public DefaultNode toTree() {
        ArrayList<DefaultNode> children = new ArrayList<>();
        if (item != null) children.add(new DefaultNode(item.toString(), null, null, "item", null));
        if (count != null) children.add(new DefaultNode(count.toString(), null, null, "count", null));
        children.add(new DefaultNode(String.valueOf(width), null, null, "width", null));
        children.add(new DefaultNode(String.valueOf(height), null, null, "height", null));
        return new DefaultNode("ItemStack", null, null, "element", children);
    }
}