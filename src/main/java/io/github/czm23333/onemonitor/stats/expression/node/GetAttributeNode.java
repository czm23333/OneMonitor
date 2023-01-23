package io.github.czm23333.onemonitor.stats.expression.node;

import com.github.steveice10.opennbt.tag.builtin.*;
import com.github.steveice10.opennbt.tag.builtin.custom.DoubleArrayTag;
import com.github.steveice10.opennbt.tag.builtin.custom.FloatArrayTag;
import com.github.steveice10.opennbt.tag.builtin.custom.ShortArrayTag;
import io.github.czm23333.onemonitor.stats.expression.exception.IllegalExpressionException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GetAttributeNode extends Node {
    public Node op;
    public Node attribute;

    public GetAttributeNode(Node attribute) {
        this.op = null;
        this.attribute = attribute;
    }

    public GetAttributeNode(Node op, Node attribute) {
        this.op = op;
        this.attribute = attribute;
    }

    private static Object convertTag(Tag value) {
        switch (value) {
            case CompoundTag compoundTag -> {
                return compoundTag;
            }
            case ByteArrayTag byteArrayTag -> {
                List<Double> result = new ArrayList<>();
                byte[] temp = byteArrayTag.getValue();
                for (byte b : temp) result.add((double) b);
                return result;
            }
            case ByteTag byteTag -> {
                return (double) byteTag.getValue();
            }
            case DoubleTag doubleTag -> {
                return doubleTag.getValue();
            }
            case FloatTag floatTag -> {
                return (double) floatTag.getValue();
            }
            case IntArrayTag intArrayTag -> {
                return Arrays.stream(intArrayTag.getValue()).asDoubleStream().boxed().collect(Collectors.toList());
            }
            case IntTag intTag -> {
                return (double) intTag.getValue();
            }
            case ListTag listTag -> {
                return listTag.getValue().stream().map(GetAttributeNode::convertTag).collect(Collectors.toList());
            }
            case LongArrayTag longArrayTag -> {
                return Arrays.stream(longArrayTag.getValue()).asDoubleStream().boxed().collect(Collectors.toList());
            }
            case LongTag longTag -> {
                return (double) longTag.getValue();
            }
            case ShortTag shortTag -> {
                return (double) shortTag.getValue();
            }
            case StringTag stringTag -> {
                return stringTag.getValue();
            }
            case DoubleArrayTag doubleArrayTag -> {
                return Arrays.stream(doubleArrayTag.getValue()).boxed().collect(Collectors.toList());
            }
            case FloatArrayTag floatArrayTag -> {
                List<Double> result = new ArrayList<>();
                float[] temp = floatArrayTag.getValue();
                for (float f : temp) result.add((double) f);
                return result;
            }
            case ShortArrayTag shortArrayTag -> {
                List<Double> result = new ArrayList<>();
                short[] temp = shortArrayTag.getValue();
                for (short s : temp) result.add((double) s);
                return result;
            }
            default -> throw new IllegalExpressionException("Unsupported tag type");
        }
    }

    private static Object convertDataType(Object value) {
        switch (value) {
            case Number num:
                return num.doubleValue();
            case byte[] byteArray: {
                List<Double> result = new ArrayList<>();
                for (byte b : byteArray) result.add((double) b);
                return result;
            }
            case int[] intArray:
                return Arrays.stream(intArray).asDoubleStream().boxed().collect(Collectors.toList());
            case long[] longArray:
                return Arrays.stream(longArray).asDoubleStream().boxed().collect(Collectors.toList());
            case double[] doubleArray:
                return Arrays.stream(doubleArray).boxed().collect(Collectors.toList());
            case float[] floatArray: {
                List<Double> result = new ArrayList<>();
                for (float f : floatArray) result.add((double) f);
                return result;
            }
            case short[] shortArray: {
                List<Double> result = new ArrayList<>();
                for (short s : shortArray) result.add((double) s);
                return result;
            }
            case List<?> list:
                return list.stream().map(v -> v instanceof Number ? ((Number) v).doubleValue() : v)
                        .collect(Collectors.toList());
            case null:
                throw new IllegalExpressionException("Null value is not allowed");
            default:
                return value;
        }
    }

    private static Object getAttr(Object opValue, Object attr) {
        if (attr instanceof Double id) if (opValue instanceof List<?> opList) {
            int index = id.intValue();
            if (opList.size() <= index) throw new IllegalExpressionException("Array index out of bound");
            return convertDataType(opList.get(index));
        } else throw new IllegalExpressionException("Tried getting by index on a non-list value");
        else if (attr instanceof String name) if (opValue instanceof Map<?, ?> opMap) {
            if (opMap.containsKey(name)) return convertDataType(opMap.get(name));
            else throw new IllegalExpressionException("Illegal attribute name: " + name);
        } else if (opValue instanceof CompoundTag opTag) if (opTag.contains(name)) return convertTag(opTag.get(name));
        else throw new IllegalExpressionException("Illegal attribute name: " + name);
        else try {
                return convertDataType(opValue.getClass().getDeclaredField(name).get(opValue));
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new IllegalExpressionException("Illegal attribute name: " + name, e);
            }
        else throw new IllegalExpressionException("Illegal attribute");
    }

    @Override
    public Object execute(Map<String, Object> env) {
        Object attr = attribute.execute(env);
        if (op instanceof SpreadNode)
            return ((List<?>) op.execute(env)).stream().map(v -> getAttr(v, attr)).collect(Collectors.toList());
        else {
            Object opValue = op == null ? env : op.execute(env);
            return getAttr(opValue, attr);
        }
    }
}