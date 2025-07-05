package xyz.aether.spigot.knockback;

import java.util.Set;
import java.util.UUID;

public interface IKnockback {

    boolean setPlayerProfile(UUID playerUUID, String profileName);

    String getActiveProfileName();

    Set<String> getAvailableProfileNames();
}
