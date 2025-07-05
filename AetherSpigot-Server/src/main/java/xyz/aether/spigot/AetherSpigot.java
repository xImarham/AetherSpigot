package xyz.aether.spigot;

import com.google.common.collect.Sets;
import lombok.Getter;
import net.minecraft.server.MinecraftServer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import xyz.aether.spigot.combat.data.PlayerDataManager;
import xyz.aether.spigot.combat.listener.PingReceiveListener;
import xyz.aether.spigot.combat.sync.KnockbackSync;
import xyz.aether.spigot.command.KnockbackCommand;
import xyz.aether.spigot.command.PingCommand;
import xyz.aether.spigot.command.TPSCommand;
import xyz.aether.spigot.config.AetherConfig;
import xyz.aether.spigot.knockback.Knockback;
import xyz.aether.spigot.knockback.KnockbackAPI;
import xyz.aether.spigot.knockback.IKnockback;
import xyz.aether.spigot.knockback.KnockbackHandler;
import xyz.aether.spigot.misc.BowBoost;
import xyz.aether.spigot.protocol.PacketHandler;
import xyz.aether.spigot.util.YamlConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Getter
public class AetherSpigot {

    private static AetherSpigot instance;
    private final BowBoost bowBoost;

    private final Set<PacketHandler> packetListeners = Sets.newConcurrentHashSet();

    private final ChatColor PRIMARY = ChatColor.AQUA;
    private final ChatColor VALUE = ChatColor.WHITE;

    private final AetherConfig aetherConfig;
    private final PlayerDataManager playerDataManager;
    private final KnockbackSync sync;

    private final KnockbackHandler knockbackHandler;
    private final Knockback knockback;
    private final IKnockback knockbackAPI;
    private final YamlConfig knockbackConfig;

    public AetherSpigot() {
        instance = this;

        this.aetherConfig = new AetherConfig();
        bowBoost = new BowBoost();
        playerDataManager = new PlayerDataManager();
        sync = new KnockbackSync();

        knockbackConfig = new YamlConfig("knockback.yml");
        knockbackHandler = new KnockbackHandler();
        knockback = new Knockback(knockbackHandler);
        knockbackAPI = knockback;
        KnockbackAPI.registerDelegate(knockbackAPI);

        if (sync.isToggled())
            this.registerPacketListener(new PingReceiveListener());

        // Start the ping timer after everything is initialized
        if (sync.isToggled()) {
            sync.startPingTimer();
        }

        System.out.println("[AetherSpigot] Successfully Loaded!");
    }

    public void shutdown() {
        if (sync.isToggled())
            this.unregisterPacketListener(new PingReceiveListener());
        System.out.println("[AetherSpigot] Shutting down...");
    }

    public void registerCommands() {
        Map<String, Command> commands = new HashMap<>();

        commands.put("tps", new TPSCommand());
        commands.put("ping", new PingCommand());
        commands.put("kb", new KnockbackCommand());

        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            MinecraftServer.getServer().server.getCommandMap().register(entry.getKey(), "Spigot", entry.getValue());
        }
    }

    public void registerPacketListener(PacketHandler packetListener) {
        this.packetListeners.add(packetListener);
    }

    public void unregisterPacketListener(PacketHandler packetListener) {
        this.packetListeners.remove(packetListener);
    }

    public static AetherSpigot get() {
        return instance == null ? new AetherSpigot() : instance;
    }
}
