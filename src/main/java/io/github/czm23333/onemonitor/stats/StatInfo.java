package io.github.czm23333.onemonitor.stats;

import io.github.czm23333.onemonitor.stats.expression.node.Node;

import java.util.Collections;
import java.util.List;

public class StatInfo {
    public List<Source> sources = Collections.singletonList(new Source());
    public List<Display> displays = Collections.singletonList(new Display());
    public long refresh = 60000;
    public long keep = 86400000;

    public static class Source {
        public List<Probe> probes = Collections.singletonList(new Probe("0", "0", "0", "0"));
        public String result = "result expression";
        transient List<ParsedProbe> parsedProbes = null;
        transient Node parsedResult = null;

        public static record Probe(String dim, String x, String y, String z) {
        }

        static record ParsedProbe(Node dim, Node x, Node y, Node z) {
        }
    }

    public static class Display {
        public String name = "display name";
        public List<Line> lines = Collections.singletonList(new Line("line name", 0));

        public static record Line(String name, int sourceIndex) {
        }
    }
}