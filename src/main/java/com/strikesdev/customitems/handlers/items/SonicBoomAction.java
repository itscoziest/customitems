package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

public class SonicBoomAction implements ItemAction {

    private final CustomItems plugin;

    public SonicBoomAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        double damage = item.getCustomDataDouble("sonic-damage", 10.0);
        double range = item.getCustomDataDouble("sonic-range", 30.0);

        Vector direction = player.getLocation().getDirection();
        Location startLocation = player.getEyeLocation();

        createSonicBoomEffect(startLocation, direction, range);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 1.0f);

        damageSonicBoomTargets(player, startLocation, direction, range, damage);

        consumeItem(player, event);
        return true;
    }

    private void createSonicBoomEffect(Location start, Vector direction, double range) {
        for (double i = 0; i < range; i += 0.5) {
            Location particleLoc = start.clone().add(direction.clone().multiply(i));
            double radius = i * 0.1;
            // Only spawn particles every 3rd step to reduce lag
            if (i % 1.5 == 0) {
                particleLoc.getWorld().spawnParticle(Particle.SONIC_BOOM, particleLoc, 1, 0, 0, 0, 0);
            }
        }
    }

    private void damageSonicBoomTargets(Player caster, Location start, Vector direction, double range, double damage) {
        // FIX: Optimize collision detection
        for (double i = 1; i < range; i += 1.0) {
            Location checkLoc = start.clone().add(direction.clone().multiply(i));
            // Expanding hitbox as it travels
            double radius = 1.0 + (i * 0.05);

            for (Entity entity : checkLoc.getWorld().getNearbyEntities(checkLoc, radius, radius, radius)) {
                if (entity instanceof LivingEntity && !entity.equals(caster)) {
                    LivingEntity target = (LivingEntity) entity;

                    // Prevent hitting the same entity multiple times in one blast
                    if (target.getNoDamageTicks() > 0) continue;

                    target.damage(damage, caster);

                    Vector knockback = target.getLocation().toVector()
                            .subtract(caster.getLocation().toVector())
                            .normalize()
                            .multiply(1.5);
                    knockback.setY(0.4);
                    target.setVelocity(knockback);
                }
            }
        }
    }

    private void consumeItem(Player player, PlayerInteractEvent event) {
        if (event.getItem() == null) return;
        if (event.getItem().getAmount() > 1) {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
        } else {
            player.getInventory().setItem(event.getHand(), null);
        }
    }
}