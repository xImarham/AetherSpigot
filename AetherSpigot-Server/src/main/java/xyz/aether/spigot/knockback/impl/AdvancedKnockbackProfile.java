package xyz.aether.spigot.knockback.impl;

import net.minecraft.server.Entity;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.MathHelper;
import net.minecraft.server.PacketPlayOutEntityVelocity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;
import xyz.aether.spigot.combat.listener.DamageListener;
import xyz.aether.spigot.knockback.KnockbackModifier;
import xyz.aether.spigot.knockback.KnockbackProfile;

import java.util.Arrays;
import java.util.List;

public class AdvancedKnockbackProfile extends KnockbackProfile {

    public AdvancedKnockbackProfile(String name) {
        super(name);
    }

    @Override
    public List<KnockbackModifier<?>> getDefaultModifiers() {
        return Arrays.asList(
            new KnockbackModifier<>(double.class, "horizontal", 0.9055),
            new KnockbackModifier<>(double.class, "vertical", 0.8835),
            new KnockbackModifier<>(double.class, "vertical-min", 0.36),
            new KnockbackModifier<>(double.class, "vertical-friction", 0.2),
            new KnockbackModifier<>(double.class, "start-range", 3.0),
            new KnockbackModifier<>(boolean.class, "zero-friction", true),
            new KnockbackModifier<>(double.class, "max-range", 0.4),
            new KnockbackModifier<>(double.class, "min-friction", 0.1111),
            new KnockbackModifier<>(double.class, "max-friction", 0.2222),
            new KnockbackModifier<>(double.class, "range-factor", 0.2),
            new KnockbackModifier<>(Integer.class, "no-damage-ticks", 20),
            new KnockbackModifier<>(double.class, "extra-horizontal", 0.4),
            new KnockbackModifier<>(double.class, "extra-vertical", 0.4),
            new KnockbackModifier<>(double.class, "vertical-limit", 0.3534),
            new KnockbackModifier<>(double.class, "arrow-horizontal", 0.4),
            new KnockbackModifier<>(double.class, "arrow-vertical", 0.4),
            new KnockbackModifier<>(double.class, "egg-horizontal", 0.4),
            new KnockbackModifier<>(double.class, "egg-vertical", 0.4),
            new KnockbackModifier<>(double.class, "pearl-horizontal", 0.4),
            new KnockbackModifier<>(double.class, "pearl-vertical", 0.4),
            new KnockbackModifier<>(double.class, "snowball-horizontal", 0.4),
            new KnockbackModifier<>(double.class, "snowball-vertical", 0.4),
            new KnockbackModifier<>(double.class, "rod-horizontal", 0.4),
            new KnockbackModifier<>(double.class, "rod-vertical", 0.4)
        );
    }

    @Override
    public String getImplementationName() {
        return "advanced";
    }

    public double horizontalDistance(EntityPlayer victim, Entity source) {
        double d0 = victim.locX - source.locX;
        double d2 = victim.locZ - source.locZ;

        return Math.hypot(d0, d2);
    }

    private double friction(double range) {
        double startRange = (double) this.getKnockbackModifier("start-range", false).getValue();
        double minFriction = (double) this.getKnockbackModifier("min-friction", false).getValue();
        double maxFriction = (double) this.getKnockbackModifier("max-friction", false).getValue();
        double t = (range - startRange) / (maxFriction - startRange);
        t = Math.max(0.0, Math.min(t, 1.0));

        double f = (maxFriction - minFriction) / 4.0f;

        if (t < 0.25) {
            return minFriction;
        } else if (t < 0.5) {
            return minFriction + f;
        } else if (t < 0.75) {
            return minFriction + (2 * f);
        } else {
            return maxFriction;
        }
    }

    @Override
    public void handleEntityLiving(EntityPlayer victim, Entity source, float f, double d0, double d1) {
        double magnitude = Math.hypot(d0, d1);
        double horizontal = (double) this.getKnockbackModifier("horizontal", false).getValue();
        double vertical = (double) this.getKnockbackModifier("vertical", false).getValue();
        double verticalLimit = (double) this.getKnockbackModifier("vertical-limit", false).getValue();
        double verticalMin = (double) this.getKnockbackModifier("vertical-min", false).getValue();
        double verticalFriction = (double) this.getKnockbackModifier("vertical-friction", false).getValue();
        double friction = this.friction(horizontalDistance(victim, source));

        if ((boolean) getKnockbackModifier("zero-friction", false).getValue()) {
            victim.motX = 0;
            victim.motY = verticalFriction;
            victim.motZ = 0;
        } else {
            victim.motX *= friction;
            victim.motY *= verticalFriction;
            victim.motZ *= friction;
        }

        victim.motX -= d0 / magnitude * horizontal;
        victim.motY += vertical;
        victim.motZ -= d1 / magnitude * horizontal;

        DamageListener.applyKnockback(victim);

        if (victim.motY > verticalLimit) {
            victim.motY = verticalLimit;
        }

        if (victim.motY < verticalMin) {
            victim.motY = verticalMin;
        }
    }

    @Override
    public void handleEntityHuman(EntityPlayer victim, Entity source, int i, Vector vector) {
        if (i > 0) {
            double extraHorizontal = (double) this.getKnockbackModifier("extra-horizontal", false).getValue();
            double extraVertical = (double) this.getKnockbackModifier("extra-vertical", false).getValue();
            double startRange = (double) this.getKnockbackModifier("start-range", false).getValue();
            double maxRange = (double) this.getKnockbackModifier("max-range", false).getValue();
            double rangeFactor = (double) this.getKnockbackModifier("range-factor", false).getValue();
            double range = this.horizontalDistance(victim, source);
            double rangeReduction = Math.min(Math.max((range - startRange) *
                    rangeFactor, 0),
                maxRange);
            source.g(-MathHelper.sin(victim.yaw * (float) Math.PI / 180.0F) * i * (extraHorizontal - rangeReduction),
                extraVertical,
                MathHelper.cos(victim.yaw * (float) Math.PI / 180.0F) * i * (extraHorizontal - rangeReduction));

            victim.setSprinting(false);
            victim.setLastSprintStatus(false);
        }

        if (source instanceof EntityPlayer && source.velocityChanged) {
            // CraftBukkit start - Add Velocity Event
            boolean cancelled = false;
            Player player = (Player) source.getBukkitEntity();
            org.bukkit.util.Vector velocity = new Vector(vector.getX(), vector.getY(), vector.getZ());

            PlayerVelocityEvent event = new PlayerVelocityEvent(player, velocity.clone());
            victim.world.getServer().getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                cancelled = true;
            } else if (!velocity.equals(event.getVelocity())) {
                player.setVelocity(event.getVelocity());
            }

            if (!cancelled) {
                ((EntityPlayer) source).playerConnection.sendPacket(new PacketPlayOutEntityVelocity(source));
                source.velocityChanged = false;
                source.motX = vector.getX();
                source.motY = vector.getY();
                source.motZ = vector.getZ();
            }
            // CraftBukkit end
        }
    }

    @Override
    public int getDamageTicks() {
        return (int) getKnockbackModifier("no-damage-ticks", false).getValue();
    }

    @Override
    public double getArrowHorizontal() {
        return (double) getKnockbackModifier("arrow-horizontal", false).getValue();
    }

    @Override
    public double getArrowVertical() {
        return (double) getKnockbackModifier("arrow-vertical", false).getValue();
    }

    @Override
    public double getEggHorizontal() {
        return (double) getKnockbackModifier("egg-horizontal", false).getValue();
    }

    @Override
    public double getEggVertical() {
        return (double) getKnockbackModifier("egg-vertical", false).getValue();
    }

    @Override
    public double getPearlHorizontal() {
        return (double) getKnockbackModifier("pearl-horizontal", false).getValue();
    }

    @Override
    public double getPearlVertical() {
        return (double) getKnockbackModifier("pearl-vertical", false).getValue();
    }

    @Override
    public double getRodHorizontal() {
        return (double) getKnockbackModifier("rod-horizontal", false).getValue();
    }

    @Override
    public double getRodVertical() {
        return (double) getKnockbackModifier("rod-vertical", false).getValue();
    }

    @Override
    public double getSnowballHorizontal() {
        return (double) getKnockbackModifier("snowball-horizontal", false).getValue();
    }

    @Override
    public double getSnowballVertical() {
        return (double) getKnockbackModifier("snowball-vertical", false).getValue();
    }
}
