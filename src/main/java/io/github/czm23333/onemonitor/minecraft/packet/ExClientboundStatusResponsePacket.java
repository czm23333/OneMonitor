package io.github.czm23333.onemonitor.minecraft.packet;

import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.czm23333.onemonitor.CommonInstance;
import io.netty.buffer.ByteBuf;

import java.io.IOException;

public class ExClientboundStatusResponsePacket implements MinecraftPacket {
    public JsonObject jsonObject;

    public ExClientboundStatusResponsePacket(ByteBuf in, MinecraftCodecHelper helper) {
        jsonObject = JsonParser.parseString(helper.readString(in)).getAsJsonObject();
    }

    @Override
    public void serialize(ByteBuf byteBuf, MinecraftCodecHelper minecraftCodecHelper) throws IOException {
        minecraftCodecHelper.writeString(byteBuf, CommonInstance.GSON.toJson(jsonObject));
    }
}