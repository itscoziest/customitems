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
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.ChatColor;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.metadata.FixedMetadataValue;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Fireball;
import org.bukkit.event.entity.EntityShootBowEvent;


// Projectile Listener for custom projectiles
public class ProjectileListener implements Listener {
    private final CustomItems plugin;

    public ProjectileListener(CustomItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;

        Arrow arrow = (Arrow) event.getEntity();

        // Check if this is an explosive arrow
        if (arrow.hasMetadata("explosive_arrow")) {
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

    @EventHandler
    public void onFireballHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Fireball)) return;

        Fireball fireball = (Fireball) event.getEntity();

        // Check if this is a grave digger fireball
        if (fireball.hasMetadata("grave_digger")) {
            // Cancel the hit event to prevent normal fireball explosion/despawn
            event.setCancelled(true);

            // Don't remove the fireball - let it continue through blocks
            // The timer in GraveDiggerAction will handle removal
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


    @EventHandler
    public void onDragonFireballHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof DragonFireball)) return;

        DragonFireball fireball = (DragonFireball) event.getEntity();

        // Check if this is our custom dragon breath
        if (!fireball.hasMetadata("custom_dragon_breath")) return;

        Location hitLocation = fireball.getLocation();

        // Get stored metadata
        double damage = fireball.getMetadata("breath_damage").get(0).asDouble();
        int duration = fireball.getMetadata("breath_duration").get(0).asInt();
        String casterUUID = fireball.getMetadata("breath_caster").get(0).asString();

        Player caster = Bukkit.getPlayer(java.util.UUID.fromString(casterUUID));

        // Create area effect cloud (lingering dragon breath)
        AreaEffectCloud cloud = hitLocation.getWorld().spawn(hitLocation, AreaEffectCloud.class);

        // Configure the cloud like dragon breath
        cloud.setRadius(3.0f);
        cloud.setDuration(duration);
        cloud.setRadiusOnUse(0f);
        cloud.setRadiusPerTick(0.02f);
        cloud.setParticle(Particle.DRAGON_BREATH);
        cloud.setColor(Color.PURPLE);

        if (caster != null) {
            cloud.setSource(caster);
        }

        // Add harmful effect (instant damage every tick)
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.HARM, 1, 0), true);

        // Play explosion sound and effects
        hitLocation.getWorld().playSound(hitLocation, Sound.ENTITY_ENDER_DRAGON_HURT, 1.0f, 0.8f);
        hitLocation.getWorld().spawnParticle(Particle.DRAGON_BREATH, hitLocation, 50, 2, 1, 2, 0.1);

        // Remove the fireball
        fireball.remove();
    }



    @EventHandler
    public void onLassoHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;

        Arrow arrow = (Arrow) event.getEntity();

        // Check if this is a lasso arrow
        if (!arrow.hasMetadata("lasso_arrow")) return;

        // Get the caster
        String casterUUID = arrow.getMetadata("lasso_caster").get(0).asString();
        Player caster = Bukkit.getPlayer(java.util.UUID.fromString(casterUUID));

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
                // Stronger pull for longer distances (up to 2x strength)
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

    private void createLassoLineEffect(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();

        for (double i = 0; i < distance; i += 0.5) {
            Location particleLoc = from.clone().add(direction.clone().multiply(i));
            particleLoc.getWorld().spawnParticle(Particle.CRIT, particleLoc, 1, 0, 0, 0, 0);
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

            // Visual feedback
            hitPlayer.spawnParticle(Particle.SNOWFLAKE, hitPlayer.getLocation().add(0, 1, 0), 15, 0.5, 1, 0.5, 0.1);
            hitPlayer.playSound(hitPlayer.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.2f);
        }
    }


    @EventHandler
    public void onTaserHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;

        Arrow arrow = (Arrow) event.getEntity();

        // Check if this is a taser arrow
        if (!arrow.hasMetadata("taser_arrow")) return;

        // Get the caster
        String casterUUID = arrow.getMetadata("taser_caster").get(0).asString();
        Player caster = Bukkit.getPlayer(java.util.UUID.fromString(casterUUID));

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

            // Apply stun effect (slowness + mining fatigue + jump boost negative)
            target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    PotionEffectType.SLOW, stunDuration, 255));
            target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOW_DIGGING, stunDuration, 255));
            target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.JUMP, stunDuration, -10));

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