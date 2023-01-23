package io.github.czm23333.onemonitor.minecraft.oneprobe.elements;

import guru.nidi.graphviz.model.Node;
import io.github.czm23333.onemonitor.minecraft.oneprobe.ResourceLocation;
import io.github.czm23333.onemonitor.minecraft.utils.GraphUtil;
import org.barfuin.texttree.api.DefaultNode;

import java.util.List;

public class ElementIcon extends Element {
    public ResourceLocation iconLocation;
    public int u;
    public int v;
    public int w;
    public int h;
    public int width;
    public int height;
    public int textureWidth;
    public int textureHeight;

    public ElementIcon(ResourceLocation iconLocation, int u, int v, int w, int h, int width, int height,
            int textureWidth, int textureHeight) {
        this.iconLocation = iconLocation;
        this.u = u;
        this.v = v;
        this.w = w;
        this.h = h;
        this.width = width;
        this.height = height;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    @Override
    public Node toGraph() {
        return GraphUtil.newNode("Icon (Element)")
                .link(GraphUtil.newNode(iconLocation + "(IconLocation)"), GraphUtil.newNode(u + " (u)"),
                        GraphUtil.newNode(v + " (v)"), GraphUtil.newNode(w + " (w)"), GraphUtil.newNode(h + " (h)"),
                        GraphUtil.newNode(width + " (Width)"), GraphUtil.newNode(height + " (Height)"),
                        GraphUtil.newNode(textureWidth + " (TextureWidth)"),
                        GraphUtil.newNode(textureHeight + " (TextureHeight)"));
    }

    @Override
    public DefaultNode toTree() {
        return new DefaultNode("Icon", null, null, "element",
                List.of(new DefaultNode(iconLocation.toString(), null, null, "iconLocation", null),
                        new DefaultNode(String.valueOf(u), null, null, "u", null),
                        new DefaultNode(String.valueOf(v), null, null, "v", null),
                        new DefaultNode(String.valueOf(w), null, null, "w", null),
                        new DefaultNode(String.valueOf(h), null, null, "h", null),
                        new DefaultNode(String.valueOf(width), null, null, "width", null),
                        new DefaultNode(String.valueOf(height), null, null, "height", null),
                        new DefaultNode(String.valueOf(textureWidth), null, null, "textureWidth", null),
                        new DefaultNode(String.valueOf(textureHeight), null, null, "textureHeight", null)));
    }
}