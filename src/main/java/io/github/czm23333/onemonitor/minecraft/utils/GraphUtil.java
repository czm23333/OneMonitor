package io.github.czm23333.onemonitor.minecraft.utils;

import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.Node;

import java.util.concurrent.atomic.AtomicLong;

public class GraphUtil {
    private static final AtomicLong id = new AtomicLong(0);

    public static void reset() {
        id.set(0);
    }

    public static Node newNode(String label) {
        return Factory.node(String.valueOf(id.getAndIncrement())).with("label", label);
    }
}