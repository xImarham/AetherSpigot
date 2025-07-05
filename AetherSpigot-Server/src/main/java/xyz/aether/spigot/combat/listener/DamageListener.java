package xyz.aether.spigot.combat.listener;

import net.minecraft.server.EntityPlayer;
import org.bukkit.entity.Player;
import xyz.aether.spigot.AetherSpigot;
import xyz.aether.spigot.combat.data.PlayerData;

public class DamageListener {

    public static void onPlayerAttack(EntityPlayer attacker, EntityPlayer victim, boolean damaged) {
        if (!AetherSpigot.get().getSync().isToggled() || !damaged) return;

        Player pAttacker = attacker.getBukkitEntity();

        PlayerData victimData = AetherSpigot.get().getPlayerDataManager().get(victim.getBukkitEntity());
        victimData.setVerticalVelocity(victimData.calculateVerticalVelocity(pAttacker));
        victimData.updateCombat();

        PlayerData attackerData = AetherSpigot.get().getPlayerDataManager().get(pAttacker);
        attackerData.updateCombat();

        if (!AetherSpigot.get().getSync().isRunnable()) {
            victimData.sendPing();
            attackerData.sendPing();
        }
    }

    public static void applyKnockback(EntityPlayer victim) {
        if (!AetherSpigot.get().getSync().isToggled()) return;
        PlayerData data = AetherSpigot.get().getPlayerDataManager().get(victim.getBukkitEntity());
        Double verticalVelocity = AetherSpigot.get().getPlayerDataManager().get(victim.getBukkitEntity()).getVerticalVelocity();

        if (verticalVelocity == null || !data.isOnGround(victim.motY)) { // intellij is weird about this, this is never true but oh well :/
            return;
        }

        victim.motY = verticalVelocity;
    }
}
