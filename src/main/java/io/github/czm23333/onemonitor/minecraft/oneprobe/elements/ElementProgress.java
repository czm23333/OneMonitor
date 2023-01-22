package io.github.czm23333.onemonitor.minecraft.oneprobe.elements;

import guru.nidi.graphviz.model.Node;
import io.github.czm23333.onemonitor.minecraft.utils.GraphUtil;

public class ElementProgress extends Element {
    public long current;
    public long max;
    public int width;
    public int height;
    public String prefix;
    public String suffix;
    public int borderColor;
    public int filledColor;
    public int alternateFilledColor;
    public int backgroundColor;
    public boolean showText;
    public byte numberFormat;
    public boolean lifeBar;
    public boolean armorBar;

    public ElementProgress(long current, long max, int width, int height, String prefix, String suffix, int borderColor,
            int filledColor, int alternateFilledColor, int backgroundColor, boolean showText, byte numberFormat,
            boolean lifeBar, boolean armorBar) {
        this.current = current;
        this.max = max;
        this.width = width;
        this.height = height;
        this.prefix = prefix;
        this.suffix = suffix;
        this.borderColor = borderColor;
        this.filledColor = filledColor;
        this.alternateFilledColor = alternateFilledColor;
        this.backgroundColor = backgroundColor;
        this.showText = showText;
        this.numberFormat = numberFormat;
        this.lifeBar = lifeBar;
        this.armorBar = armorBar;
    }

    @Override
    public Node toTree() {
        return GraphUtil.newNode("Progress (Element)")
                .link(GraphUtil.newNode(current + " (Current)"), GraphUtil.newNode(max + " (Max)"),
                        GraphUtil.newNode(width + " (Width)"), GraphUtil.newNode(height + " (Height)"),
                        GraphUtil.newNode(prefix + " (Prefix)"), GraphUtil.newNode(suffix + " (Suffix)"),
                        GraphUtil.newNode(borderColor + " (BorderColor)"),
                        GraphUtil.newNode(filledColor + " (FilledColor)"),
                        GraphUtil.newNode(alternateFilledColor + " (AlternateFilledColor)"),
                        GraphUtil.newNode(backgroundColor + " (BackgroundColor)"),
                        GraphUtil.newNode(showText + " (ShowText)"),
                        GraphUtil.newNode(numberFormat + " (NumberFormat)"), GraphUtil.newNode(lifeBar + " (LifeBar)"),
                        GraphUtil.newNode(armorBar + " (ArmorBar)"));
    }
}