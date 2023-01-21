package io.github.czm23333.onemonitor.minecraft.packet;

import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import io.netty.buffer.ByteBuf;

public class DummyPacket implements MinecraftPacket {
    public DummyPacket(ByteBuf in, MinecraftCodecHelper helper) {
        in.skipBytes(in.readableBytes());
    }

    @Override
    public void serialize(ByteBuf out, MinecraftCodecHelper helper) {
    }
}