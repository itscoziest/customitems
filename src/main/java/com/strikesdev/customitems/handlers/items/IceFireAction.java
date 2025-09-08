package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class IceFireAction implements ItemAction {
    private final CustomItems plugin;

    public IceFireAction(CustomItems plugin) {
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

        // Place soul campfire
        campfireLoc.getBlock().setType(Material.SOUL_CAMPFIRE);

        double radius = item.getRadius() > 0 ? item.getRadius() : 4.0;
        int duration = item.getDuration() > 0 ? item.getDuration() : 30;

        // Start slowness effect task
        BukkitRunnable slowTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Check if campfire still exists
                if (campfireLoc.getBlock().getType() != Material.SOUL_CAMPFIRE) {
                    this.cancel();
                    return;
                }

                // Get configurable slowness values
                int slownessDuration = item.getCustomDataInt("slowness-duration", 60);
                int slownessAmplifier = item.getCustomDataInt("slowness-amplifier", 2);

                // Apply slowness to nearby enemies
                for (Entity entity : campfireLoc.getWorld().getNearbyEntities(campfireLoc, radius, radius, radius)) {
                    if (entity instanceof Player && !entity.equals(player)) {
                        Player target = (Player) entity;
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slownessDuration, slownessAmplifier));

                        // Ice particles
                        target.spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.05);
                    }
                }

                // Ice particles around campfire
                campfireLoc.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        campfireLoc.clone().add(0.5, 1, 0.5), 10, radius/2, 0.5, radius/2, 0.05);
            }
        };

        slowTask.runTaskTimer(plugin, 20L, 20L); // Every second

        // Remove campfire after duration
        new BukkitRunnable() {
            @Override
            public void run() {
                slowTask.cancel();
                if (campfireLoc.getBlock().getType() == Material.SOUL_CAMPFIRE) {
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