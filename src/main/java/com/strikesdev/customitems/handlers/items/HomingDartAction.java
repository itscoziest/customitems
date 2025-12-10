package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class HomingDartAction implements ItemAction {
    private final CustomItems plugin;

    public HomingDartAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        // FIX: Use item's range property for finding target
        double searchRange = item.getRange() > 0 ? item.getRange() : 20.0;
        Player target = findNearestPlayer(player, searchRange);

        if (target == null) {
            player.sendMessage("§cNo target found within range!");
            return false;
        }

        // Launch arrow
        Arrow arrow = player.launchProjectile(Arrow.class);
        arrow.setMetadata("custom_item", new FixedMetadataValue(plugin, "homing_dart"));
        arrow.setMetadata("target", new FixedMetadataValue(plugin, target.getUniqueId()));
        arrow.setMetadata("damage", new FixedMetadataValue(plugin, item.getDamage() > 0 ? item.getDamage() : 6.0));
        arrow.setMetadata("can_damage", new FixedMetadataValue(plugin, true)); // Track if it can still damage

        // Make it home towards target with timeout
        createHomingEffect(arrow, target, 100); // 5 second timeout

        // Sound effect
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.8f);

        // Consume item
        consumeItem(player, event);
        return true;
    }

    private void createHomingEffect(Arrow arrow, Player target, int maxTicks) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                if (!arrow.isValid() || arrow.isDead() || !target.isOnline() || ticks >= maxTicks) {
                    // Remove arrow after timeout or if invalid
                    if (arrow.isValid()) {
                        arrow.remove();
                    }
                    this.cancel();
                    return;
                }

                // Stop homing if arrow hit ground
                if (arrow.isOnGround()) {
                    arrow.setMetadata("can_damage", new FixedMetadataValue(plugin, false));
                    // Remove after short delay
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (arrow.isValid()) arrow.remove();
                        }
                    }.runTaskLater(plugin, 40L); // 2 seconds
                    this.cancel();
                    return;
                }

                // Calculate direction to target
                Vector direction = target.getLocation().add(0, 1, 0).toVector()
                        .subtract(arrow.getLocation().toVector());

                double distance = direction.length();

                // If too far, stop homing
                if (distance > 25.0) {
                    arrow.setMetadata("can_damage", new FixedMetadataValue(plugin, false));
                    this.cancel();
                    return;
                }

                direction.normalize();

                // Apply gentle homing
                Vector currentVel = arrow.getVelocity();
                Vector newVel = currentVel.add(direction.multiply(0.2)).normalize().multiply(1.2);
                arrow.setVelocity(newVel);

                // Particle trail
                arrow.getWorld().spawnParticle(Particle.CRIT_MAGIC, arrow.getLocation(), 2);
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }

    private Player findNearestPlayer(Player shooter, double range) {
        Player nearest = null;
        double nearestDistance = range;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.equals(shooter) || !player.getWorld().equals(shooter.getWorld())) {
                continue;
            }

            double distance = player.getLocation().distance(shooter.getLocation());
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private void createHomingEffect(Arrow arrow, Player target) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isDead() || !target.isOnline()) {
                    this.cancel();
                    return;
                }

                // Calculate direction to target
                Vector direction = target.getLocation().add(0, 1, 0).toVector()
                        .subtract(arrow.getLocation().toVector()).normalize();

                // Apply gentle homing (not too aggressive)
                Vector currentVel = arrow.getVelocity();
                Vector newVel = currentVel.add(direction.multiply(0.3)).normalize().multiply(1.5);
                arrow.setVelocity(newVel);

                // Particle trail
                arrow.getWorld().spawnParticle(Particle.CRIT_MAGIC, arrow.getLocation(), 2);
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }

    private void consumeItem(Player player, PlayerInteractEvent event) {
        if (event.getItem().getAmount() > 1) {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
        } else {
            player.getInventory().setItem(event.getHand(), null);
        }
    }
}