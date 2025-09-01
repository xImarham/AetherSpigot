package xyz.aether.spigot.config;

import net.minecraft.server.World;
import org.bukkit.Bukkit;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;

public class AetherWorldConfig {

    private final String worldName;
    private final YamlConfiguration config;
    private final World world;
    private boolean verbose;

    public AetherWorldConfig(World world, String worldName) {
        this.worldName = worldName;
        this.config = AetherConfig.config;
        this.world = world;
        init();
    }

    public void init() {
        this.verbose = getBoolean("verbose", true);

        log("-------- World Settings For [" + worldName + "] --------");
        AetherConfig.readConfig(AetherWorldConfig.class, this);
    }

    private void log(String s) {
        if (false) {
            Bukkit.getLogger().info(s);
        }
    }

    private void set(String path, Object val) {
        config.set(path, val);
    }

    private <T> T getAndRemove(String path, T t) {
        Object obj = config.get(path, t);
        // Let's assume it's not what we're expecting
        if (t != null && obj instanceof MemorySection)
            return t;
        config.addDefault(path, null);
        config.set(path, null);
        return (T) obj;
    }

    private boolean getBoolean(String path, boolean def) {
        config.addDefault(path, def);
        return config.getBoolean("world-settings." + worldName + "." + path, config.getBoolean(path));
    }

    private double getDouble(String path, double def) {
        config.addDefault(path, def);
        return config.getDouble("world-settings." + worldName + "." + path, config.getDouble(path));
    }

    private int getInt(String path, int def) {
        config.addDefault(path, def);
        return config.getInt("world-settings." + worldName + "." + path, config.getInt(path));
    }

    private int getInterval(String path, int def) {
        return Math.max(getInt(path, def), 0) + 1;
    }

    private float getFloat(String path, float def) {
        config.addDefault(path, def);
        return config.getFloat("world-settings." + worldName + "." + path, config.getFloat(path));
    }

    private <T> List getList(String path, T def) {
        config.addDefault(path, def);
        return (List<T>) config.getList("world-settings." + worldName + "." + path, config.getList(path));
    }

    private String getString(String path, String def) {
        config.addDefault(path, def);
        return config.getString("world-settings." + worldName + "." + path, config.getString(path));
    }

    public boolean dropSwordWhileBlocking;

    private void DropSwordWhileBlocking() {
        dropSwordWhileBlocking = getBoolean("SETTINGS.drop-sword-while-blocking", false);
    }

    public boolean trackPlayersEveryTick;

    private void TrackPlayersEveryTick() {
        trackPlayersEveryTick = getBoolean("SETTINGS.track-players-every-tick", true);
    }

    public double rodSpeed;

    private void RodSpeed() {
        rodSpeed = getDouble("SETTINGS.rod-speed", 1.0);
    }

    public boolean releaseItemPacketFix; // the author of this is Krouda, you can dm him on discord and ask for the implementation

    private void ReleaseItemPacketFix() {
        trackPlayersEveryTick = getBoolean("SETTINGS.release-item-packet-fix", true);
    }

    public boolean eatWhileRunningBugFix;

    private void EatWhileRunningBugFix() {
        eatWhileRunningBugFix = getBoolean("SETTINGS.eat-while-running-fix", true);
    }

    public double potionGravity;

    private void PotionGravity() {
        potionGravity = getDouble("POTION.potion-gravity", 0.05);
    }

    public boolean compensatedPots;

    private void CompensatedPots() {
        compensatedPots = getBoolean("POTION.lag-compensated-potion", true);
    }

    public double potionSpeed;

    private void PotionSpeed() {
        potionSpeed = getDouble("POTION.potion-speed", 0.500);
    }

    public int potionTime;

    private void PotionTime() {
        potionTime = getInt("POTION.potion-time", 3);
    }

    public double potionOffset;

    private void PotionOffset() {
        potionOffset = getDouble("POTION.potion-offset", -18.0);
    }

    public boolean reducedSplash;

    private void ReducedSplash() {
        reducedSplash = getBoolean("POTION.reduced-splash", true);
    }

    public double pearlOffset;

    private void PearlOffset() {
        pearlOffset = getDouble("PEARL.pearl-offset", 0.0);
    }

    public double pearlSpeed;

    private void PearlSpeed() {
        pearlSpeed = getDouble("PEARL.pearl-speed", 1.5);
    }

    public double pearlGravity;

    private void PearlGravity() {
        pearlGravity = getDouble("PEARL.pearl-gravity", 0.03);
    }

    public boolean smallPearlHitBox;

    private void SmallPearlHitbox() {
        smallPearlHitBox = getBoolean("PEARL.small-hitbox", true);
    }

    public boolean compensatedPearls;

    private void CompensatedPearls() {
        compensatedPearls = getBoolean("PEARL.lag-compensated-pearl", true);
    }
}
