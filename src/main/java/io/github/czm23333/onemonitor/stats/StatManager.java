package io.github.czm23333.onemonitor.stats;

import io.github.czm23333.onemonitor.minecraft.oneprobe.ProbeRequest;
import io.github.czm23333.onemonitor.minecraft.oneprobe.ProbeResponse;
import io.github.czm23333.onemonitor.stats.expression.ExpressionParser;
import io.github.czm23333.onemonitor.stats.expression.exception.IllegalExpressionException;
import it.unimi.dsi.fastutil.Pair;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class StatManager {
    private static final Logger LOGGER = Logger.getLogger("StatManager");
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Pair<Double, Long>>[]> displayData = new ConcurrentHashMap<>();
    private final Thread refreshThread;
    private final HashMap<String, StatInfo.Display> displays = new HashMap<>();
    private final StatInfo info;
    private volatile boolean flag = true;

    public StatManager(StatInfo info, BlockingQueue<ProbeRequest> channel) {
        this.info = info;
        for (StatInfo.Source source : info.sources) {
            source.parsedResult = ExpressionParser.parse(source.result);
            source.parsedProbes = source.probes.stream()
                    .map(probe -> new StatInfo.Source.ParsedProbe(ExpressionParser.parse(probe.dim()),
                            ExpressionParser.parse(probe.x()), ExpressionParser.parse(probe.y()),
                            ExpressionParser.parse(probe.z()))).collect(Collectors.toList());
        }
        for (StatInfo.Display display : info.displays) {
            displays.put(display.name, display);

            ConcurrentLinkedQueue<Pair<Double, Long>>[] arr = new ConcurrentLinkedQueue[display.lines.size()];
            for (int i = 0; i < arr.length; ++i) arr[i] = new ConcurrentLinkedQueue<>();
            displayData.put(display.name, arr);
        }
        this.refreshThread = new Thread(() -> {
            byte[] lock = new byte[0];
            while (flag) {
                try {
                    Thread.sleep(info.refresh);
                    long current = System.currentTimeMillis();
                    ArrayList<Object> sources = new ArrayList<>();
                    {
                        HashMap<String, Object> env = new HashMap<>();
                        ArrayList<ProbeResponse> responses = new ArrayList<>();
                        env.put("probes", responses);
                        env.put("sources", sources);
                        env.put("time", current);
                        for (StatInfo.Source source : info.sources) {
                            responses.clear();
                            for (StatInfo.Source.ParsedProbe parsedProbe : source.parsedProbes) {
                                List<Integer> dimList = toIntList(parsedProbe.dim().execute(env));
                                List<Integer> xList = toIntList(parsedProbe.x().execute(env));
                                List<Integer> yList = toIntList(parsedProbe.y().execute(env));
                                List<Integer> zList = toIntList(parsedProbe.z().execute(env));
                                for (int dim : dimList)
                                    for (int x : xList)
                                        for (int y : yList)
                                            for (int z : zList) {
                                                channel.add(new ProbeRequest(dim, x, y, z, probeResponse -> {
                                                    responses.add(probeResponse);
                                                    synchronized (lock) {
                                                        lock.notify();
                                                    }
                                                }));
                                                synchronized (lock) {
                                                    lock.wait();
                                                }
                                            }
                            }
                            sources.add(source.parsedResult.execute(env));
                        }
                    }
                    for (StatInfo.Display display : info.displays) {
                        ConcurrentLinkedQueue<Pair<Double, Long>>[] arr = displayData.get(display.name);
                        for (int i = 0; i < display.lines.size(); ++i) {
                            StatInfo.Display.Line line = display.lines.get(i);
                            Object data = sources.get(line.sourceIndex());
                            if (data instanceof Double d) arr[i].add(Pair.of(d, current));
                            else throw new IllegalExpressionException("Illegal result type");
                            while (!arr[i].isEmpty() &&
                                   current - Objects.requireNonNull(arr[i].peek()).right() > info.keep) arr[i].remove();
                        }
                    }
                } catch (IllegalExpressionException e) {
                    LOGGER.log(Level.WARNING, "Error evaluating expression: ", e);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error refreshing: ", e);
                }
            }
        });
        this.refreshThread.start();
    }

    private static List<Integer> toIntList(Object obj) {
        if (obj instanceof Double d) return Collections.singletonList(d.intValue());
        else if (obj instanceof List<?> l) return l.stream().map(v -> {
            if (v instanceof Double d) return d.intValue();
            else throw new IllegalExpressionException("Illegal result type");
        }).collect(Collectors.toList());
        else throw new IllegalExpressionException("Illegal result type");
    }

    public BufferedImage getChart(String displayName) {
        if (displays.containsKey(displayName)) {
            StatInfo.Display display = displays.get(displayName);
            ConcurrentLinkedQueue<Pair<Double, Long>>[] data = displayData.get(displayName);
            TimeSeriesCollection dataset = new TimeSeriesCollection();
            for (int i = 0, linesSize = display.lines.size(); i < linesSize; i++) {
                StatInfo.Display.Line line = display.lines.get(i);
                TimeSeries lineSeries = new TimeSeries(line.name());
                for (var entry : data[i]) lineSeries.add(new FixedMillisecond(entry.right()), entry.left());
                dataset.addSeries(lineSeries);
            }
            JFreeChart chart = ChartFactory.createTimeSeriesChart(displayName, "Time", "Value", dataset);
            return chart.createBufferedImage(1600, 900);
        } else return null;
    }

    public void shutdown() {
        flag = false;
        refreshThread.interrupt();
    }
}