package xyz.aether.spigot.config;

import com.google.common.base.Throwables;
import net.minecraft.server.RegionFile;
import org.bukkit.command.Command;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AetherConfig {
    private final static Logger LOGGER = Logger.getLogger("AetherSpigot");
    private static File CONFIG_FILE;
    private static final String HEADER = "This is the main configuration file for AetherSpigot.\n" + "As you can see, there's tons to configure. Some options may impact gameplay, so use\n" + "with caution, and make sure you know what each option does before configuring.\n";
    /*========================================================================*/
    public static YamlConfiguration config;
    static int version;
    static Map<String, Command> commands;
    /*========================================================================*/

    public static void init(File configFile) {
        CONFIG_FILE = configFile;
        config = new YamlConfiguration();
        try {
            System.out.println("Loading AetherSpigot config from " + configFile.getName());
            config.load(CONFIG_FILE);
        } catch (IOException ex) {
        } catch (InvalidConfigurationException ex) {
            LOGGER.log(Level.SEVERE, "Could not load aether.yml, please correct your syntax errors", ex);
            throw Throwables.propagate(ex);
        }
        config.options().header(HEADER);
        config.options().copyDefaults(true);

        commands = new HashMap<>();

        version = getInt("config-version", 1);
        set("config-version", 1);
        readConfig(AetherConfig.class, null);
    }

    static void readConfig(Class<?> clazz, Object instance) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (Modifier.isPrivate(method.getModifiers())) {
                if (method.getParameterTypes().length == 0 && method.getReturnType() == Void.TYPE) {
                    try {
                        method.setAccessible(true);
                        method.invoke(instance);
                    } catch (InvocationTargetException ex) {
                        throw Throwables.propagate(ex.getCause());
                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, "Error invoking " + method, ex);
                    }
                }
            }
        }

        try {
            config.save(CONFIG_FILE);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Could not save " + CONFIG_FILE, ex);
        }
    }

    private static void set(String path, Object val) {
        config.set(path, val);
    }

    private static <T> T getAndRemove(String path, T t) {
        Object obj = config.get(path, t);
        // Let's assume it's not what we're expecting
        if (t != null && obj instanceof MemorySection)
            return t;
        config.addDefault(path, null);
        config.set(path, null);
        return (T) obj;
    }

    private static boolean getBoolean(String path, boolean def) {
        config.addDefault(path, def);
        return config.getBoolean(path, config.getBoolean(path));
    }

    private static double getDouble(String path, double def) {
        config.addDefault(path, def);
        return config.getDouble(path, config.getDouble(path));
    }

    private static float getFloat(String path, float def) {
        config.addDefault(path, def);
        return config.getFloat(path, config.getFloat(path));
    }

    private static int getInt(String path, int def) {
        config.addDefault(path, def);
        return config.getInt(path, config.getInt(path));
    }

    private int getInterval(String path, int def) {
        return Math.max(getInt(path, def), 0) + 1;
    }

    private static <T> List getList(String path, T def) {
        config.addDefault(path, def);
        return (List<T>) config.getList(path, config.getList(path));
    }

    private static String getString(String path, String def) {
        config.addDefault(path, def);
        return config.getString(path, config.getString(path));
    }

    public static RegionFile.CompressionAlgorithm regionCompressionAlgorithm;

    private static void RegionCompressionAlgorithm() {
        try {
            regionCompressionAlgorithm = RegionFile.CompressionAlgorithm.valueOf(getString("SETTINGS.region-compression-algo", "ZSTD").toUpperCase());
        } catch (IllegalArgumentException e) {
            regionCompressionAlgorithm = RegionFile.CompressionAlgorithm.ZLIB;
        }
    }

    public static int regionFileCacheSize;

    private static void RegionFileCacheSize() {
        regionFileCacheSize = getInt("SETTINGS.region-file-cache-size", 256);
    }

    public static boolean blockPlaceDelay;

    private static void BlockPlaceDelay() {
        blockPlaceDelay = getBoolean("SETTINGS.block-place-delay", true);
    }

    public static boolean movedTooQuickly;
    public static void MovedTooQuickly() {
        movedTooQuickly = getBoolean("MOVEMENT.moved-too-quickly", false);
    }

    public static boolean isKnockbackSyncEnabled;

    private static void IsKnockbackSyncEnabled() {
        isKnockbackSyncEnabled = getBoolean("COMBAT.knockback-sync", true);
    }

    public static int spikeThreshold;

    private static void SpikeThreshold() {
        spikeThreshold = getInt("COMBAT.spike-threshold", 25);
    }

    public static boolean isRunnableEnabled;
    public static int runnableInterval;
    public static int combatTimer;

    private static void IsRunnableEnabled() {
        isRunnableEnabled = getBoolean("COMBAT.runnable", true);
        runnableInterval = getInt("COMBAT.runnable-interval", 5);
        combatTimer = getInt("COMBAT.combat-timer", 30);
    }

    public static boolean firePlayerMoveEvent;

    private static void FirePlayerMoveEvent() {
        firePlayerMoveEvent = getBoolean("SETTINGS.fire-move-event", true);
    }

    public static boolean isBetterSlowdown;

    private static void BetterSlowdown() {
        getBoolean("MOVEMENT.better-slowdown.enabled", false);
    }

    public static List<String> motd;

    private static void MOTD() {
        motd = getList("SERVER.motd", Arrays.asList(
                "&bWelcome to Arcuno Network!",
                "&fReleasing on June 1st."
        ));
    }
}