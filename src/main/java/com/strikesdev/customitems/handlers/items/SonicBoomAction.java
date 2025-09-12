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

        // Get damage from config or use default
        double damage = item.getCustomDataDouble("sonic-damage", 10.0);
        double range = item.getCustomDataDouble("sonic-range", 30.0);

        // Get player's facing direction
        Vector direction = player.getLocation().getDirection();
        Location startLocation = player.getEyeLocation();

        // Create sonic boom effect in a cone
        createSonicBoomEffect(startLocation, direction, range);

        // Play warden sonic boom sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 1.0f);

        // Damage entities in the sonic boom path
        damageSonicBoomTargets(player, startLocation, direction, range, damage);

        if (event.getItem() != null) {
            if (event.getItem().getAmount() > 1) {
                event.getItem().setAmount(event.getItem().getAmount() - 1);
            } else {
                if (player.getInventory().getItemInMainHand().equals(event.getItem())) {
                    player.getInventory().setItemInMainHand(null);
                } else if (player.getInventory().getItemInOffHand().equals(event.getItem())) {
                    player.getInventory().setItemInOffHand(null);
                }
            }
        }

        return true;
    }

    private void createSonicBoomEffect(Location start, Vector direction, double range) {
        // Create the sonic boom visual effect
        for (double i = 0; i < range; i += 0.5) {
            Location particleLoc = start.clone().add(direction.clone().multiply(i));

            // Create expanding cone effect
            double radius = i * 0.1; // Expanding cone
            for (int j = 0; j < 8; j++) {
                double angle = (j * Math.PI * 2) / 8;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                Location effectLoc = particleLoc.clone().add(x, 0, z);

                // Sonic boom particles
                effectLoc.getWorld().spawnParticle(Particle.SONIC_BOOM, effectLoc, 1, 0, 0, 0, 0);
                effectLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, effectLoc, 1, 0, 0, 0, 0);
            }
        }
    }

    private void damageSonicBoomTargets(Player caster, Location start, Vector direction, double range, double damage) {
        for (double i = 0; i < range; i += 1.0) {
            Location checkLoc = start.clone().add(direction.clone().multiply(i));
            double radius = 1.5 + (i * 0.05); // Expanding hitbox

            for (Entity entity : checkLoc.getWorld().getNearbyEntities(checkLoc, radius, radius, radius)) {
                if (entity instanceof LivingEntity && !entity.equals(caster)) {
                    LivingEntity target = (LivingEntity) entity;

                    // Apply damage
                    target.damage(damage, caster);

                    // Apply knockback effect (like warden sonic boom)
                    Vector knockback = target.getLocation().toVector()
                            .subtract(caster.getLocation().toVector())
                            .normalize()
                            .multiply(1.5);
                    knockback.setY(0.3); // Add upward force

                    target.setVelocity(knockback);

                    // Visual effect on hit
                    target.getWorld().spawnParticle(Particle.CRIT,
                            target.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                }
            }
        }
    }
}