package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.EquipmentSlot;


public class DogArmyAction implements ItemAction {
    private final CustomItems plugin;

    public DogArmyAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        // Get number of dogs from config or use default
        int dogCount = item.getCustomDataInt("dog-count", 2);
        int dogHealth = item.getCustomDataInt("dog-health", 20); // Default 10 hearts

        // FIX: Get duration in seconds and convert to ticks
        int dogDurationSeconds = item.getCustomDataInt("dog-duration", 15); // Default 15 seconds
        long dogDurationTicks = (long) dogDurationSeconds * 20L;

        Location spawnLocation = player.getLocation();

        // Spawn the dogs around the player
        for (int i = 0; i < dogCount; i++) {
            // Calculate spawn position in a circle around player
            double angle = (2 * Math.PI * i) / dogCount;
            double x = spawnLocation.getX() + (2 * Math.cos(angle));
            double z = spawnLocation.getZ() + (2 * Math.sin(angle));

            Location dogSpawnLoc = new Location(spawnLocation.getWorld(), x, spawnLocation.getY(), z);

            // Make sure spawn location is safe
            dogSpawnLoc.setY(dogSpawnLoc.getWorld().getHighestBlockYAt(dogSpawnLoc) + 1);

            // Spawn the wolf
            Wolf wolf = (Wolf) dogSpawnLoc.getWorld().spawnEntity(dogSpawnLoc, EntityType.WOLF);

            // Tame the wolf to the player
            wolf.setTamed(true);
            wolf.setOwner(player);
            wolf.setAdult();
            wolf.setHealth(dogHealth);
            wolf.setMaxHealth(dogHealth);

            // Make the wolf aggressive and sitting initially
            wolf.setSitting(false);
            wolf.setAngry(false);

            // Add custom name
            wolf.setCustomName(ChatColor.GOLD + player.getName() + "'s Guard Dog");
            wolf.setCustomNameVisible(true);

            // Schedule removal after duration
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (wolf.isValid()) {
                    // Poof effect when despawning
                    wolf.getWorld().spawnParticle(Particle.CLOUD, wolf.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);
                    wolf.remove();
                }
            }, dogDurationTicks); // Use the corrected ticks
        }

        // Play sound and effects
        player.playSound(player.getLocation(), Sound.ENTITY_WOLF_HOWL, 1.0f, 1.0f);
        spawnLocation.getWorld().spawnParticle(Particle.HEART,
                spawnLocation.add(0, 1, 0), dogCount * 5, 1, 1, 1, 0.1);

        if (event.getItem() != null) {
            if (event.getItem().getAmount() > 1) {
                event.getItem().setAmount(event.getItem().getAmount() - 1);
            } else {
                player.getInventory().setItem(event.getHand(), null);
            }
        }

        return true;
    }
}