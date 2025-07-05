package xyz.aether.spigot.util;

import net.minecraft.server.BlockPosition;
import net.minecraft.server.World;

public final class MCUtil {

    private MCUtil() {
    }

    public static org.bukkit.block.Block toBukkitBlock(World world, BlockPosition pos) {
        if (pos == null) {
            return null;
        }
        return world.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());
    }

}