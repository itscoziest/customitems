package com.strikesdev.customitems.listeners;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ProjectileListener implements Listener {
    private final CustomItems plugin;
    private final Set<Location> dragonBreathImpacts = new HashSet<>();

    public ProjectileListener(CustomItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();

        // FIX: Region Check (Prevents shooting into protected regions from outside)
        // This solves Issue #5: "Super wind charge can be used in regions its not supposed to"
        if (plugin.getRegionManager().isWorldGuardAvailable()) {
            Location impactLoc = projectile.getLocation();
            if (event.getHitEntity() != null) impactLoc = event.getHitEntity().getLocation();

            if (projectile.hasMetadata("custom_item")) {
                String itemId = projectile.getMetadata("custom_item").get(0).asString();
                CustomItem ci = plugin.getItemManager().getCustomItem(itemId);

                if (ci != null && projectile.getShooter() instanceof Player) {
                    Player shooter = (Player) projectile.getShooter();
                    // Check if the impact location allows this item
                    if (!plugin.getRegionManager().canUseItemInRegion(shooter, ci, impactLoc)) {
                        projectile.remove(); // Fizzle the projectile
                        return; // Stop processing
                    }
                }
            }
        }

        // --- Wind Charge Logic ---
        if (projectile instanceof WindCharge) {
            if (projectile.hasMetadata("custom_item") && "super_wind_charge".equals(projectile.getMetadata("custom_item").get(0).asString())) {
                handleSuperWindChargeHit(event);
                return;
            }
        }

        // --- Snowball Logic ---
        if (projectile instanceof Snowball) {
            // Dragon breath
            if (projectile.hasMetadata("dragon_breath_projectile")) {
                onDragonBreathImpact(event);
                return;
            }

            // Slowness Snowball
            if (projectile.hasMetadata("custom_item") && "slowness_snowball".equals(projectile.getMetadata("custom_item").get(0).asString())) {
                handleSlownessSnowballHit(event);
                return;
            }

            // Swap Ball
            if (projectile.hasMetadata("custom_item") && "swap_ball".equals(projectile.getMetadata("custom_item").get(0).asString())) {
                handleSwapBallHit(event);
                return;
            }
        }

        // --- Fireball Logic ---
        if (projectile instanceof Fireball) {
            Fireball fireball = (Fireball) projectile;
            if (fireball.hasMetadata("custom_item") && "fireball".equals(fireball.getMetadata("custom_item").get(0).asString())) {
                handleFireballHit(event);
                fireball.remove();
                return;
            }
        }

        // --- Arrow Logic ---
        if (projectile instanceof Arrow) {
            Arrow arrow = (Arrow) projectile;

            if (arrow.hasMetadata("lasso_arrow")) {
                handleLassoHit(event);
                return;
            }

            if (arrow.hasMetadata("taser_arrow")) {
                handleTaserHit(event);
                return;
            }

            if (arrow.hasMetadata("custom_item") && "homing_dart".equals(arrow.getMetadata("custom_item").get(0).asString())) {
                handleHomingDartHit(event);
                return;
            }

            if (arrow.hasMetadata("explosive_arrow")) {
                handleExplosiveArrowHit(event);
                return;
            }
        }
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
                            double damageToApply = damagePerTick;

                            victim.damage(damageToApply, caster);

                            victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                                    victim.getLocation().add(0, 1.5, 0), 8, 0.4, 0.4, 0.4, 0.2);
                            victim.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.8f, 1.5f);
                        }
                    }
                }

                currentTick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void handleSuperWindChargeHit(ProjectileHitEvent event) {
        // Get configurable values from metadata
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

    private void handleFireballHit(ProjectileHitEvent event) {
        // 1. Get damage from config (Defaults to 1.5 if not found)
        double damage = 1.5;
        if (event.getEntity().hasMetadata("damage")) {
            damage = event.getEntity().getMetadata("damage").get(0).asDouble();
        }

        Player owner = null;
        if (event.getEntity().getShooter() instanceof Player) {
            owner = (Player) event.getEntity().getShooter();
        }

        Location loc = event.getEntity().getLocation();

        // --- FIX: VISUALS ONLY (No Real Explosion Damage) ---
        // This makes the "Boom" effect without hurting anyone
        loc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, loc, 1);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        // --- 2. APPLY EXACT CONFIG DAMAGE ---
        // We manually damage entities so it is exactly 1.5 (or whatever you set)
        double impactRadius = 3.5;
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, impactRadius, impactRadius, impactRadius)) {
            if (entity instanceof LivingEntity) {
                if (owner != null && entity.equals(owner)) continue;

                // This will deal exactly 1.5 damage
                ((LivingEntity) entity).damage(damage, owner);
            }
        }
    }

    private void handleSlownessSnowballHit(ProjectileHitEvent event) {
        if (event.getHitEntity() instanceof Player) {
            Player hitPlayer = (Player) event.getHitEntity();
            hitPlayer.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOW,
                    60, // 3 seconds
                    1   // Slowness II
            ));

            hitPlayer.spawnParticle(Particle.SNOWFLAKE, hitPlayer.getLocation().add(0, 1, 0), 15, 0.5, 1, 0.5, 0.1);
            hitPlayer.playSound(hitPlayer.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.2f);
        }
    }

    private void handleSwapBallHit(ProjectileHitEvent event) {
        Snowball pearl = (Snowball) event.getEntity();
        pearl.remove();

        if (!(event.getHitEntity() instanceof Player)) {
            return;
        }

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

    private void handleExplosiveArrowHit(ProjectileHitEvent event) {
        Arrow arrow = (Arrow) event.getEntity();
        double explosionPower = arrow.getMetadata("explosion_power").get(0).asDouble();
        Location loc = arrow.getLocation();
        loc.getWorld().createExplosion(loc, (float) explosionPower, false, false);
        loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 1);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        arrow.remove();
    }

    private void handleLassoHit(ProjectileHitEvent event) {
        Arrow arrow = (Arrow) event.getEntity();
        String casterUUID = arrow.getMetadata("lasso_caster").get(0).asString();
        Player caster = Bukkit.getPlayer(UUID.fromString(casterUUID));

        if (caster == null) {
            arrow.remove();
            return;
        }

        if (event.getHitEntity() instanceof Player) {
            Player target = (Player) event.getHitEntity();

            if (target.equals(caster)) {
                arrow.remove();
                return;
            }

            double basePullStrength = arrow.getMetadata("pull_strength").get(0).asDouble();
            double maxRange = arrow.getMetadata("max_range").get(0).asDouble();
            boolean distanceBased = arrow.getMetadata("distance_based").get(0).asBoolean();

            double distance = caster.getLocation().distance(target.getLocation());

            if (distance > maxRange) {
                caster.sendMessage(ChatColor.RED + "Target is too far away!");
                arrow.remove();
                return;
            }

            double pullStrength = basePullStrength;
            if (distanceBased) {
                pullStrength = basePullStrength * (1.0 + (distance / maxRange));
            }

            Vector pullVector = caster.getLocation().toVector()
                    .subtract(target.getLocation().toVector())
                    .normalize()
                    .multiply(pullStrength);
            pullVector.setY(pullVector.getY() + 0.3);

            target.setVelocity(pullVector);
            target.playSound(target.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.0f);
            caster.playSound(caster.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.0f);

            Location targetLoc = target.getLocation().add(0, 1, 0);
            targetLoc.getWorld().spawnParticle(Particle.CRIT, targetLoc, 15, 0.5, 0.5, 0.5, 0.1);
            createLassoLineEffect(caster.getLocation(), target.getLocation());

            caster.sendMessage(ChatColor.GOLD + "Lassoed " + target.getName() + "!");
            target.sendMessage(ChatColor.RED + "You've been lassoed by " + caster.getName() + "!");
        }
        arrow.remove();
    }

    private void handleTaserHit(ProjectileHitEvent event) {
        Arrow arrow = (Arrow) event.getEntity();
        String casterUUID = arrow.getMetadata("taser_caster").get(0).asString();
        Player caster = Bukkit.getPlayer(UUID.fromString(casterUUID));

        if (caster == null) {
            arrow.remove();
            return;
        }

        if (event.getHitEntity() instanceof Player) {
            Player target = (Player) event.getHitEntity();

            if (target.equals(caster)) {
                arrow.remove();
                return;
            }

            int stunDuration = arrow.getMetadata("stun_duration").get(0).asInt();
            double maxRange = arrow.getMetadata("taser_range").get(0).asDouble();

            double distance = caster.getLocation().distance(target.getLocation());
            if (distance > maxRange) {
                caster.sendMessage(ChatColor.RED + "Target is too far away!");
                arrow.remove();
                return;
            }

            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, stunDuration, 255));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, stunDuration, 255));
            target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, stunDuration, -10));

            Location freezeLocation = target.getLocation();

            int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (target.isOnline() && target.hasPotionEffect(PotionEffectType.SLOW)) {
                    if (target.getLocation().distance(freezeLocation) > 0.5) {
                        target.teleport(freezeLocation);
                    }
                    target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            target.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.1);
                }
            }, 0L, 2L).getTaskId();

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Bukkit.getScheduler().cancelTask(taskId);
            }, stunDuration + 5);

            target.playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 2.0f);
            caster.playSound(caster.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 2.0f);

            Location targetLoc = target.getLocation().add(0, 1, 0);
            targetLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, targetLoc, 25, 0.5, 1, 0.5, 0.2);
            targetLoc.getWorld().spawnParticle(Particle.CRIT, targetLoc, 10, 0.3, 0.5, 0.3, 0.1);

            caster.sendMessage(ChatColor.YELLOW + "You tased " + target.getName() + "!");
            target.sendMessage(ChatColor.RED + "You've been tased by " + caster.getName() + "! You're stunned!");
        }
        arrow.remove();
    }

    private void handleHomingDartHit(ProjectileHitEvent event) {
        if (event.getHitEntity() instanceof Player) {
            Player hitPlayer = (Player) event.getHitEntity();

            if (event.getEntity().hasMetadata("can_damage") &&
                    event.getEntity().getMetadata("can_damage").get(0).asBoolean()) {

                double damage = 6.0;
                if (event.getEntity().hasMetadata("damage")) {
                    damage = event.getEntity().getMetadata("damage").get(0).asDouble();
                }

                Entity shooter = (Entity) event.getEntity().getShooter();
                if (shooter != null) {
                    hitPlayer.damage(damage, shooter);
                } else {
                    hitPlayer.damage(damage);
                }
            }
        }
        event.getEntity().remove();
    }

    @EventHandler
    public void onCrossbowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getBow() == null) return;

        if (plugin.getItemManager().isCustomItem(event.getBow())) {
            CustomItem item = plugin.getItemManager().getCustomItem(event.getBow());
            if (item != null && "explosive_crossbow".equals(item.getCustomDataString("type", ""))) {
                if (event.getProjectile() instanceof Arrow) {
                    Arrow arrow = (Arrow) event.getProjectile();
                    double explosionPower = item.getCustomDataDouble("explosion-power", 2.0);

                    arrow.setMetadata("explosive_arrow", new FixedMetadataValue(plugin, true));
                    arrow.setMetadata("explosion_power", new FixedMetadataValue(plugin, explosionPower));

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
}