package io.github.czm23333.onemonitor.minecraft.utils;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundCustomPayloadPacket;
import com.github.steveice10.opennbt.NBTIO;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.codec.PacketCodecHelper;
import io.github.czm23333.onemonitor.minecraft.oneprobe.elements.Element;
import io.github.czm23333.onemonitor.minecraft.oneprobe.elements.ElementFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NetworkUtil {
    private static final Logger LOGGER = Logger.getLogger("NetworkUtil");
    private static final String REGISTER_CHANNEL = "REGISTER";

    public static String readNextProbeString(ByteBuf in)  {
        int length = in.readInt();
        if (length > 0) return in.readBytes(length).toString(StandardCharsets.UTF_8);
        else return "";
    }

    public static void readElements(ByteBuf in, PacketCodecHelper helper, List<Element> out) {
        short size = in.readShort();
        for (short i = 0; i < size; ++i) {
            int id = in.readInt();
            out.add(ElementFactory.createElement(id, in, helper));
        }
    }

    public static CompoundTag readTag(ByteBuf buf) {
        return readTag(buf, CompoundTag.class);
    }

    @Nullable
    public static <T extends Tag> T readTag(final ByteBuf buf, Class<T> expected) {
        Tag tag = null;
        try {
            tag = NBTIO.readTag(new InputStream() {
                public int read() {
                    return buf.readUnsignedByte();
                }
            });
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while reading NBT: ", e);
        }
        if (tag == null) {
            return null;
        } else if (tag.getClass() != expected) {
            throw new IllegalArgumentException("Expected tag of type " + expected.getName() + " but got " + tag.getClass().getName());
        } else {
            return expected.cast(tag);
        }
    }

    public static void registerChannels(Session session, String... channelNames) {
        ByteBuf buf = Unpooled.buffer();
        try {
            for (String channelName : channelNames) {
                session.getCodecHelper().writeString(buf, channelName);
                buf.writeByte(0);
            }
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            session.send(new ServerboundCustomPayloadPacket(REGISTER_CHANNEL, bytes));
        } finally {
            buf.release();
        }
    }
}