package com.strikesdev.customitems.listeners;

import com.strikesdev.customitems.CustomItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class BlockListener implements Listener {
    private final CustomItems plugin;

    public BlockListener(CustomItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        // Handle healing campfire placement
        var customItem = plugin.getItemManager().getCustomItem(event.getItemInHand());
        if (customItem != null && "healing_campfire".equals(customItem.getCustomDataString("type", ""))) {

            // Register the campfire as a healing source
            org.bukkit.Location campfireLoc = event.getBlock().getLocation();
            Player owner = event.getPlayer();

            // Start healing task
            startHealingCampfireTask(campfireLoc, owner, customItem);
        }
    }

    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        // Stop healing when campfire is broken
        if (event.getBlock().getType() == org.bukkit.Material.CAMPFIRE ||
                event.getBlock().getType() == org.bukkit.Material.SOUL_CAMPFIRE) {
            // Cancel any associated healing tasks (would need to track these)
        }
    }

    private void startHealingCampfireTask(org.bukkit.Location campfireLoc, Player owner,
                                          com.strikesdev.customitems.models.CustomItem item) {
        int duration = item.getDuration();
        double radius = item.getRadius();
        int healAmount = item.getCustomDataInt("heal-amount", 1);

        org.bukkit.scheduler.BukkitTask task = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Check if campfire still exists
            if (campfireLoc.getBlock().getType() != org.bukkit.Material.CAMPFIRE &&
                    campfireLoc.getBlock().getType() != org.bukkit.Material.SOUL_CAMPFIRE) {
                return; // Task will be cancelled by duration
            }

            // Heal only the owner if they're nearby
            if (owner.isOnline() && owner.getLocation().distance(campfireLoc) <= radius) {
                double currentHealth = owner.getHealth();
                double maxHealth = owner.getMaxHealth();

                if (currentHealth < maxHealth) {
                    double newHealth = Math.min(maxHealth, currentHealth + healAmount);
                    owner.setHealth(newHealth);

                    // Visual effect
                    owner.spawnParticle(org.bukkit.Particle.HEART, owner.getLocation().add(0, 2, 0), 3);
                }
            }
        }, 20L, 40L); // Heal every 2 seconds

        // Cancel task after duration
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            task.cancel();
            // Remove campfire
            campfireLoc.getBlock().setType(org.bukkit.Material.AIR);
        }, duration * 20L);
    }
}