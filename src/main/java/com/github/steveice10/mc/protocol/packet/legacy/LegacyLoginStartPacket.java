package com.github.steveice10.mc.protocol.packet.legacy;

import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import io.netty.buffer.ByteBuf;

import java.io.IOException;

public class LegacyLoginStartPacket implements MinecraftPacket {
    private final String username;

    public LegacyLoginStartPacket(String username) {
        this.username = username;
    }

    public LegacyLoginStartPacket(ByteBuf in, MinecraftCodecHelper helper) {
        username = helper.readString(in);
    }

    @Override
    public void serialize(ByteBuf out, MinecraftCodecHelper helper) throws IOException {
        helper.writeString(out, this.username);
    }
}