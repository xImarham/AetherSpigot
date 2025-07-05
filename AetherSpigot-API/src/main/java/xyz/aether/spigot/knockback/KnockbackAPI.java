package xyz.aether.spigot.knockback;

import java.util.Set;
import java.util.UUID;

public class KnockbackAPI {

    private static IKnockback delegate;

    public static void registerDelegate(IKnockback delegate) {
        KnockbackAPI.delegate = delegate;
    }

    public static String getActiveProfileName() {
        return delegate.getActiveProfileName();
    }
    public static Set<String> getAvailableProfileNames() {
        return delegate.getAvailableProfileNames();
    }

    public static boolean setPlayerProfile(UUID playerUUID, String profileName) {
        return delegate.setPlayerProfile(playerUUID, profileName);
    }
}