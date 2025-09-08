package com.strikesdev.customitems.listeners;

import com.strikesdev.customitems.CustomItems;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.inventory.ItemStack;
import com.strikesdev.customitems.models.CustomItem;


// Projectile Listener for custom projectiles
public class ProjectileListener implements Listener {
    private final CustomItems plugin;

    public ProjectileListener(CustomItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        // Handle custom projectile impacts
        if (event.getEntity().hasMetadata("custom_item")) {
            String itemType = event.getEntity().getMetadata("custom_item").get(0).asString();

            switch (itemType) {
                case "fireball":
                    handleFireballHit(event);
                    break;
                case "dynamite":
                    handleDynamiteHit(event);
                    break;
                case "slowness_snowball":
                    handleSlownessSnowballHit(event);
                    break;
                case "swap_ball":
                    handleSwapBallHit(event);
                    break;
                case "homing_dart":
                    handleHomingDartHit(event);
                    break;
                case "super_wind_charge":
                    handleSuperWindChargeHit(event);
                    break;
            }
        }
    }

    private void handleFireballHit(ProjectileHitEvent event) {
        // Create explosion without fire
        double damage = 5.0;
        if (event.getEntity().hasMetadata("damage")) {
            damage = event.getEntity().getMetadata("damage").get(0).asDouble();
        }

        Player owner = null;
        if (event.getEntity().getShooter() instanceof Player) {
            owner = (Player) event.getEntity().getShooter();
        }

        // Create explosion that doesn't damage the owner
        Location loc = event.getEntity().getLocation();
        loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(),
                (float) damage, false, false, owner);
    }

    private void handleDynamiteHit(ProjectileHitEvent event) {
        // Create explosion with block damage
        double damage = 4.0;
        if (event.getEntity().hasMetadata("damage")) {
            damage = event.getEntity().getMetadata("damage").get(0).asDouble();
        }

        Player owner = null;
        if (event.getEntity().getShooter() instanceof Player) {
            owner = (Player) event.getEntity().getShooter();
        }

        // Create explosion that doesn't damage the owner
        Location loc = event.getEntity().getLocation();
        loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(),
                (float) damage, false, true, owner);
    }

    private void handleSlownessSnowballHit(ProjectileHitEvent event) {
        if (event.getHitEntity() instanceof Player) {
            Player hitPlayer = (Player) event.getHitEntity();
            hitPlayer.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOW,
                    60, // 3 seconds
                    1   // Slowness II
            ));

            // Visual feedback
            hitPlayer.spawnParticle(Particle.SNOWFLAKE, hitPlayer.getLocation().add(0, 1, 0), 15, 0.5, 1, 0.5, 0.1);
            hitPlayer.playSound(hitPlayer.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.2f);
        }
    }

    private void handleSwapBallHit(ProjectileHitEvent event) {
        if (event.getHitEntity() instanceof Player && event.getEntity().getShooter() instanceof Player) {
            Player shooter = (Player) event.getEntity().getShooter();
            Player target = (Player) event.getHitEntity();

            // Successful hit - consume the item now
            if (event.getEntity().hasMetadata("item_slot") && event.getEntity().hasMetadata("original_item")) {
                String slotName = event.getEntity().getMetadata("item_slot").get(0).asString();
                org.bukkit.inventory.EquipmentSlot slot = org.bukkit.inventory.EquipmentSlot.valueOf(slotName);

                ItemStack originalItem = (ItemStack) event.getEntity().getMetadata("original_item").get(0).value();
                ItemStack currentItem = (slot == org.bukkit.inventory.EquipmentSlot.HAND) ?
                        shooter.getInventory().getItemInMainHand() : shooter.getInventory().getItemInOffHand();

                // Consume the item
                if (currentItem.isSimilar(originalItem)) {
                    if (currentItem.getAmount() > 1) {
                        currentItem.setAmount(currentItem.getAmount() - 1);
                    } else {
                        if (slot == org.bukkit.inventory.EquipmentSlot.HAND) {
                            shooter.getInventory().setItemInMainHand(null);
                        } else {
                            shooter.getInventory().setItemInOffHand(null);
                        }
                    }
                }
            }

            // Swap positions
            Location shooterLoc = shooter.getLocation().clone();
            Location targetLoc = target.getLocation().clone();

            shooter.teleport(targetLoc);
            target.teleport(shooterLoc);

            // Effects
            shooter.playSound(shooter.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            target.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        }
        // If no player hit, item is not consumed (stays in inventory)
    }



    private void handleHomingDartHit(ProjectileHitEvent event) {
        if (event.getHitEntity() instanceof Player) {
            Player hitPlayer = (Player) event.getHitEntity();

            // Only damage if it can still damage
            if (event.getEntity().hasMetadata("can_damage") &&
                    event.getEntity().getMetadata("can_damage").get(0).asBoolean()) {

                double damage = 6.0;
                if (event.getEntity().hasMetadata("damage")) {
                    damage = event.getEntity().getMetadata("damage").get(0).asDouble();
                }

                hitPlayer.damage(damage);
                event.getEntity().remove(); // Remove arrow after hit
            }
        } else {
            // Hit block, remove arrow
            event.getEntity().remove();
        }
    }


    private void handleEggBridgerHit(ProjectileHitEvent event) {
        // Generate bridge blocks behind the egg's flight path
        if (event.getEntity().hasMetadata("bridge_blocks")) {
            @SuppressWarnings("unchecked")
            java.util.List<Location> blocks =
                    (java.util.List<Location>) event.getEntity().getMetadata("bridge_blocks").get(0).value();

            for (Location loc : blocks) {
                if (loc.getBlock().getType() == Material.AIR) {
                    loc.getBlock().setType(Material.COBBLESTONE);
                    plugin.getEffectManager().registerTemporaryBlock(loc, 30); // 30 second bridge
                }
            }
        }
    }

    private void handleSuperWindChargeHit(ProjectileHitEvent event) {
        // Get configurable values from metadata (set by the action)
        double range = 8.0;
        double launchPower = 4.5;
        double upwardForce = 0.6;

        if (event.getEntity().hasMetadata("range")) {
            range = event.getEntity().getMetadata("range").get(0).asDouble();
        }
        if (event.getEntity().hasMetadata("launch_power")) {
            launchPower = event.getEntity().getMetadata("launch_power").get(0).asDouble();
        }
        if (event.getEntity().hasMetadata("upward_force")) {
            upwardForce = event.getEntity().getMetadata("upward_force").get(0).asDouble();
        }

        Location impactLoc = event.getEntity().getLocation();

        // Find all players near the impact location
        for (Entity entity : impactLoc.getWorld().getNearbyEntities(impactLoc, range, range, range)) {
            if (!(entity instanceof Player)) continue;
            Player nearbyPlayer = (Player) entity;

            // Calculate direction from impact point to each nearby player
            Vector direction = nearbyPlayer.getLocation().toVector()
                    .subtract(impactLoc.toVector()).normalize();
            direction.setY(Math.max(direction.getY(), upwardForce));
            direction.multiply(launchPower);

            // Launch the nearby player
            nearbyPlayer.setVelocity(direction);

            // Effects on the launched player
            nearbyPlayer.spawnParticle(Particle.GUST, nearbyPlayer.getLocation(), 25, 1, 1, 1, 0.2);
            nearbyPlayer.spawnParticle(Particle.EXPLOSION_LARGE, nearbyPlayer.getLocation(), 2);
        }

        // Impact effects
        impactLoc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, impactLoc, 3);
        impactLoc.getWorld().spawnParticle(Particle.GUST, impactLoc, 50, 5, 5, 5, 0.5);
        impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
        impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 0.5f);
    }
}