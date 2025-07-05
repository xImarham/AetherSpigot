package com.hpfxd.pandaspigot;

import org.bukkit.Bukkit;

public class CompactHacks {

    private CompactHacks() {}

    private static Boolean hasProtocolSupport = null;

    public static boolean hasProtocolSupport() {
        if (hasProtocolSupport != null) {
            return hasProtocolSupport;
        }

        hasProtocolSupport = Bukkit.getPluginManager().isPluginEnabled("ProtocolSupport");
        return hasProtocolSupport;
    }
}
