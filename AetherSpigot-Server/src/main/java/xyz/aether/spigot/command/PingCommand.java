package xyz.aether.spigot.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.aether.spigot.service.PingSpoofService;

import java.util.Collections;

public class PingCommand extends Command {
    public PingCommand() {
        super("ping");
        this.setAliases(Collections.singletonList("ps"));
        this.setPermission("aether.command.ping");
        this.setDescription("Check or spoof a player's ping");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        Player player = (Player) sender;
        if (!testPermission(player)) return true;
        // /ping
        if (args.length == 0) {
            int artificialPing = PingSpoofService.getArtificialPing(player.getUniqueId());
            int ping = player.getPing();
            if (artificialPing > 0) {
                ping += artificialPing;
                sender.sendMessage(ChatColor.GREEN + "Your ping is " + ping + "ms" + " (spoofed).");
            } else {
                sender.sendMessage(ChatColor.GREEN + "Your ping is " + ping + "ms" + ".");
            }
            return true;
        }

        // /ping <player>
        if (args.length == 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null && target.isOnline()) {
                int artificialPing = PingSpoofService.getArtificialPing(target.getUniqueId());
                int ping = target.getPing();
                if (artificialPing > 0) {
                    ping += artificialPing;
                    sender.sendMessage(ChatColor.GREEN + target.getName() + "'s ping is " + ping + "ms");
                } else {
                    sender.sendMessage(ChatColor.GREEN + target.getName() + "'s ping is " + ping + "ms" + ".");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Player not found or offline.");
            }
            return true;
        }

        // /ping set <player> <ping>
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(ChatColor.RED + "Player not found or offline.");
                return true;
            }

            try {
                int ping = Integer.parseInt(args[2]);
                PingSpoofService.setArtificialPing(target.getUniqueId(), ping);
                sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s ping to " + ping + "ms" + ".");
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Ping must be a valid number.");
            }
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Wrong usage.");
        return true;
    }
}
