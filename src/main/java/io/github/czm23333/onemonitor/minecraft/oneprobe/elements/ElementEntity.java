package io.github.czm23333.onemonitor.minecraft.oneprobe.elements;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import org.barfuin.texttree.api.DefaultNode;

import java.util.ArrayList;

public class ElementEntity extends Element {
    public String entityName;
    public int width;
    public int height;
    public float scale;
    public CompoundTag entityNbt;
    public Integer playerId;

    public ElementEntity(String entityName, int width, int height, float scale, CompoundTag entityNbt, Integer playerId) {
        this.entityName = entityName;
        this.width = width;
        this.height =height;
        this.scale = scale;
        this.entityNbt = entityNbt;
        this.playerId = playerId;
    }

    @Override
    public DefaultNode toTree() {
        ArrayList<DefaultNode> children = new ArrayList<>();
        children.add(new DefaultNode(entityName, null, null, "entityName", null));
        children.add(new DefaultNode(String.valueOf(width), null, null, "width", null));
        children.add(new DefaultNode(String.valueOf(height), null, null, "height", null));
        children.add(new DefaultNode(String.valueOf(scale), null, null, "scale", null));
        if (entityNbt != null) children.add(new DefaultNode(entityNbt.toString(), null, null, "entityNbt", null));
        if (playerId != null) children.add(new DefaultNode(playerId.toString(), null, null, "playerId", null));
        return new DefaultNode("Entity", null, null, "element", children);
    }
}