package xyz.aether.spigot.combat.listener;

import net.minecraft.server.PacketPlayInKeepAlive;
import net.minecraft.server.PlayerConnection;
import xyz.aether.spigot.AetherSpigot;
import xyz.aether.spigot.combat.data.PlayerData;
import xyz.aether.spigot.protocol.PacketHandler;

public class PingReceiveListener implements PacketHandler {

    @Override
    public boolean handleReceivedPacket(Object connection, Object packet) {
        if (!(packet instanceof PacketPlayInKeepAlive))
            return true;

        if (!(connection instanceof PlayerConnection))
            return true;

        if (AetherSpigot.get().getSync() == null || !AetherSpigot.get().getSync().isToggled()) // KnockbackSync might not just be initialized so just check for that too
            return true;

        PlayerData playerData = AetherSpigot.get().getPlayerDataManager().get(((PlayerConnection) connection).player.getBukkitEntity());

        int packetId = ((PacketPlayInKeepAlive) packet).a();

        Long sendTime = playerData.getTimeline().get(packetId);
        if (sendTime == null) {
            return true;
        }

        long ping = System.currentTimeMillis() - sendTime;

        playerData.getTimeline().remove(packetId);
        playerData.setPreviousPing(playerData.getPing() != null ? playerData.getPing() : ping);
        playerData.setPing(ping);
        return true;
    }
}
