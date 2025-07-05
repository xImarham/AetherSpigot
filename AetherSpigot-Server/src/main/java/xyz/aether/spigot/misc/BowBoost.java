package xyz.aether.spigot.misc;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

/*
 * https://github.com/diamond-rip/BowBooster/blob/master/src/main/java/me/goodestenglish/bowbooster/listener/BowBoostListener.java
*/
public class BowBoost {

    public void onShoot(EntityShootBowEvent event) {
        LivingEntity entity = event.getEntity();
        Vector direction = entity.getLocation().getDirection();
        Arrow arrow = (Arrow) event.getProjectile();
        double speed = arrow.getVelocity().length();
        Vector velocity = direction.multiply(speed);
        arrow.setVelocity(velocity);
    }

    public void onVelocity(PlayerVelocityEvent event) {
        Player player = event.getPlayer();
        Vector velocity = event.getVelocity();
        EntityDamageEvent lastDamage = player.getLastDamageCause();

        if (lastDamage instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) lastDamage;
            Entity damager = damageEvent.getDamager();

            if (damager instanceof Arrow) {
                Arrow arrow = (Arrow) damager;

                if (arrow.getShooter() != null && arrow.getShooter().equals(player)) {
                    double speed = Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ());
                    Vector direction = arrow.getLocation().getDirection().normalize();

                    double xMultiplier = 1.2;
                    double yMultiplier = 1.0;
                    double zMultiplier = 1.2;

                    Vector newVelocity = new Vector(
                            (direction.getX() * speed * -1.0) * xMultiplier,
                            velocity.getY() * yMultiplier,
                            direction.getZ() * speed * zMultiplier
                    );

                    event.setVelocity(newVelocity);
                }
            }
        }
    }
}
