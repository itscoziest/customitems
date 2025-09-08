package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class HealingCampfireAction implements ItemAction {
    private final CustomItems plugin;

    public HealingCampfireAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return false;

        Location campfireLoc = clickedBlock.getRelative(event.getBlockFace()).getLocation();

        // Place regular campfire
        campfireLoc.getBlock().setType(Material.CAMPFIRE);

        double radius = item.getRadius() > 0 ? item.getRadius() : 3.0;
        int duration = item.getDuration() > 0 ? item.getDuration() : 30;
        int healAmount = item.getCustomDataInt("heal-amount", 1);

        // Start healing task
        BukkitRunnable healTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Check if campfire still exists
                if (campfireLoc.getBlock().getType() != Material.CAMPFIRE) {
                    this.cancel();
                    return;
                }

                // Show range particles (happy villager particles around the edge)
                for (int i = 0; i < 16; i++) {
                    double angle = (2 * Math.PI * i) / 16;
                    double x = campfireLoc.getX() + 0.5 + radius * Math.cos(angle);
                    double z = campfireLoc.getZ() + 0.5 + radius * Math.sin(angle);
                    double y = campfireLoc.getY() + 0.5;

                    Location particleLoc = new Location(campfireLoc.getWorld(), x, y, z);
                    campfireLoc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleLoc, 1, 0, 0, 0, 0);
                }

                // Additional particles above the campfire
                campfireLoc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY,
                        campfireLoc.clone().add(0.5, 1.5, 0.5), 3, 0.3, 0.3, 0.3, 0.05);

                // Heal only the owner if they're nearby
                if (player.isOnline() && player.getLocation().distance(campfireLoc) <= radius) {
                    double currentHealth = player.getHealth();
                    double maxHealth = player.getMaxHealth();

                    if (currentHealth < maxHealth) {
                        double newHealth = Math.min(maxHealth, currentHealth + healAmount);
                        player.setHealth(newHealth);

                        // Healing particles on the player
                        player.spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 3, 0.3, 0.3, 0.3, 0.1);

                        // Extra happy particles when healing
                        player.spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.1);
                    }
                }
            }
        };

        healTask.runTaskTimer(plugin, 20L, 40L); // Heal every 2 seconds

        // Remove campfire after duration
        new BukkitRunnable() {
            @Override
            public void run() {
                healTask.cancel();
                if (campfireLoc.getBlock().getType() == Material.CAMPFIRE) {
                    campfireLoc.getBlock().setType(Material.AIR);
                }
            }
        }.runTaskLater(plugin, duration * 20L);

        // Register as temporary block
        plugin.getEffectManager().registerTemporaryBlock(campfireLoc, duration);

        // Consume item
        consumeItem(player, event);
        return true;
    }

    private void consumeItem(Player player, PlayerInteractEvent event) {
        if (event.getItem().getAmount() > 1) {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
        } else {
            player.getInventory().setItem(event.getHand(), null);
        }
    }
}
