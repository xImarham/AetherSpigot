package xyz.aether.spigot.combat.data;

import net.minecraft.server.EntityPlayer;
import net.minecraft.server.PlayerConnection;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private final Map<Player, PlayerData> playerDataMap = new ConcurrentHashMap<>();

    public void cache(Player player, PlayerData data) {
        playerDataMap.put(player, data);
    }

    public void remove(Player player) {
        playerDataMap.remove(player);
    }

    public PlayerData get(Player player) {
        PlayerData data = playerDataMap.get(player);
        if (data == null) {
            data = new PlayerData();
            if (player != null) {
                EntityPlayer entityPlayer = ((org.bukkit.craftbukkit.entity.CraftPlayer) player).getHandle();
                data.setPlayer(entityPlayer);
            }
            playerDataMap.put(player, data);
        }
        return data;
    }

    public PlayerData get(PlayerConnection connection) {
        EntityPlayer entityPlayer = connection.player;
        Player player = entityPlayer.getBukkitEntity();
        return get(player);
    }
}