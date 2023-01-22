package io.github.czm23333.onemonitor.minecraft.utils;

import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.Node;

public class GraphUtil {
    private static long id = 0;

    public static void reset() {
        id = 0;
    }

    public static Node newNode(String label) {
        return Factory.node(String.valueOf(id++)).with("label", label);
    }
}