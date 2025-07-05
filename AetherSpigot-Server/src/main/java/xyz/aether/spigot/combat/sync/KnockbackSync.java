package xyz.aether.spigot.combat.sync;

import lombok.Getter;
import xyz.aether.spigot.combat.runnable.PingRunnable;
import xyz.aether.spigot.config.AetherConfig;

import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Getter
public class KnockbackSync {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private Timer pingTimer;

    private final boolean toggled;
    private final boolean runnable;

    private final long runnableInterval;
    private final long combatTimer;
    private final long spikeThreshold;

    public KnockbackSync() {
        toggled = AetherConfig.isKnockbackSyncEnabled;
        runnable = AetherConfig.isRunnableEnabled;
        runnableInterval = AetherConfig.runnableInterval;
        combatTimer = AetherConfig.combatTimer;
        spikeThreshold = AetherConfig.spikeThreshold;
    }

    public void startPingTimer() {
        if (runnable) {
            pingTimer = new Timer("PingRunnable", true);
            pingTimer.scheduleAtFixedRate(new PingRunnable(), 1000L, runnableInterval * 50L);
        }
    }
}
