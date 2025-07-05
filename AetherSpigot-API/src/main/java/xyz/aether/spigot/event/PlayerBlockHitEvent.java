package xyz.aether.spigot.event;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PlayerBlockHitEvent extends EntityDamageByEntityEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;
    private float reducedDamage;

    public PlayerBlockHitEvent(Player player, Entity damager, float originalDamage, float reducedDamage) {
        super(damager, player, DamageCause.ENTITY_ATTACK, originalDamage);
        this.reducedDamage = reducedDamage;
        this.cancelled = false;
    }

    @Override
    public Player getEntity() {
        return (Player) super.getEntity();
    }

    @Override
    public Entity getDamager() {
        return super.getDamager();
    }

    public float getOriginalDamage() {
        return (float) getDamage();
    }

    public float getReducedDamage() {
        return reducedDamage;
    }

    public void setReducedDamage(float reducedDamage) {
        if (reducedDamage < 0) {
            throw new IllegalArgumentException("Reduced damage cannot be negative"); // who knows what i do with negative damage
        }
        this.reducedDamage = reducedDamage;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}