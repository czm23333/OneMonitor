package io.github.czm23333.onemonitor.minecraft.oneprobe;

import io.github.czm23333.onemonitor.minecraft.oneprobe.elements.Element;

import java.util.ArrayList;

public class ProbeResponse {
    public boolean hasElements = false;
    public ArrayList<Element> elements = new ArrayList<>();
    public boolean timedOut = false;
}