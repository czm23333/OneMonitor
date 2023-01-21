package io.github.czm23333.onemonitor.minecraft.oneprobe.elements;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import org.barfuin.texttree.api.DefaultNode;

import java.util.ArrayList;

public class ElementItemLabel extends Element {
    public CompoundTag item;
    public Integer count;

    public ElementItemLabel(CompoundTag item, Integer count) {
        this.item = item;
        this.count = count;
    }

    @Override
    public DefaultNode toTree() {
        ArrayList<DefaultNode> children = new ArrayList<>();
        if (item != null) children.add(new DefaultNode(item.toString(), null, null, "item", null));
        if (count != null) children.add(new DefaultNode(count.toString(), null, null, "count", null));
        return new DefaultNode("ItemLabel", null, null, "element", children);
    }
}