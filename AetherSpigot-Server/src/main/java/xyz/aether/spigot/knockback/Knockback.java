package xyz.aether.spigot.knockback;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Knockback implements IKnockback {

    private final KnockbackHandler knockbackHandler;

    public Knockback(KnockbackHandler knockbackHandler) {
        this.knockbackHandler = knockbackHandler;
    }

    @Override
    public boolean setPlayerProfile(UUID playerUUID, String profileName) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) {
            return false;
        }

        KnockbackProfile profile = knockbackHandler.getKnockbackProfileByName(profileName, true);
        if (profile == null) {
            return false;
        }

        knockbackHandler.assignProfileToPlayer(player, profile);
        return true;
    }

    @Override
    public String getActiveProfileName() {
        return knockbackHandler.getActiveProfile().getName();
    }

    @Override
    public Set<String> getAvailableProfileNames() {
        return knockbackHandler.getKnockbackProfiles()
                .stream()
                .map(KnockbackProfile::getName)
                .collect(Collectors.toSet());
    }
}
