package xyz.aether.spigot.combat.listener;

import net.minecraft.server.EntityPlayer;
import org.bukkit.entity.Player;
import xyz.aether.spigot.AetherSpigot;
import xyz.aether.spigot.combat.data.PlayerData;

public class ConnectionListener {

    public static void onPlayerJoin(EntityPlayer player) {
        Player bukkitPlayer = player.getBukkitEntity();
        AetherSpigot.get().getPlayerDataManager().get(bukkitPlayer);
    }

    public static void onPlayerQuit(EntityPlayer player) {
        Player bukkitPlayer = player.getBukkitEntity();
        PlayerData playerData = AetherSpigot.get().getPlayerDataManager().get(bukkitPlayer);

        if (playerData.isInCombat()) playerData.quitCombat(true);

        AetherSpigot.get().getPlayerDataManager().remove(bukkitPlayer);
    }
}
