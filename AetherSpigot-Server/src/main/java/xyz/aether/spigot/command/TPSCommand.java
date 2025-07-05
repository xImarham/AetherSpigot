package xyz.aether.spigot.command;

import net.minecraft.server.MinecraftServer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.aether.spigot.AetherSpigot;

import java.util.concurrent.TimeUnit;

public class TPSCommand extends Command {
    private static final long STARTUP_TIME = System.currentTimeMillis();
    private static final ChatColor PRIMARY = AetherSpigot.get().getPRIMARY();
    private static final ChatColor VALUE = AetherSpigot.get().getVALUE();

    public TPSCommand() {
        super("tps");
        this.setPermission("aether.command.tps");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        double[] tps = MinecraftServer.getServer().recentTps;
        long totalMemory = Runtime.getRuntime().totalMemory() / 0x100000L;
        long freeMemory = Runtime.getRuntime().freeMemory() / 0x100000L;

        if (sender instanceof Player && !sender.isOp()) {
            sender.sendMessage(PRIMARY + "➥ Current Server TPS: " +
                    VALUE + tpsFormat(tps[0]) + PRIMARY + " (1s), " +
                    VALUE + tpsFormat(tps[1]) + PRIMARY + " (5s), " +
                    VALUE + tpsFormat(tps[2]) + PRIMARY + " (15s)");
            return true;
        }

        sender.sendMessage(PRIMARY + "➥ TPS: " +
                VALUE + tpsFormat(tps[0]) + PRIMARY + " (1s), " +
                VALUE + tpsFormat(tps[1]) + PRIMARY + " (5s), " +
                VALUE + tpsFormat(tps[2]) + PRIMARY + " (15s)");

        sender.sendMessage(PRIMARY + "➥ Uptime: " + VALUE + formatTime(System.currentTimeMillis() - STARTUP_TIME));
        sender.sendMessage(PRIMARY + "➥ Memory: " + VALUE + freeMemory + "MB" +
                PRIMARY + " / " + VALUE + totalMemory + "MB");

        return true;
    }

    private String formatTime(long millis) {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);

        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);

        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s ");

        return sb.toString().trim();
    }

    private String tpsFormat(double tps) {
        ChatColor color = tps > 18.0 ? ChatColor.GREEN : (tps > 16.0 ? ChatColor.YELLOW : ChatColor.RED);
        return color + (tps > 20.0 ? "*" : "") + Math.min(Math.round(tps * 100.0) / 100.0, 20.0) + VALUE;
    }
}
