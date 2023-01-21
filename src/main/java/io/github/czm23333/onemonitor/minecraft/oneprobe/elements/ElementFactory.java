package io.github.czm23333.onemonitor.minecraft.oneprobe.elements;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.packetlib.codec.PacketCodecHelper;
import io.github.czm23333.onemonitor.minecraft.oneprobe.ResourceLocation;
import io.github.czm23333.onemonitor.minecraft.utils.NetworkUtil;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class ElementFactory {
    private static final Map<Integer, BiFunction<ByteBuf, PacketCodecHelper, Element>> parsers = new HashMap<>();

    static {
        parsers.put(0, (buf, helper) -> new ElementText(NetworkUtil.readNextProbeString(buf))); // ID 0 - ElementText
        parsers.put(1, (buf, helper) -> {
            boolean flag = buf.readBoolean();
            CompoundTag item = null;
            Integer count = null;
            if (flag) {
                item = NetworkUtil.readTag(buf);
                count = buf.readInt();
            }
            return new ElementItemStack(item, count, buf.readInt(), buf.readInt());
        }); // ID 1 - ElementItemStack
        parsers.put(2,
                (buf, helper) -> new ElementProgress(buf.readLong(), buf.readLong(), buf.readInt(), buf.readInt(),
                        NetworkUtil.readNextProbeString(buf), NetworkUtil.readNextProbeString(buf), buf.readInt(),
                        buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean(), buf.readByte(),
                        buf.readBoolean(), buf.readBoolean())); // ID 2 - ElementProgress
        parsers.put(3, (buf, helper) -> {
            ArrayList<Element> temp = new ArrayList<>();
            NetworkUtil.readElements(buf, helper, temp);
            return new ElementHorizontal(temp, buf.readBoolean() ? buf.readInt() : null, buf.readShort(),
                    buf.readShort());
        }); // ID 3 - ElementHorizontal
        parsers.put(4, (buf, helper) -> {
            ArrayList<Element> temp = new ArrayList<>();
            NetworkUtil.readElements(buf, helper, temp);
            return new ElementVertical(temp, buf.readBoolean() ? buf.readInt() : null, buf.readShort(),
                    buf.readShort());
        }); // ID 4 - ElementVertical
        parsers.put(5,
                (buf, helper) -> new ElementEntity(NetworkUtil.readNextProbeString(buf), buf.readInt(), buf.readInt(),
                        buf.readFloat(), buf.readBoolean() ? NetworkUtil.readTag(buf) : null,
                        buf.readBoolean() ? buf.readInt() : null)); // ID 5 - ElementEntity
        parsers.put(6, (buf, helper) -> new ElementIcon(
                new ResourceLocation(NetworkUtil.readNextProbeString(buf), NetworkUtil.readNextProbeString(buf)),
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readInt())); // ID 6 - ElementIcon
        parsers.put(7, (buf, helper) -> {
            boolean flag = buf.readBoolean();
            CompoundTag item = null;
            Integer count = null;
            if (flag) {
                item = NetworkUtil.readTag(buf);
                count = buf.readInt();
            }
            return new ElementItemLabel(item, count);
        }); // ID 7 - ElementItemLabel
    }

    public static Element createElement(int id, ByteBuf buf, PacketCodecHelper helper) {
        if (!parsers.containsKey(id)) throw new IllegalArgumentException("Illegal element id");
        return parsers.get(id).apply(buf, helper);
    }
}