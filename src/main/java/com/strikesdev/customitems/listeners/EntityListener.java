package com.strikesdev.customitems.listeners;

import com.strikesdev.customitems.CustomItems;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EvokerFangs;


// Entity Listener for vampire sword and other entity interactions
public class EntityListener implements Listener {
    private final CustomItems plugin;

    public EntityListener(CustomItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Handle vampire sword healing
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        // Check if killer is holding vampire sword
        if (killer.getInventory().getItemInMainHand() != null) {
            var customItem = plugin.getItemManager().getCustomItem(killer.getInventory().getItemInMainHand());
            if (customItem != null && "vampire_sword".equals(customItem.getCustomDataString("type", ""))) {
                int healAmount = customItem.getCustomDataInt("heal-on-kill", 4);

                // Only heal missing health, not add extra hearts
                double currentHealth = killer.getHealth();
                double maxHealth = killer.getMaxHealth();

                if (currentHealth < maxHealth) {
                    double newHealth = Math.min(maxHealth, currentHealth + healAmount);
                    killer.setHealth(newHealth);

                    // Visual feedback
                    killer.sendMessage(plugin.getConfigManager().getMessage("vampire-sword.healed",
                            "{amount}", String.valueOf((int)(newHealth - currentHealth))));
                }
            }
        }
    }

    @EventHandler
    public void onFireworkDamage(EntityDamageByEntityEvent event) {
        // Prevent damage from smoke bomb fireworks
        if (event.getDamager() instanceof Firework) {
            Firework firework = (Firework) event.getDamager();
            if (firework.hasMetadata("no_damage")) {
                event.setCancelled(true); // Prevent all damage from smoke bomb fireworks
            }
        }
    }

    @EventHandler
    public void onEvokerFangsDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof EvokerFangs) {
            EvokerFangs fangs = (EvokerFangs) event.getDamager();

            if (fangs.hasMetadata("snap_ring_fangs")) {
                // Get owner and custom damage
                String ownerUUID = fangs.getMetadata("owner").get(0).asString();
                double customDamage = fangs.getMetadata("custom_damage").get(0).asDouble();

                // Don't damage the owner
                if (event.getEntity() instanceof Player) {
                    Player target = (Player) event.getEntity();
                    if (target.getUniqueId().toString().equals(ownerUUID)) {
                        event.setCancelled(true);
                        return;
                    }
                }

                // Apply custom damage
                event.setDamage(customDamage);
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            Player player = (Player) event.getEntity();

            if (player.hasMetadata("leap_protection")) {
                long protectionEnd = player.getMetadata("leap_protection").get(0).asLong();
                if (System.currentTimeMillis() < protectionEnd) {
                    event.setCancelled(true); // Cancel fall damage
                    player.removeMetadata("leap_protection", plugin);
                }
            }
        }
    }

    @EventHandler
    public void onEnderPearlTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            // Check if this is from a swap ball
            Player player = event.getPlayer();

            // Find nearby ender pearls with no_teleport metadata
            for (Entity entity : player.getWorld().getNearbyEntities(event.getTo(), 5, 5, 5)) {
                if (entity instanceof EnderPearl) {
                    EnderPearl pearl = (EnderPearl) entity;
                    if (pearl.hasMetadata("no_teleport") && pearl.getShooter() == player) {
                        event.setCancelled(true); // Cancel normal teleportation
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onIllusionZombieTarget(EntityTargetEvent event) {
        if (event.getEntity().hasMetadata("illusion_zombie")) {
            String ownerUUID = event.getEntity().getMetadata("illusion_owner").get(0).asString();

            // Don't target the owner
            if (event.getTarget() instanceof Player) {
                Player target = (Player) event.getTarget();
                if (target.getUniqueId().toString().equals(ownerUUID)) {
                    event.setCancelled(true);

                    // Find a different target (enemy player)
                    Player newTarget = findNearestEnemy(target, event.getEntity().getLocation(), 10.0);
                    if (newTarget != null) {
                        event.setTarget(newTarget);
                    }
                }
            }
        }
    }

    private Player findNearestEnemy(Player owner, Location center, double range) {
        Player closest = null;
        double closestDistance = range;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.equals(owner) || !player.getWorld().equals(center.getWorld())) {
                continue;
            }

            double distance = player.getLocation().distance(center);
            if (distance < closestDistance) {
                closest = player;
                closestDistance = distance;
            }
        }

        return closest;
    }
}