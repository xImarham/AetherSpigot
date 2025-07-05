package xyz.aether.spigot.service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PingSpoofService {
    private static final ConcurrentHashMap<UUID, Integer> artificialPings = new ConcurrentHashMap<>();

    public static void setArtificialPing(UUID playerId, int ping) {
        if (ping <= 0) {
            artificialPings.remove(playerId);
        } else {
            artificialPings.put(playerId, ping);
        }
    }

    public static int getArtificialPing(UUID playerId) {
        return artificialPings.getOrDefault(playerId, -1);
    }

    public static void clearArtificialPing(UUID playerId) {
        artificialPings.remove(playerId);
    }
}