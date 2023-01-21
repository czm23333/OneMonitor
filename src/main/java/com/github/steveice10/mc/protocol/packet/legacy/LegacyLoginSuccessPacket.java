package com.github.steveice10.mc.protocol.packet.legacy;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import io.netty.buffer.ByteBuf;

import java.io.IOException;

public class LegacyLoginSuccessPacket implements MinecraftPacket {
    private final GameProfile profile;

    public LegacyLoginSuccessPacket(ByteBuf in, MinecraftCodecHelper helper) throws IOException {
        profile = new GameProfile(helper.readString(in), helper.readString(in));
    }

    public void serialize(ByteBuf out, MinecraftCodecHelper helper) throws IOException {
        helper.writeUUID(out, profile.getId());
        helper.writeString(out, profile.getName());
    }
}