package com.strikesdev.customitems.listeners;

import com.strikesdev.customitems.CustomItems;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.ChatColor;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.metadata.FixedMetadataValue;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Fireball;
import org.bukkit.event.entity.EntitySpawnEvent;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import java.util.UUID;

public class ProjectileListener implements Listener {
    private final CustomItems plugin;
    private final Set<Location> dragonBreathImpacts = new HashSet<>();

    public ProjectileListener(CustomItems plugin) {
        this.plugin = plugin;
    }



    private void onDragonBreathImpact(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball)) return;

        Snowball projectile = (Snowball) event.getEntity();
        if (!projectile.hasMetadata("dragon_breath_projectile")) return;

        projectile.remove();

        String casterName = projectile.getMetadata("caster_name").get(0).asString();
        Player caster = plugin.getServer().getPlayer(casterName);
        if (caster == null || !caster.isOnline()) return;

        double damagePerTick = projectile.getMetadata("config_damage").get(0).asDouble();
        int totalDurationTicks = projectile.getMetadata("config_duration").get(0).asInt();

        Location breathCenter = projectile.getLocation();
        double effectRadius = 4.0;

        breathCenter.getWorld().playSound(breathCenter, Sound.ENTITY_ENDER_DRAGON_HURT, 2.0f, 0.5f);
        breathCenter.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, breathCenter, 3, 1, 1, 1, 0);

        new BukkitRunnable() {
            private int currentTick = 0;

            @Override
            public void run() {
                if (currentTick >= totalDurationTicks) {
                    breathCenter.getWorld().spawnParticle(Particle.DRAGON_BREATH, breathCenter, 150, effectRadius, 1, effectRadius, 0.3);
                    breathCenter.getWorld().playSound(breathCenter, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.5f);
                    this.cancel();
                    return;
                }

                breathCenter.getWorld().spawnParticle(Particle.DRAGON_BREATH, breathCenter, 50, effectRadius * 0.8, 0.8, effectRadius * 0.8, 0.1);

                if (currentTick % 20 == 0) {
                    for (Entity nearbyEntity : breathCenter.getWorld().getNearbyEntities(breathCenter, effectRadius, effectRadius, effectRadius)) {
                        if (nearbyEntity instanceof Player && !nearbyEntity.equals(caster)) {
                            Player victim = (Player) nearbyEntity;

                            double currentHealth = victim.getHealth();
                            double newHealth = Math.max(0, currentHealth - damagePerTick);
                            victim.setHealth(newHealth);

                            victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                                    victim.getLocation().add(0, 1.5, 0), 8, 0.4, 0.4, 0.4, 0.2);
                            victim.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.8f, 1.5f);

                            if (newHealth <= 0) {
                                victim.setLastDamageCause(new org.bukkit.event.entity.EntityDamageByEntityEvent(caster, victim, org.bukkit.event.entity.EntityDamageEvent.DamageCause.MAGIC, damagePerTick));
                                victim.damage(0.1, caster);
                            }
                        }
                    }
                }

                currentTick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();

        // Dragon breath
        if (projectile instanceof Snowball && projectile.hasMetadata("dragon_breath_projectile")) {
            onDragonBreathImpact(event);
            return;
        }

        // Arrows
        if (projectile instanceof Arrow) {
            Arrow arrow = (Arrow) projectile;

            if (arrow.hasMetadata("explosive_arrow")) {
                double explosionPower = arrow.getMetadata("explosion_power").get(0).asDouble();
                Location loc = arrow.getLocation();
                loc.getWorld().createExplosion(loc, (float) explosionPower, false, false);
                loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 1);
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                arrow.remove();
                return;
            }
        }

        // Ender pearls
        if (projectile instanceof EnderPearl) {
            EnderPearl pearl = (EnderPearl) projectile;

            if (pearl.hasMetadata("custom_item") && "swap_ball".equals(pearl.getMetadata("custom_item").get(0).asString())) {
                event.setCancelled(true);
                pearl.remove();

                if (event.getHitEntity() instanceof Player) {
                    Player target = (Player) event.getHitEntity();
                    if (!pearl.hasMetadata("caster_uuid")) return;
                    String casterUUID = pearl.getMetadata("caster_uuid").get(0).asString();
                    Player caster = Bukkit.getPlayer(UUID.fromString(casterUUID));

                    if (caster == null || !caster.isOnline() || caster.equals(target)) {
                        return;
                    }

                    Location casterLoc = caster.getLocation().clone();
                    Location targetLoc = target.getLocation().clone();
                    caster.teleport(targetLoc);
                    target.teleport(casterLoc);

                    caster.playSound(caster.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    target.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                }
                return;
            }
        }
    }

    private void handleExplosiveArrowHit(ProjectileHitEvent event) {
        Arrow arrow = (Arrow) event.getEntity();

        // Get explosion power
        double explosionPower = arrow.getMetadata("explosion_power").get(0).asDouble();

        // Create explosion at arrow location
        Location loc = arrow.getLocation();
        loc.getWorld().createExplosion(loc, (float) explosionPower, false, false);

        // Add visual effects
        loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 1);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        // Remove the arrow
        arrow.remove();
    }

    private void handleLassoHit(ProjectileHitEvent event) {
        Arrow arrow = (Arrow) event.getEntity();

        // Get the caster
        String casterUUID = arrow.getMetadata("lasso_caster").get(0).asString();
        Player caster = Bukkit.getPlayer(UUID.fromString(casterUUID));

        if (caster == null) {
            arrow.remove();
            return;
        }

        // Check if we hit a player
        if (event.getHitEntity() instanceof Player) {
            Player target = (Player) event.getHitEntity();

            if (target.equals(caster)) {
                arrow.remove();
                return; // Don't lasso yourself
            }

            // Get lasso settings
            double basePullStrength = arrow.getMetadata("pull_strength").get(0).asDouble();
            double maxRange = arrow.getMetadata("max_range").get(0).asDouble();
            boolean distanceBased = arrow.getMetadata("distance_based").get(0).asBoolean();

            // Calculate distance
            double distance = caster.getLocation().distance(target.getLocation());

            if (distance > maxRange) {
                caster.sendMessage(ChatColor.RED + "Target is too far away!");
                arrow.remove();
                return;
            }

            // Calculate pull strength based on distance if enabled
            double pullStrength = basePullStrength;
            if (distanceBased) {
                pullStrength = basePullStrength * (1.0 + (distance / maxRange));
            }

            // Calculate pull vector (from target to caster)
            Vector pullVector = caster.getLocation().toVector()
                    .subtract(target.getLocation().toVector())
                    .normalize()
                    .multiply(pullStrength);

            // Add some upward force to help with terrain
            pullVector.setY(pullVector.getY() + 0.3);

            // Apply the pull
            target.setVelocity(pullVector);

            // Effects
            target.playSound(target.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.0f);
            caster.playSound(caster.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.0f);

            // Visual effects
            Location targetLoc = target.getLocation().add(0, 1, 0);
            targetLoc.getWorld().spawnParticle(Particle.CRIT, targetLoc, 15, 0.5, 0.5, 0.5, 0.1);

            // Create a line effect between caster and target
            createLassoLineEffect(caster.getLocation(), target.getLocation());

            // Message
            caster.sendMessage(ChatColor.GOLD + "Lassoed " + target.getName() + "!");
            target.sendMessage(ChatColor.RED + "You've been lassoed by " + caster.getName() + "!");
        }

        // Remove the arrow
        arrow.remove();
    }

    private void handleTaserHit(ProjectileHitEvent event) {
        Arrow arrow = (Arrow) event.getEntity();

        // Get the caster
        String casterUUID = arrow.getMetadata("taser_caster").get(0).asString();
        Player caster = Bukkit.getPlayer(UUID.fromString(casterUUID));

        if (caster == null) {
            arrow.remove();
            return;
        }

        // Check if we hit a player
        if (event.getHitEntity() instanceof Player) {
            Player target = (Player) event.getHitEntity();

            if (target.equals(caster)) {
                arrow.remove();
                return; // Don't tase yourself
            }

            // Get taser settings
            int stunDuration = arrow.getMetadata("stun_duration").get(0).asInt();
            double maxRange = arrow.getMetadata("taser_range").get(0).asDouble();

            // Check range
            double distance = caster.getLocation().distance(target.getLocation());
            if (distance > maxRange) {
                caster.sendMessage(ChatColor.RED + "Target is too far away!");
                arrow.remove();
                return;
            }

            // Apply stun effect
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, stunDuration, 255));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, stunDuration, 255));
            target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, stunDuration, -10));

            // Freeze them in place
            Location freezeLocation = target.getLocation();

            // Create a task to keep them frozen
            int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (target.isOnline() && target.hasPotionEffect(PotionEffectType.SLOW)) {
                    // Teleport them back to freeze location if they move too far
                    if (target.getLocation().distance(freezeLocation) > 0.5) {
                        target.teleport(freezeLocation);
                    }

                    // Electric effect while stunned
                    target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            target.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.1);
                }
            }, 0L, 2L).getTaskId();

            // Cancel the freeze task when stun ends
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Bukkit.getScheduler().cancelTask(taskId);
            }, stunDuration + 5);

            // Effects
            target.playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 2.0f);
            caster.playSound(caster.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 2.0f);

            // Visual effects
            Location targetLoc = target.getLocation().add(0, 1, 0);
            targetLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, targetLoc, 25, 0.5, 1, 0.5, 0.2);
            targetLoc.getWorld().spawnParticle(Particle.CRIT, targetLoc, 10, 0.3, 0.5, 0.3, 0.1);

            // Messages
            caster.sendMessage(ChatColor.YELLOW + "You tased " + target.getName() + "!");
            target.sendMessage(ChatColor.RED + "You've been tased by " + caster.getName() + "! You're stunned!");
        }

        // Remove the arrow
        arrow.remove();
    }

    private void handleSwapBallHit(ProjectileHitEvent event) {
        EnderPearl pearl = (EnderPearl) event.getEntity();

        // This is the key part: it stops the default ender pearl teleport
        event.setCancelled(true);
        pearl.remove();

        // We only care if it hits a player
        if (!(event.getHitEntity() instanceof Player)) {
            return;
        }

        Player target = (Player) event.getHitEntity();

        // Get the player who threw the ball
        if (!pearl.hasMetadata("caster_uuid")) return;
        String casterUUID = pearl.getMetadata("caster_uuid").get(0).asString();
        Player caster = Bukkit.getPlayer(UUID.fromString(casterUUID));

        // Make sure the caster is valid and not hitting themselves
        if (caster == null || !caster.isOnline() || caster.equals(target)) {
            return;
        }

        // --- SWAP LOGIC ---
        Location casterLoc = caster.getLocation().clone();
        Location targetLoc = target.getLocation().clone();

        caster.teleport(targetLoc);
        target.teleport(casterLoc);

        // Sound effects
        caster.playSound(caster.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        target.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }

    @EventHandler
    public void onCrossbowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getBow() == null) return;

        Player player = (Player) event.getEntity();

        // Check if they're using an explosive crossbow
        if (plugin.getItemManager().isCustomItem(event.getBow())) {
            CustomItem item = plugin.getItemManager().getCustomItem(event.getBow());
            if (item != null && "explosive_crossbow".equals(item.getCustomDataString("type", ""))) {
                // Mark the projectile as explosive
                if (event.getProjectile() instanceof Arrow) {
                    Arrow arrow = (Arrow) event.getProjectile();
                    double explosionPower = item.getCustomDataDouble("explosion-power", 2.0);

                    arrow.setMetadata("explosive_arrow", new FixedMetadataValue(plugin, true));
                    arrow.setMetadata("explosion_power", new FixedMetadataValue(plugin, explosionPower));

                    // Visual trail
                    createExplosiveTrail(arrow);
                }
            }
        }
    }

    private void createExplosiveTrail(Arrow arrow) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!arrow.isValid() || arrow.isDead()) {
                return;
            }
            arrow.getWorld().spawnParticle(Particle.FLAME, arrow.getLocation(), 2, 0.1, 0.1, 0.1, 0);
        }, 0L, 2L);
    }

    private void createLassoLineEffect(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();

        for (double i = 0; i < distance; i += 0.5) {
            Location particleLoc = from.clone().add(direction.clone().multiply(i));
            particleLoc.getWorld().spawnParticle(Particle.CRIT, particleLoc, 1, 0, 0, 0, 0);
        }
    }

    // ===== UNUSED HELPER METHODS (KEEP FOR COMPATIBILITY) =====

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