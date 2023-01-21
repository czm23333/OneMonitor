package io.github.czm23333.onemonitor.minecraft;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.mc.protocol.LegacyFMLClientHandler;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.codec.PacketCodec;
import com.github.steveice10.mc.protocol.codec.PacketStateCodec;
import com.github.steveice10.mc.protocol.data.ProtocolState;
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
import com.github.steveice10.mc.protocol.packet.status.clientbound.ClientboundPongResponsePacket;
import com.github.steveice10.mc.protocol.packet.status.serverbound.ServerboundPingRequestPacket;
import com.github.steveice10.mc.protocol.packet.status.serverbound.ServerboundStatusRequestPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import io.github.czm23333.onemonitor.minecraft.oneprobe.ProbeMode;
import io.github.czm23333.onemonitor.minecraft.oneprobe.ProbeRequest;
import io.github.czm23333.onemonitor.minecraft.oneprobe.ProbeResponse;
import io.github.czm23333.onemonitor.minecraft.packet.DummyPacket;
import io.github.czm23333.onemonitor.minecraft.packet.ExClientboundStatusResponsePacket;
import io.github.czm23333.onemonitor.minecraft.utils.NetworkUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MinecraftBot extends SessionAdapter {
    private static final Logger LOGGER = Logger.getLogger("MinecraftBot");
    private static final String ONE_PROBE_CHANNEL = "theoneprobe";
    private static final int PACKET_GET_INFO_ID = 0;
    private static final int PACKET_RETURN_INFO_ID = 2;
    private static final PacketCodec MINECRAFT_1122_NECESSARY_CODEC;
    private static final PacketCodec FML_STATUS_CODEC = PacketCodec.builder().protocolVersion(760)
            .helper(() -> new MinecraftCodecHelper(new Int2ObjectOpenHashMap<>(), Collections.emptyMap()))
            .minecraftVersion("1.19.1").state(ProtocolState.HANDSHAKE, PacketStateCodec.builder()
                    .registerServerboundPacket(0, ClientIntentionPacket.class, ClientIntentionPacket::new))
            .state(ProtocolState.STATUS, PacketStateCodec.builder()
                    .registerClientboundPacket(0, ExClientboundStatusResponsePacket.class,
                            ExClientboundStatusResponsePacket::new)
                    .registerClientboundPacket(1, ClientboundPongResponsePacket.class,
                            ClientboundPongResponsePacket::new)
                    .registerServerboundPacket(0, ServerboundStatusRequestPacket.class,
                            ServerboundStatusRequestPacket::new)
                    .registerServerboundPacket(1, ServerboundPingRequestPacket.class,
                            ServerboundPingRequestPacket::new)).build();

    static {
        var builder = PacketCodec.builder().protocolVersion(340)
                .helper(() -> new MinecraftCodecHelper(new Int2ObjectOpenHashMap<>(), Collections.emptyMap()))
                .minecraftVersion("1.12.2").state(ProtocolState.HANDSHAKE, PacketStateCodec.builder()
                        .registerServerboundPacket(0, ClientIntentionPacket.class, ClientIntentionPacket::new));
        var loginBuilder = PacketStateCodec.builder();
        var gameBuilder = PacketStateCodec.builder();
        for (int i = 0; i <= 200; ++i) {
            switch (i) {
                case 0 -> loginBuilder.registerClientboundPacket(0, ClientboundLoginDisconnectPacket.class,
                        ClientboundLoginDisconnectPacket::new);
                case 1 -> loginBuilder.registerClientboundPacket(1, ClientboundHelloPacket.class,
                        ClientboundHelloPacket::new);
                case 2 -> loginBuilder.registerClientboundPacket(2, LegacyLoginSuccessPacket.class,
                        LegacyLoginSuccessPacket::new);
                case 3 -> loginBuilder.registerClientboundPacket(3, ClientboundLoginCompressionPacket.class,
                        ClientboundLoginCompressionPacket::new);
                default -> loginBuilder.registerClientboundPacket(i, DummyPacket.class, DummyPacket::new);
            }
        }
        for (int i = 0; i <= 200; ++i) {
            switch (i) {
                case 0 -> loginBuilder.registerServerboundPacket(0, LegacyLoginStartPacket.class,
                        LegacyLoginStartPacket::new);
                case 1 -> loginBuilder.registerServerboundPacket(1, LegacyEncryptionResponsePacket.class,
                        LegacyEncryptionResponsePacket::new);
                default -> loginBuilder.registerServerboundPacket(i, DummyPacket.class, DummyPacket::new);
            }
        }
        for (int i = 0; i <= 200; ++i) {
            switch (i) {
                case 24 -> gameBuilder.registerClientboundPacket(24, ClientboundCustomPayloadPacket.class,
                        ClientboundCustomPayloadPacket::new);
                case 26 -> gameBuilder.registerClientboundPacket(26, ClientboundDisconnectPacket.class,
                        ClientboundDisconnectPacket::new);
                case 31 -> gameBuilder.registerClientboundPacket(31, ClientboundKeepAlivePacket.class,
                        ClientboundKeepAlivePacket::new);
                default -> gameBuilder.registerClientboundPacket(i, DummyPacket.class, DummyPacket::new);
            }
        }
        for (int i = 0; i <= 200; ++i) {
            switch (i) {
                case 9 -> gameBuilder.registerServerboundPacket(9, ServerboundCustomPayloadPacket.class,
                        ServerboundCustomPayloadPacket::new);
                case 11 -> gameBuilder.registerServerboundPacket(11, ServerboundKeepAlivePacket.class,
                        ServerboundKeepAlivePacket::new);
                default -> gameBuilder.registerServerboundPacket(i, DummyPacket.class, DummyPacket::new);
            }
        }
        MINECRAFT_1122_NECESSARY_CODEC = builder.state(ProtocolState.LOGIN, loginBuilder).state(ProtocolState.GAME, gameBuilder).build();
    }

    private final SessionService service = new SessionService();
    private final BlockingQueue<ProbeRequest> channel;
    private final Thread worker;
    private final MinecraftProtocol protocol;
    private final LegacyFMLClientHandler handler = new LegacyFMLClientHandler();
    private volatile long timeoutMillis = 5000;
    private volatile TcpClientSession session;
    private volatile boolean autoReconnect = false;
    private volatile boolean shutdownFlag = false;
    private volatile ProbeResponse responseTemp;
    private volatile String address;
    private volatile int port;

    public MinecraftBot(BlockingQueue<ProbeRequest> channel, GameProfile profile, String accessToken) {
        this.channel = channel;
        MinecraftProtocol protocol = new MinecraftProtocol(MINECRAFT_1122_NECESSARY_CODEC, profile, accessToken);
        protocol.setUseDefaultListeners(false);
        this.protocol = protocol;
        this.worker = new Thread(() -> {
            do {
                try {
                    ProbeRequest request = this.channel.take();
                    responseTemp = new ProbeResponse();
                    responseTemp.timedOut = true;
                    probe(request.dim(), request.x(), request.y(), request.z());
                    synchronized (this) {
                        this.wait(timeoutMillis);
                    }
                    request.callback().accept(responseTemp);
                } catch (InterruptedException ignored) {
                }
            } while (!shutdownFlag);
        });
    }

    public void initModList(String address, int port) {
        MinecraftProtocol protocol = new MinecraftProtocol(FML_STATUS_CODEC);
        Session client = new TcpClientSession(address, port, protocol);
        client.setFlag(MinecraftConstants.SESSION_SERVICE_KEY, service);
        client.addListener(new SessionAdapter() {
            @Override
            public void packetReceived(Session session, Packet packet) {
                if (packet instanceof ExClientboundStatusResponsePacket response) {
                    ArrayList<LegacyFMLClientHandler.ModInfo> mods = new ArrayList<>();
                    response.jsonObject.getAsJsonObject("modinfo").getAsJsonArray("modList").forEach(infoE -> {
                        var info = infoE.getAsJsonObject();
                        mods.add(new LegacyFMLClientHandler.ModInfo(info.get("modid").getAsString(),
                                info.get("version").getAsString()));
                    });
                    handler.setModList(mods);
                    synchronized (client) {
                        client.notify();
                    }
                }
            }
        });
        client.connect();
        synchronized (client) {
            try {
                client.wait();
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Error while waiting for mod list response: ", e);
            }
        }
    }

    public void connect(String address, int port) {
        if (shutdownFlag)
            throw new IllegalStateException("Tried to establish new session when the instance has shut down");
        if (session != null && session.isConnected())
            throw new IllegalStateException("Tried to establish new session when a session is still ongoing");
        session = new TcpClientSession(address, port, protocol);
        this.address = address;
        this.port = port;
        session.setFlag(MinecraftConstants.SESSION_SERVICE_KEY, service);
        session.addListener(handler);
        session.addListener(this);
        session.connect();
    }

    public void disconnect() {
        if (session == null) return;
        TcpClientSession temp = session;
        session = null;
        temp.disconnect("Disconnected");
    }

    public void shutdown() {
        shutdownFlag = true;
        synchronized (worker) {
            worker.interrupt();
        }
        disconnect();
    }

    public void probe(int dim, int x, int y, int z) {
        ByteBuf buf = Unpooled.buffer();
        try {
            var helper = session.getCodecHelper();
            helper.writeVarInt(buf, PACKET_GET_INFO_ID); // PacketGetInfo ID
            buf.writeInt(dim); // Block dim
            buf.writeInt(x); // Block location
            buf.writeInt(y);
            buf.writeInt(z);
            buf.writeByte(ProbeMode.EXTENDED.ordinal()); //  Extended probe mode
            buf.writeByte(127); // No hit side
            buf.writeBoolean(false); // No hit vector
            buf.writeInt(0); // Empty item -  empty NBT (TAG_END)
            buf.writeInt(0); // Empty item - 0 count
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            session.send(new ServerboundCustomPayloadPacket(ONE_PROBE_CHANNEL, bytes));
        } finally {
            buf.release();
        }
    }

    @Override
    public void packetReceived(Session _session, Packet packet) {
        switch (packet) {
            case LegacyLoginSuccessPacket ignored -> {
                LOGGER.info("Minecraft bot logged in");
                NetworkUtil.registerChannels(session, ONE_PROBE_CHANNEL);
                synchronized (worker) {
                    if (!worker.isAlive()) worker.start();
                }
            }
            case ClientboundLoginDisconnectPacket loginDisconnect -> {
                disconnected(new DisconnectedEvent(_session, loginDisconnect.getReason().toString()));
            }
            case ClientboundCustomPayloadPacket payload -> {
                if (ONE_PROBE_CHANNEL.equals(payload.getChannel())) {
                    var helper = session.getCodecHelper();
                    ByteBuf buf = Unpooled.wrappedBuffer(payload.getData());
                    try {
                        int id = helper.readVarInt(buf);
                        if (id == PACKET_RETURN_INFO_ID) {
                            LOGGER.log(Level.INFO, "Probe response in dim {0} at {1}, {2}, {3}",
                                    new Integer[]{buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt()});
                            responseTemp = parseResponse(buf);
                            synchronized (this) {
                                this.notify();
                            }
                        }
                    } finally {
                        buf.release();
                    }
                }
            }
            default -> {
            }
        }
    }

    private ProbeResponse parseResponse(ByteBuf buf) {
        var helper = session.getCodecHelper();
        var response = new ProbeResponse();
        if (!(response.hasElements = buf.readBoolean())) return response;
        NetworkUtil.readElements(buf, helper, response.elements);
        return response;
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        LOGGER.info("Minecraft bot disconnected with " + event.getReason());
        if (autoReconnect && session != null) {
            LOGGER.info("Minecraft bot reconnecting");
            session = null;
            connect(address, port);
        }
    }

    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    @Override
    public void connected(ConnectedEvent event) {
        LOGGER.info("Minecraft bot connected");
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

}