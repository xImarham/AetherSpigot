package xyz.aether.spigot.combat.runnable;

import org.bukkit.Bukkit;
import xyz.aether.spigot.AetherSpigot;
import xyz.aether.spigot.combat.data.PlayerData;

import java.util.TimerTask;

public class PingRunnable extends TimerTask {

    @Override
    public void run() {
        if (AetherSpigot.get().getSync() == null) {
            return;
        }

        if (!AetherSpigot.get().getSync().isToggled()) {
            return;
        }

        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            PlayerData playerData = AetherSpigot.get().getPlayerDataManager().get(player);
            if (playerData != null) {
                if (!playerData.isInCombat()) {
                    playerData.updateCombat();
                }
                playerData.sendPing();
            }
        }

        /*
        for (UUID uuid : CombatManager.getPlayers()) {
            PlayerData playerData = PlayerDataManager.getPlayerData(uuid);
            if (playerData != null) {
                playerData.sendPing();
                Bukkit.broadcastMessage("sent ping to player " + uuid);
            }
        }
        */
    }
}
