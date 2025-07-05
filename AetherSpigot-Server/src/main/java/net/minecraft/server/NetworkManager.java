package net.minecraft.server;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoopGroup;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.timeout.TimeoutException;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.crypto.SecretKey;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import xyz.aether.spigot.AetherSpigot;
import xyz.aether.spigot.protocol.PacketHandler;
import xyz.aether.spigot.service.PingSpoofService;

public class NetworkManager extends SimpleChannelInboundHandler<Packet> {

    private static final Logger g = LogManager.getLogger();
    public static final Marker a = MarkerManager.getMarker("NETWORK");
    public static final Marker b = MarkerManager.getMarker("NETWORK_PACKETS", NetworkManager.a);
    public static final AttributeKey<EnumProtocol> c = AttributeKey.valueOf("protocol");
    public static final LazyInitVar<NioEventLoopGroup> d = new LazyInitVar() {
        protected NioEventLoopGroup a() {
            return new NioEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Client IO #%d").setDaemon(true).build());
        }

        protected Object init() {
            return this.a();
        }
    };
    public static final LazyInitVar<EpollEventLoopGroup> e = new LazyInitVar() {
        protected EpollEventLoopGroup a() {
            return new EpollEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Epoll Client IO #%d").setDaemon(true).build());
        }

        protected Object init() {
            return this.a();
        }
    };
    public static final LazyInitVar<LocalEventLoopGroup> f = new LazyInitVar() {
        protected LocalEventLoopGroup a() {
            return new LocalEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Local Client IO #%d").setDaemon(true).build());
        }

        protected Object init() {
            return this.a();
        }
    };
    private final EnumProtocolDirection h;
    private final Queue<NetworkManager.QueuedPacket> i = Queues.newConcurrentLinkedQueue();
    // private final ReentrantReadWriteLock j = new ReentrantReadWriteLock(); // PandaSpigot - Remove packet queue locking
    public Channel channel;
    // Spigot Start // PAIL
    public SocketAddress l;
    public java.util.UUID spoofedUUID;
    public com.mojang.authlib.properties.Property[] spoofedProfile;
    public boolean preparing = true;
    // Spigot End
    private PacketListener m;
    private IChatBaseComponent n;
    private boolean o;
    private boolean p;
    private static boolean enableExplicitFlush = Boolean.getBoolean("paper.explicit-flush"); // PandaSpigot
    // PandaSpigot start - Optimize Network
    public boolean isPending = true;
    public boolean queueImmunity = false;
    public EnumProtocol protocol;
    // PandaSpigot end

    // PandaSpigot start - allow controlled flushing
    volatile boolean canFlush = true;
    private final java.util.concurrent.atomic.AtomicInteger packetWrites = new java.util.concurrent.atomic.AtomicInteger();
    private int flushPacketsStart;
    private final Object flushLock = new Object();

    public void disableAutomaticFlush() {
        synchronized (this.flushLock) {
            this.flushPacketsStart = this.packetWrites.get(); // must be volatile and before canFlush = false
            this.canFlush = false;
        }
    }

    public void enableAutomaticFlush() {
        synchronized (this.flushLock) {
            this.canFlush = true;
            if (this.packetWrites.get() != this.flushPacketsStart) { // must be after canFlush = true
                this.flush(); // only make the flush call if we need to
            }
        }
    }

    private void flush() {
        if (this.channel.eventLoop().inEventLoop()) {
            this.channel.flush();
        } else {
            this.channel.eventLoop().execute(() -> {
                this.channel.flush();
            });
        }
    }
    // PandaSpigot end
    // PandaSpigot start - packet limiter
    protected final Object PACKET_LIMIT_LOCK = new Object();
    protected final com.hpfxd.pandaspigot.util.IntervalledCounter allPacketCounts = com.hpfxd.pandaspigot.config.PandaSpigotConfig.get().packetLimiter.getAllPacketsLimit() != null ? new com.hpfxd.pandaspigot.util.IntervalledCounter(
        (long) (com.hpfxd.pandaspigot.config.PandaSpigotConfig.get().packetLimiter.getAllPacketsLimit().packetLimitInterval * 1.0e9)
    ) : null;
    protected final java.util.Map<Class<? extends Packet<?>>, com.hpfxd.pandaspigot.util.IntervalledCounter> packetSpecificLimits = new java.util.HashMap<>();
    private boolean stopReadingPackets;
    private void killForPacketSpam() {
        IChatBaseComponent[] reason = org.bukkit.craftbukkit.util.CraftChatMessage.fromString(org.bukkit.ChatColor.translateAlternateColorCodes('&', com.hpfxd.pandaspigot.config.PandaSpigotConfig.get().packetLimiter.getKickMessage()));
        this.a(new PacketPlayOutKickDisconnect(reason[0]), future -> {
            this.close(reason[0]);
            this.k();
            this.stopReadingPackets = true;
        }, (GenericFutureListener<? extends Future<? super Void>>) null);
    }
    // PandaSpigot end - packet limiter

    // AetherSpigot start - delayed packet queue for artificial ping
    private final Queue<DelayedPacket> delayedPackets = new ConcurrentLinkedQueue<>();

    private static class DelayedPacket {
        private final Packet packet;
        private final GenericFutureListener<? extends Future<? super Void>>[] listeners;
        private final long dispatchTime;

        public DelayedPacket(Packet packet, GenericFutureListener<? extends Future<? super Void>>[] listeners, long dispatchTime) {
            this.packet = packet;
            this.listeners = listeners;
            this.dispatchTime = dispatchTime;
        }

        public Packet getPacket() {
            return packet;
        }

        public GenericFutureListener<? extends Future<? super Void>>[] getListeners() {
            return listeners;
        }

        public long getDispatchTime() {
            return dispatchTime;
        }
    }

    public void tick() {
        Iterator<DelayedPacket> iterator = delayedPackets.iterator();
        long currentTime = System.currentTimeMillis();
        while (iterator.hasNext()) {
            DelayedPacket delayed = iterator.next();
            if (currentTime >= delayed.getDispatchTime()) {
                iterator.remove();
                if (isConnected()) {
                    dispatchPacket(delayed.getPacket(), delayed.getListeners());
                }
            }
        }
    }
    // AetherSpigot end

    public NetworkManager(EnumProtocolDirection enumprotocoldirection) {
        this.h = enumprotocoldirection;
    }

    public void channelActive(ChannelHandlerContext channelhandlercontext) throws Exception {
        super.channelActive(channelhandlercontext);
        this.channel = channelhandlercontext.channel();
        this.l = this.channel.remoteAddress();
        // Spigot Start
        this.preparing = false;
        // Spigot End

        try {
            this.a(EnumProtocol.HANDSHAKING);
        } catch (Throwable throwable) {
            NetworkManager.g.fatal(throwable);
        }

    }

    public void a(EnumProtocol enumprotocol) {
        this.protocol = enumprotocol; // PandaSpigot
        this.channel.attr(NetworkManager.c).set(enumprotocol);
        this.channel.config().setAutoRead(true);
        NetworkManager.g.debug("Enabled auto read");
    }

    public void channelInactive(ChannelHandlerContext channelhandlercontext) throws Exception {
        this.close(new ChatMessage("disconnect.endOfStream", new Object[0]));
    }

    public void exceptionCaught(ChannelHandlerContext channelhandlercontext, Throwable throwable) throws Exception {
        ChatMessage chatmessage;

        if (throwable instanceof TimeoutException) {
            chatmessage = new ChatMessage("disconnect.timeout", new Object[0]);
        } else {
            chatmessage = new ChatMessage("disconnect.genericReason", new Object[] { "Internal Exception: " + throwable});
        }

        this.close(chatmessage);
        if (MinecraftServer.getServer().isDebugging()) throwable.printStackTrace(); // Spigot
    }

    protected void a(ChannelHandlerContext channelhandlercontext, Packet packet) throws Exception {
        if (this.channel.isOpen()) {
            // PandaSpigot start - packet limiter
            if (this.stopReadingPackets) {
                return;
            }
            if (this.allPacketCounts != null ||
                com.hpfxd.pandaspigot.config.PandaSpigotConfig.get().packetLimiter.getPacketSpecificLimits().containsKey(packet.getClass())) {
                long time = System.nanoTime();
                synchronized (PACKET_LIMIT_LOCK) {
                    if (this.allPacketCounts != null) {
                        this.allPacketCounts.updateAndAdd(1, time);
                        if (this.allPacketCounts.getRate() >= com.hpfxd.pandaspigot.config.PandaSpigotConfig.get().packetLimiter.getAllPacketsLimit().maxPacketRate) {
                            this.killForPacketSpam();
                            return;
                        }
                    }

                    for (Class<?> check = packet.getClass(); check != Object.class; check = check.getSuperclass()) {
                        com.hpfxd.pandaspigot.config.PacketLimiterConfig.PacketLimit packetSpecificLimit = com.hpfxd.pandaspigot.config.PandaSpigotConfig.get().packetLimiter.getPacketSpecificLimits().get(check);
                        if (packetSpecificLimit == null) {
                            continue;
                        }
                        com.hpfxd.pandaspigot.util.IntervalledCounter counter = this.packetSpecificLimits.computeIfAbsent((Class) check, clazz -> {
                            return new com.hpfxd.pandaspigot.util.IntervalledCounter((long) (packetSpecificLimit.packetLimitInterval * 1.0e9));
                        });
                        counter.updateAndAdd(1, time);
                        if (counter.getRate() >= packetSpecificLimit.maxPacketRate) {
                            switch (packetSpecificLimit.violateAction) {
                                case DROP:
                                    return;
                                case KICK:
                                    this.killForPacketSpam();
                                    return;
                            }
                        }
                    }
                }
            }
            // PandaSpigot end - packet limiter
            // AetherSpigot start
            if (this.m instanceof PlayerConnection) {
                try {
                    for (PacketHandler packetHandler : AetherSpigot.get().getPacketListeners()) {
                        if (!packetHandler.handleReceivedPacket(this.m, packet)) {
                            return;
                        }
                    }
                } catch (Exception e) {
                    NetworkManager.g.error("Error in PacketListener for packet " + packet.getClass().getSimpleName(), e);
                }
            }
            // AetherSpigot
            try {
                packet.a(this.m);
            } catch (CancelledPacketHandleException cancelledpackethandleexception) {
                ;
            }
        }

    }

    public void a(PacketListener packetlistener) {
        Validate.notNull(packetlistener, "packetListener", new Object[0]);
        NetworkManager.g.debug("Set listener of {} to {}", new Object[] { this, packetlistener});
        this.m = packetlistener;
    }

    // PandaSpigot start
    public EntityPlayer getPlayer() {
        if (this.m instanceof PlayerConnection) {
            return ((PlayerConnection) this.m).player;
        } else {
            return null;
        }
    }
    private static class InnerUtil { // Attempt to hide these methods from ProtocolLib so it doesn't accidently pick them up.
        private static java.util.List<Packet> buildExtraPackets(Packet packet) {
            java.util.List<Packet> extra = packet.getExtraPackets();
            if (extra == null || extra.isEmpty()) {
               return null;
            }
            java.util.List<Packet> ret = new java.util.ArrayList<>(1 + extra.size());
            buildExtraPackets0(extra, ret);
            return ret;
        }

        private static void buildExtraPackets0(java.util.List<Packet> extraPackets, java.util.List<Packet> into) {
            for (Packet extra : extraPackets) {
                into.add(extra);
                java.util.List<Packet> extraExtra = extra.getExtraPackets();
                if (extraExtra != null && !extraExtra.isEmpty()) {
                    buildExtraPackets0(extraExtra, into);
                }
            }
        }
        private static boolean canSendImmediate(NetworkManager networkManager, Packet<?> packet) {
            return networkManager.isPending || networkManager.protocol != EnumProtocol.PLAY ||
                    packet instanceof PacketPlayOutKeepAlive ||
                    packet instanceof PacketPlayOutChat ||
                    packet instanceof PacketPlayOutTabComplete ||
                    packet instanceof PacketPlayOutTitle;
        }
    }
    // PandaSpigot end

    public void handle(Packet packet) {
        this.a(packet, null, (GenericFutureListener<? extends Future<? super Void>>) null); // PandaSpigot
    }
    public void a(Packet packet, GenericFutureListener<? extends Future<? super Void>> genericfuturelistener, GenericFutureListener<? extends Future<? super Void>>... agenericfuturelistener) {
        // AetherSpigot start - artificial ping delay
        GenericFutureListener<? extends Future<? super Void>>[] listener = null;
        if (genericfuturelistener != null || agenericfuturelistener != null) {
            listener = ArrayUtils.add(agenericfuturelistener, 0, genericfuturelistener);
        }
        if (!this.isConnected() && !preparing) {
            return;
        }
        packet.onPacketDispatch(getPlayer());

        EntityPlayer player = getPlayer();
        if (player != null) {
            int artificialPing = PingSpoofService.getArtificialPing(player.getUniqueID());
            if (artificialPing > 0 && !InnerUtil.canSendImmediate(this, packet)) {
                long dispatchTime = System.currentTimeMillis() + artificialPing;
                delayedPackets.add(new DelayedPacket(packet, listener, dispatchTime));
                return;
            }
        }
        // AetherSpigot end
        // PandaSpigot start - handle oversized packets better
        GenericFutureListener<? extends Future<? super Void>>[] listeners = null;
        if (genericfuturelistener != null || agenericfuturelistener != null) { // cannot call ArrayUtils.add with both null arguments
            listeners = ArrayUtils.add(agenericfuturelistener, 0, genericfuturelistener);
        }
        boolean connected = this.isConnected();
        if (!connected && !preparing) {
            return; // Do nothing
        }
        packet.onPacketDispatch(getPlayer());
        if (connected && (InnerUtil.canSendImmediate(this, packet) || (
            MinecraftServer.getServer().isMainThread() && packet.isReady() && this.i.isEmpty() &&
                        (packet.getExtraPackets() == null || packet.getExtraPackets().isEmpty())
        ))) {
            this.writePacket(packet, listeners, null); // PandaSpigot
            return;
        }
        // write the packets to the queue, then flush - antixray hooks there already
        java.util.List<Packet> extraPackets = InnerUtil.buildExtraPackets(packet);
        boolean hasExtraPackets = extraPackets != null && !extraPackets.isEmpty();
        if (!hasExtraPackets) {
            this.i.add(new NetworkManager.QueuedPacket(packet, listeners));
        } else {
            java.util.List<NetworkManager.QueuedPacket> packets = new java.util.ArrayList<>(1 + extraPackets.size());
            packets.add(new NetworkManager.QueuedPacket(packet, (GenericFutureListener<? extends Future<? super Void>>) null)); // delay the future listener until the end of the extra packets

            for (int i = 0, len = extraPackets.size(); i < len;) {
                Packet extra = extraPackets.get(i);
                boolean end = ++i == len;
                packets.add(new NetworkManager.QueuedPacket(extra, end ? listeners : null)); // append listener to the end
            }
            this.i.addAll(packets);
        }

        this.sendPacketQueue();
        // PandaSpigot end
    }

    private void dispatchPacket(final Packet packet, final GenericFutureListener<? extends Future<? super Void>>[] agenericfuturelistener) { this.a(packet, agenericfuturelistener); } // PandaSpigot - OBFHELPER
    private void a(final Packet packet, final GenericFutureListener<? extends Future<? super Void>>[] agenericfuturelistener) {
        // PandaSpigot start - add flush parameter
        this.writePacket(packet, agenericfuturelistener, Boolean.TRUE);
    }
    private void writePacket(final Packet packet, final GenericFutureListener<? extends Future<? super Void>>[] agenericfuturelistener, Boolean flushConditional) {
        this.packetWrites.getAndIncrement(); // must be before using canFlush
        boolean effectiveFlush = flushConditional == null ? this.canFlush : flushConditional;
        final boolean flush = effectiveFlush || packet instanceof PacketPlayOutKeepAlive || packet instanceof PacketPlayOutKickDisconnect; // no delay for certain packets
        // PandaSpigot end - add flush parameter
        final EnumProtocol enumprotocol = EnumProtocol.a(packet);
        final EnumProtocol enumprotocol1 = (EnumProtocol) this.channel.attr(NetworkManager.c).get();

        if (enumprotocol1 != enumprotocol) {
            NetworkManager.g.debug("Disabled auto read");
            this.channel.config().setAutoRead(false);
        }

        EntityPlayer player = getPlayer(); // PandaSpigot
        if (this.channel.eventLoop().inEventLoop()) {
            if (enumprotocol != enumprotocol1) {
                this.a(enumprotocol);
            }

            // PandaSpigot start
            if (!isConnected()) {
                packet.onPacketDispatchFinish(player, null);
                return;
            }
            try {
            // PandaSpigot end
            ChannelFuture channelfuture = (flush) ? this.channel.writeAndFlush(packet) : this.channel.write(packet); // PandaSpigot - add flush parameter

            if (agenericfuturelistener != null) {
                channelfuture.addListeners(agenericfuturelistener);
            }

            // PandaSpigot start
            if (packet.hasFinishListener()) {
                channelfuture.addListener((ChannelFutureListener) channelFuture -> packet.onPacketDispatchFinish(player, channelFuture));
            }
            // PandaSpigot end
            channelfuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            // PandaSpigot start
            } catch (Exception e) {
                g.error("NetworkException: " + player, e);
                close(new ChatMessage("disconnect.genericReason", "Internal Exception: " + e.getMessage()));
                packet.onPacketDispatchFinish(player, null);
            }
            // PandaSpigot end
        } else {
            // PandaSpigot start - optimise packets that are not flushed
            io.netty.util.concurrent.AbstractEventExecutor.LazyRunnable choice1 = null;
            Runnable choice2 = null;
            // note: since the type is not dynamic here, we need to actually copy the old executor code
            // into two branches. On conflict, just re-copy - no changes were made inside the executor code.
            if (!flush) {
                choice1 = () -> {
            // PandaSpigot end
                    if (enumprotocol != enumprotocol1) {
                        NetworkManager.this.a(enumprotocol);
                    }

                // PandaSpigot start
                if (!isConnected()) {
                    packet.onPacketDispatchFinish(player, null);
                    return;
                }
                try {
                // PandaSpigot end
                    ChannelFuture channelfuture = (flush) ? NetworkManager.this.channel.writeAndFlush(packet) : NetworkManager.this.channel.write(packet); // PandaSpigot - add flush parameter

                    if (agenericfuturelistener != null) {
                        channelfuture.addListeners(agenericfuturelistener);
                    }

                // PandaSpigot start
                if (packet.hasFinishListener()) {
                    channelfuture.addListener((ChannelFutureListener) channelFuture -> packet.onPacketDispatchFinish(player, channelFuture));
                }
                // PandaSpigot end
                    channelfuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                // PandaSpigot start
                } catch (Exception e) {
                    g.error("NetworkException: " + player, e);
                    close(new ChatMessage("disconnect.genericReason", "Internal Exception: " + e.getMessage()));
                    packet.onPacketDispatchFinish(player, null);
                }
                // PandaSpigot end
                // PandaSpigot start - optimise packets that are not flushed
                };
            } else {
                choice2 = () -> {
                    if (enumprotocol != enumprotocol1) {
                        NetworkManager.this.a(enumprotocol);
                    }

                    // PandaSpigot start
                    if (!isConnected()) {
                        packet.onPacketDispatchFinish(player, null);
                        return;
                    }
                    try {
                        // PandaSpigot end
                        ChannelFuture channelfuture = (flush) ? NetworkManager.this.channel.writeAndFlush(packet) : NetworkManager.this.channel.write(packet); // PandaSpigot - add flush parameter

                        if (agenericfuturelistener != null) {
                            channelfuture.addListeners(agenericfuturelistener);
                        }

                        // PandaSpigot start
                        if (packet.hasFinishListener()) {
                            channelfuture.addListener((ChannelFutureListener) channelFuture -> packet.onPacketDispatchFinish(player, channelFuture));
                        }
                        // PandaSpigot end
                        channelfuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                        // PandaSpigot start
                    } catch (Exception e) {
                        g.error("NetworkException: " + player, e);
                        close(new ChatMessage("disconnect.genericReason", "Internal Exception: " + e.getMessage()));
                        packet.onPacketDispatchFinish(player, null);
                    }
                    // PandaSpigot end
                };
            }
            this.channel.eventLoop().execute(choice1 != null ? choice1 : choice2);
            // PandaSpigot end
        }

    }

    // PandaSpigot start - rewrite this to be safer if ran off main thread
    private boolean sendPacketQueue() { return this.m(); } // OBFHELPER // void -> boolean
    private boolean m() { // void -> boolean
        if (!isConnected()) {
            return true;
        }
        if (MinecraftServer.getServer().isMainThread()) {
            return processQueue();
        } else if (isPending) {
            // Should only happen during login/status stages
            synchronized (this.i) {
                return this.processQueue();
            }
        }
        return false;
    }
    private boolean processQueue() {
        if (this.i.isEmpty()) return true;
        // PandaSpigot start - make only one flush call per sendPacketQueue() call
        final boolean needsFlush = this.canFlush; // make only one flush call per sendPacketQueue() call
        boolean hasWrotePacket = false;
        // PandaSpigot end
        // If we are on main, we are safe here in that nothing else should be processing queue off main anymore
        // But if we are not on main due to login/status, the parent is synchronized on packetQueue
        java.util.Iterator<QueuedPacket> iterator = this.i.iterator();
        while (iterator.hasNext()) {
            NetworkManager.QueuedPacket queued = iterator.next(); // poll -> peek
            // Fix NPE (Spigot bug caused by handleDisconnection())
            if (false && queued == null) { // PandaSpigot - diff on change, this logic is redundant: iterator guarantees ret of an element - on change, hook the flush logic here
                return true;
            }

            Packet packet = queued.getPacket();
            if (!packet.isReady()) {
                // PandaSpigot start - make only one flush call per sendPacketQueue() call
                if (hasWrotePacket && (needsFlush || this.canFlush)) {
                    this.flush();
                }
                // PandaSpigot end
                return false;
            } else {
                iterator.remove();
                // PandaSpigot start - make only one flush call per sendPacketQueue() call
                this.writePacket(packet, queued.getGenericFutureListener(), (!iterator.hasNext() && (needsFlush || this.canFlush)) ? Boolean.TRUE : Boolean.FALSE);
                hasWrotePacket = true;
                // PandaSpigot end
            }
        }
        return true;
    }
    // PandaSpigot end

    public void a() {
        this.tick(); // AetherSpigot - process delayed packets
        this.m();
        if (this.m instanceof IUpdatePlayerListBox) {
            ((IUpdatePlayerListBox) this.m).c();
        }

        if (enableExplicitFlush) this.channel.eventLoop().execute(() -> this.channel.flush()); // PandaSpigot - Allow Disabling Explicit Network Manager Flushing
    }

    public SocketAddress getSocketAddress() {
        return this.l;
    }

    // PandaSpigot start
    public void clearPacketQueue() {
        EntityPlayer player = getPlayer();
        i.forEach(queuedPacket -> {
            Packet packet = queuedPacket.getPacket();
            if (packet.hasFinishListener()) {
                packet.onPacketDispatchFinish(player, null);
            }
        });
        i.clear();
        delayedPackets.clear(); // AetherSpigot - Clear delayed packets
    } // PandaSpigot end
    public void close(IChatBaseComponent ichatbasecomponent) {
        // Spigot Start
        this.preparing = false;
        clearPacketQueue(); // PandaSpigot
        // Spigot End
        if (this.channel.isOpen()) {
            this.channel.close(); // We can't wait as this may be called from an event loop.
            this.n = ichatbasecomponent;
            // AetherSpigot start - clear artificial ping on disconnect
            EntityPlayer player = getPlayer();
            if (player != null) {
                PingSpoofService.clearArtificialPing(player.getUniqueID());
            }
            // AetherSpigot end
        }

    }

    public boolean c() {
        return this.channel instanceof LocalChannel || this.channel instanceof LocalServerChannel;
    }

    public void a(SecretKey secretkey) {
        this.o = true;
        this.channel.pipeline().addBefore("splitter", "decrypt", new PacketDecrypter(MinecraftEncryption.a(2, secretkey)));
        this.channel.pipeline().addBefore("prepender", "encrypt", new PacketEncrypter(MinecraftEncryption.a(1, secretkey)));
    }

    public boolean isConnected() { return this.g(); } // PandaSpigot - OBFHELPER
    public boolean g() {
        return this.channel != null && this.channel.isOpen();
    }

    public boolean h() {
        return this.channel == null;
    }

    public PacketListener getPacketListener() {
        return this.m;
    }

    public IChatBaseComponent j() {
        return this.n;
    }

    public void k() {
        this.channel.config().setAutoRead(false);
    }

    public void a(int i) {
        if (i >= 0) {
            if (this.channel.pipeline().get("decompress") instanceof PacketDecompressor) {
                ((PacketDecompressor) this.channel.pipeline().get("decompress")).a(i);
            } else {
                this.channel.pipeline().addBefore("decoder", "decompress", new PacketDecompressor(i));
            }

            if (this.channel.pipeline().get("compress") instanceof PacketCompressor) {
                ((PacketCompressor) this.channel.pipeline().get("compress")).a(i); // PandaSpigot - Fix wrong handler name used for cast to PacketCompressor
            } else {
                this.channel.pipeline().addBefore("encoder", "compress", new PacketCompressor(i));
            }
        } else {
            if (this.channel.pipeline().get("decompress") instanceof PacketDecompressor) {
                this.channel.pipeline().remove("decompress");
            }

            if (this.channel.pipeline().get("compress") instanceof PacketCompressor) {
                this.channel.pipeline().remove("compress");
            }
        }

    }

    public void l() {
        if (this.channel != null && !this.channel.isOpen()) {
            if (!this.p) {
                this.p = true;
                if (this.j() != null) {
                    this.getPacketListener().a(this.j());
                } else if (this.getPacketListener() != null) {
                    this.getPacketListener().a(new ChatComponentText("Disconnected"));
                }
                clearPacketQueue(); // PandaSpigot
            } else {
                //NetworkManager.g.warn("handleDisconnection() called twice"); // PandaSpigot - Do not log useless message
            }

        }
    }

    protected void channelRead0(ChannelHandlerContext channelhandlercontext, Packet object) throws Exception { // CraftBukkit - fix decompile error
        this.a(channelhandlercontext, (Packet) object);
    }

    static class QueuedPacket {

        private final Packet a; private final Packet getPacket() { return this.a; } // PandaSpigot - OBFHELPER
        private final GenericFutureListener<? extends Future<? super Void>>[] b; private final GenericFutureListener<? extends Future<? super Void>>[] getGenericFutureListener() { return this.b; } // PandaSpigot - OBFHELPER

        public QueuedPacket(Packet packet, GenericFutureListener<? extends Future<? super Void>>... agenericfuturelistener) {
            this.a = packet;
            this.b = agenericfuturelistener;
        }
    }

    // Spigot Start
    public SocketAddress getRawAddress()
    {
        // PandaSpigot start - This can be null in the case of a Unix domain socket, so if it is, fake something
        if (this.channel.remoteAddress() == null) {
            return new java.net.InetSocketAddress(java.net.InetAddress.getLoopbackAddress(), 0);
        }
        // PandaSpigot end
        return this.channel.remoteAddress();
    }
    // Spigot End
}
