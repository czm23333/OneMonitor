package io.github.czm23333.onemonitor.minecraft.oneprobe.elements;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import guru.nidi.graphviz.model.Node;
import io.github.czm23333.onemonitor.minecraft.utils.GraphUtil;

import java.util.ArrayList;

public class ElementEntity extends Element {
    public String entityName;
    public int width;
    public int height;
    public float scale;
    public CompoundTag entityNbt;
    public Integer playerId;

    public ElementEntity(String entityName, int width, int height, float scale, CompoundTag entityNbt,
            Integer playerId) {
        this.entityName = entityName;
        this.width = width;
        this.height = height;
        this.scale = scale;
        this.entityNbt = entityNbt;
        this.playerId = playerId;
    }

    @Override
    public Node toTree() {
        ArrayList<Node> children = new ArrayList<>();
        children.add(GraphUtil.newNode(entityName + " (EntityName)"));
        children.add(GraphUtil.newNode(width + " (Width)"));
        children.add(GraphUtil.newNode(height + " (Height)"));
        children.add(GraphUtil.newNode(scale + " (Scale)"));
        if (entityNbt != null) children.add(GraphUtil.newNode(entityNbt + " (EntityNbt)"));
        if (playerId != null) children.add(GraphUtil.newNode(playerId + " (PlayerId)"));
        return GraphUtil.newNode("Entity (Element)").link(children);
    }
}