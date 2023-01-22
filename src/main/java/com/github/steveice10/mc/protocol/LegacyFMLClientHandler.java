package com.github.steveice10.mc.protocol;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.request.InvalidCredentialsException;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.exception.request.ServiceUnavailableException;
import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.data.UnexpectedEncryptionException;
import com.github.steveice10.mc.protocol.data.handshake.HandshakeIntent;
import com.github.steveice10.mc.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundDisconnectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.legacy.LegacyEncryptionResponsePacket;
import com.github.steveice10.mc.protocol.packet.legacy.LegacyLoginStartPacket;
import com.github.steveice10.mc.protocol.packet.legacy.LegacyLoginSuccessPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundHelloPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundLoginCompressionPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundLoginDisconnectPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import io.github.czm23333.onemonitor.minecraft.utils.NetworkUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;

public class LegacyFMLClientHandler extends SessionAdapter {
    private static final String FML_HANDSHAKE_CHANNEL = "FML|HS";
    private volatile List<LegacyFMLClientHandler.ModInfo> modList = Collections.emptyList();

    private static void sendClientHello(Session session, byte protocolVersion) {
        session.send(new ServerboundCustomPayloadPacket(FML_HANDSHAKE_CHANNEL,
                new byte[]{1 /*Discriminator for ClientHello*/, protocolVersion}));
    }

    private static void sendModList(Session session, List<ModInfo> mods) {
        ByteBuf buf = Unpooled.buffer();
        try {
            var helper = session.getCodecHelper();
            buf.writeByte(2); // Discriminator for ModList
            helper.writeVarInt(buf, mods.size()); // Number of mods
            mods.forEach(mod -> {
                helper.writeString(buf, mod.name);
                helper.writeString(buf, mod.version);
            });
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            session.send(new ServerboundCustomPayloadPacket(FML_HANDSHAKE_CHANNEL, bytes));
        } finally {
            buf.release();
        }
    }

    private static void sendHandshakeAck(Session session, byte phase) {
        session.send(new ServerboundCustomPayloadPacket(FML_HANDSHAKE_CHANNEL,
                new byte[]{-1 /*Discriminator for HandshakeAck*/, phase}));
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        MinecraftProtocol protocol = (MinecraftProtocol) session.getPacketProtocol();
        switch (packet) {
            case ClientboundHelloPacket helloPacket -> {
                GameProfile profile = session.getFlag("profile");
                String accessToken = session.getFlag("access-token");
                if (profile == null || accessToken == null) {
                    throw new UnexpectedEncryptionException();
                }

                SecretKey key;
                try {
                    KeyGenerator gen = KeyGenerator.getInstance("AES");
                    gen.init(128);
                    key = gen.generateKey();
                } catch (NoSuchAlgorithmException var14) {
                    throw new IllegalStateException("Failed to generate shared key.", var14);
                }

                SessionService sessionService = session.getFlag("session-service", new SessionService());
                String serverId = sessionService.getServerId(helloPacket.getServerId(), helloPacket.getPublicKey(),
                        key);

                try {
                    sessionService.joinServer(profile, accessToken, serverId);
                } catch (ServiceUnavailableException var11) {
                    session.disconnect("Login failed: Authentication service unavailable.", var11);
                    return;
                } catch (InvalidCredentialsException var12) {
                    session.disconnect("Login failed: Invalid login session.", var12);
                    return;
                } catch (RequestException var13) {
                    session.disconnect("Login failed: Authentication error: " + var13.getMessage(), var13);
                    return;
                }

                session.send(new LegacyEncryptionResponsePacket(helloPacket.getPublicKey(), key,
                        helloPacket.getVerifyToken()));
                session.enableEncryption(protocol.enableEncryption(key));
            }
            case LegacyLoginSuccessPacket ignored -> protocol.setState(ProtocolState.GAME);
            case ClientboundLoginDisconnectPacket disconnectPacket -> session.disconnect(
                    disconnectPacket.getReason().toString());
            case ClientboundLoginCompressionPacket compressionPacket -> session.setCompressionThreshold(
                    compressionPacket.getThreshold(), false);
            case ClientboundKeepAlivePacket keepAlivePacket -> {
                if (session.getFlag("manage-keep-alive", true))
                    session.send(new ServerboundKeepAlivePacket(keepAlivePacket.getPingId()));
            }
            case ClientboundDisconnectPacket disconnectPacket -> session.disconnect(
                    disconnectPacket.getReason().toString());
            case ClientboundCustomPayloadPacket payload -> {
                if (FML_HANDSHAKE_CHANNEL.equals(payload.getChannel())) {
                    ByteBuf buf = Unpooled.wrappedBuffer(payload.getData());
                    try {
                        byte discriminator = buf.readByte();
                        switch (discriminator) {
                            case 0 -> { // ServerHello
                                byte protocolVersion = buf.readByte();
                                NetworkUtil.registerChannels(session, "FML|HS", "FML", "FML|MP", "FML", "FORGE");
                                sendClientHello(session, protocolVersion);
                                sendModList(session, modList);
                            }
                            case 2 -> // ModList
                                    sendHandshakeAck(session, (byte) 2); // WAITINGSERVERDATA
                            case 3 -> { // RegistryData
                                boolean hasMore = buf.readBoolean();
                                if (!hasMore) sendHandshakeAck(session, (byte) 3); // WAITINGSERVERCOMPLETE
                            }
                            case -1 -> { // HandshakeAck
                                byte phase = buf.readByte();
                                switch (phase) {
                                    case 2 -> // WAITINGCACK
                                            sendHandshakeAck(session, (byte) 4); // PENDINGCOMPLETE
                                    case 3 -> // COMPLETE
                                            sendHandshakeAck(session, (byte) 5); // COMPLETE
                                }
                            }
                        }
                    } finally {
                        buf.release();
                    }
                }
            }
            default -> {}
        }
    }

    @Override
    public void packetSent(Session session, Packet packet) {
        if (packet instanceof ClientIntentionPacket) {
            MinecraftProtocol protocol = (MinecraftProtocol) session.getPacketProtocol();
            protocol.setState(ProtocolState.LOGIN);
            GameProfile profile = session.getFlag("profile");
            session.send(new LegacyLoginStartPacket(profile.getName()));
        }
    }

    @Override
    public void connected(ConnectedEvent event) {
        MinecraftProtocol protocol = (MinecraftProtocol) event.getSession().getPacketProtocol();
        event.getSession().send(new ClientIntentionPacket(protocol.getCodec().getProtocolVersion(),
                event.getSession().getHost() + "\0FML\0", event.getSession().getPort(), HandshakeIntent.LOGIN));
    }

    public void setModList(List<ModInfo> modList) {
        this.modList = modList;
    }

    public static class ModInfo {
        public String name;
        public String version;

        public ModInfo(String name, String version) {
            this.name = name;
            this.version = version;
        }
    }
}