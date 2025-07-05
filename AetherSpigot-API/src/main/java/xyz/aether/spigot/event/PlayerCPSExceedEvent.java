package xyz.aether.spigot.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerCPSExceedEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final int cps;
    private boolean cancelled;

    public PlayerCPSExceedEvent(Player player, int cps) {
        this.player = player;
        this.cps = cps;
        this.cancelled = false;
    }

    public Player getPlayer() {
        return player;
    }

    public int getCps() {
        return cps;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
