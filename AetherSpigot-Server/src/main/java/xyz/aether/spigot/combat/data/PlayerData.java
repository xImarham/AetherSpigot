package xyz.aether.spigot.combat.data;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.GenericAttributes;
import net.minecraft.server.PacketPlayOutKeepAlive;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.aether.spigot.AetherSpigot;
import xyz.aether.spigot.combat.util.MathUtil;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
public class PlayerData {
    private byte lastUsefulBitmask;
    private double lastSpeed;
    private boolean clientSprintingState;

    private EntityPlayer player;

    private ScheduledFuture<?> combatTask;

    private static final long PING_OFFSET = 25;

    @NotNull
    private final ConcurrentHashMap<Integer, Long> timeline = new ConcurrentHashMap<>();

    @NotNull
    private final Random random = new Random();

    @Nullable
    @Setter
    private Long ping, previousPing;

    @Nullable
    @Setter
    private Double verticalVelocity;

    public PlayerData() {
        this.reset();
    }

    public long getEstimatedPing() {
        long currentPing = (ping != null) ? ping : player.ping;
        long lastPing = (previousPing != null) ? previousPing : player.ping;
        long ping = (currentPing - lastPing > AetherSpigot.get().getSync().getSpikeThreshold()) ? lastPing : currentPing;

        return Math.max(1, ping - PING_OFFSET);
    }

    public void sendPing() {
        if (player == null) {
            return;
        }

        int packetId = 1 + random.nextInt(9999);

        timeline.put(packetId, System.currentTimeMillis());

        PacketPlayOutKeepAlive packet = new PacketPlayOutKeepAlive(packetId);
        player.playerConnection.sendPacket(packet);
    }

    /**
     * Determines if the Player is on the ground, either serverside or clientside.
     * <p>
     * Returns <code>true</code> if:
     * <ul>
     *   <li>Clientside: <code>ping ≥ (tMax + tFall)</code> and <code>gDist ≤ 1.3</code></li>
     *   <li>Serverside: <code>gDist ≤ 0</code></li>
     * </ul>
     * <p>
     * Where:
     * <ul>
     *   <li><code>ping</code>: Estimated latency</li>
     *   <li><code>tMax</code>: Time to reach maximum upward velocity</li>
     *   <li><code>tFall</code>: Time to fall to the ground</li>
     *   <li><code>gDist</code>: Distance to the ground</li>
     * </ul>
     *
     * @param verticalVelocity The Player's current vertical velocity.
     * @return <code>true</code> if the Player is on the ground; <code>false</code> otherwise.
     */
    public boolean isOnGround(double verticalVelocity) {
        Material material = player.getBukkitEntity().getLocation().getBlock().getType();
        if (material == Material.WATER || material == Material.LAVA)
            return false;

        if (ping == null || ping < PING_OFFSET)
            return false;

        double gDist = getDistanceToGround();
        if (gDist <= 0)
            return true;

        int tMax = verticalVelocity > 0 ? MathUtil.calculateTimeToMaxVelocity(verticalVelocity) : 0;
        double mH = verticalVelocity > 0 ? MathUtil.calculateDistanceTraveled(verticalVelocity, tMax) : 0;
        int tFall = MathUtil.calculateFallTime(verticalVelocity, mH + gDist);

        return getEstimatedPing() >= tMax + tFall / 20.0 * 1000 && gDist <= 1.3;
    }

    /**
     * Gets the corners of the player's bounding box for ground distance calculation
     *
     * @return List of corner positions as Vec3D
     */
    private java.util.List<net.minecraft.server.Vec3D> getBBCorners() {
        net.minecraft.server.AxisAlignedBB boundingBox = player.getBoundingBox();
        double width = 0.3;
        double x = player.locX;
        double y = boundingBox.b;
        double z = player.locZ;

        return java.util.Arrays.asList(
                new net.minecraft.server.Vec3D(x + width, y, z + width),
                new net.minecraft.server.Vec3D(x + width, y, z - width),
                new net.minecraft.server.Vec3D(x - width, y, z + width),
                new net.minecraft.server.Vec3D(x - width, y, z - width)
        );
    }

    /**
     * Ray traces from each corner of the player's bounding box to the ground,
     * returning the smallest distance, with a maximum limit of 5 blocks.
     *
     * @return The distance to the ground in blocks
     */
    public double getDistanceToGround() {
        double collisionDist = 5;
        net.minecraft.server.World nmsWorld = player.world;

        for (net.minecraft.server.Vec3D corner : getBBCorners()) {
            net.minecraft.server.Vec3D direction = new net.minecraft.server.Vec3D(0, -5, 0);
            net.minecraft.server.Vec3D target = corner.add(direction.a, direction.b, direction.c);

            net.minecraft.server.MovingObjectPosition result = nmsWorld.rayTrace(corner, target, false, true, false);
            if (result == null || result.type != net.minecraft.server.MovingObjectPosition.EnumMovingObjectType.BLOCK)
                continue;

            // distance from the corner to the hit position
            collisionDist = Math.min(collisionDist, corner.b - result.pos.b);
        }

        return collisionDist - 1;
    }

    public double calculateVerticalVelocity(Player attacker) {
        double yAxis = 0.36080000519752503;

        if (!attacker.isSprinting()) {
            yAxis = 0.36080000519752503;
            double knockbackResistance = player.getAttributeInstance(GenericAttributes.c).getValue();
            double resistanceFactor = 0.04000000119 * knockbackResistance * 10;
            yAxis -= resistanceFactor;
        }

        return yAxis;
    }

    public boolean isInCombat() {
        return combatTask != null && !combatTask.isCancelled();
    }

    public void updateCombat() {
        if (isInCombat()) {
            combatTask.cancel(false);
        }

        combatTask = newCombatTask();
    }

    public void quitCombat(boolean cancelTask) {
        if (cancelTask && combatTask != null) {
            combatTask.cancel(false);
        }

        combatTask = null;
        timeline.clear();
    }

    @NotNull
    private ScheduledFuture<?> newCombatTask() {
        long delay = AetherSpigot.get().getSync().getCombatTimer();
        long delayMillis = delay * 50;

        return AetherSpigot.get().getSync().getScheduler().schedule(
                () -> quitCombat(false),
                delayMillis,
                TimeUnit.MILLISECONDS
        );
    }

    public void reset() {
        this.lastUsefulBitmask = (byte) -1;
        this.lastSpeed = -1.0D;
        this.clientSprintingState = false;
    }
}