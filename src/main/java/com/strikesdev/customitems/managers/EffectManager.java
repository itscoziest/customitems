package com.strikesdev.customitems.managers;

import com.strikesdev.customitems.CustomItems;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Material;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EffectManager {

    private final CustomItems plugin;
    private final Map<UUID, Long> temporaryEntities;
    private final Map<UUID, BukkitTask> entityTasks;
    private final Map<Location, Long> temporaryBlocks;
    private final Map<Location, BukkitTask> blockTasks;

    public EffectManager(CustomItems plugin) {
        this.plugin = plugin;
        this.temporaryEntities = new ConcurrentHashMap<>();
        this.entityTasks = new ConcurrentHashMap<>();
        this.temporaryBlocks = new ConcurrentHashMap<>();
        this.blockTasks = new ConcurrentHashMap<>();
    }

    public void registerTemporaryEntity(Entity entity, int durationSeconds) {
        UUID entityId = entity.getUniqueId();
        long expiryTime = System.currentTimeMillis() + (durationSeconds * 1000L);

        temporaryEntities.put(entityId, expiryTime);

        // Schedule removal task
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (entity.isValid() && !entity.isDead()) {
                entity.remove();
            }
            temporaryEntities.remove(entityId);
            entityTasks.remove(entityId);
        }, durationSeconds * 20L);

        entityTasks.put(entityId, task);
    }

    public void registerTemporaryBlock(Location location, int durationSeconds) {
        long expiryTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        temporaryBlocks.put(location.clone(), expiryTime);

        // Schedule block restoration task
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Restore original block or remove if it was placed by the plugin
            restoreBlock(location);
            temporaryBlocks.remove(location);
            blockTasks.remove(location);
        }, durationSeconds * 20L);

        blockTasks.put(location.clone(), task);
    }

    public void removeTemporaryEntity(Entity entity) {
        UUID entityId = entity.getUniqueId();

        // Cancel task
        BukkitTask task = entityTasks.remove(entityId);
        if (task != null) {
            task.cancel();
        }

        // Remove from tracking
        temporaryEntities.remove(entityId);

        // Remove entity
        if (entity.isValid() && !entity.isDead()) {
            entity.remove();
        }
    }

    public void removeTemporaryBlock(Location location) {
        Location key = findLocationKey(location);
        if (key == null) return;

        // Cancel task
        BukkitTask task = blockTasks.remove(key);
        if (task != null) {
            task.cancel();
        }

        // Remove from tracking
        temporaryBlocks.remove(key);

        // Restore block
        restoreBlock(location);
    }

    public boolean isTemporaryEntity(Entity entity) {
        return temporaryEntities.containsKey(entity.getUniqueId());
    }

    public boolean isTemporaryBlock(Location location) {
        return findLocationKey(location) != null;
    }

    public long getEntityRemainingTime(Entity entity) {
        Long expiryTime = temporaryEntities.get(entity.getUniqueId());
        if (expiryTime == null) {
            return 0;
        }
        return Math.max(0, expiryTime - System.currentTimeMillis());
    }

    public long getBlockRemainingTime(Location location) {
        Location key = findLocationKey(location);
        if (key == null) return 0;

        Long expiryTime = temporaryBlocks.get(key);
        if (expiryTime == null) {
            return 0;
        }
        return Math.max(0, expiryTime - System.currentTimeMillis());
    }

    public void cleanupExpiredEffects() {
        long currentTime = System.currentTimeMillis();

        // Cleanup expired entities
        temporaryEntities.entrySet().removeIf(entry -> {
            if (entry.getValue() <= currentTime) {
                Entity entity = Bukkit.getEntity(entry.getKey());
                if (entity != null && entity.isValid() && !entity.isDead()) {
                    entity.remove();
                }

                // Cancel associated task
                BukkitTask task = entityTasks.remove(entry.getKey());
                if (task != null) {
                    task.cancel();
                }

                return true;
            }
            return false;
        });

        // Cleanup expired blocks
        temporaryBlocks.entrySet().removeIf(entry -> {
            if (entry.getValue() <= currentTime) {
                restoreBlock(entry.getKey());

                // Cancel associated task
                BukkitTask task = blockTasks.remove(entry.getKey());
                if (task != null) {
                    task.cancel();
                }

                return true;
            }
            return false;
        });
    }

    public void cleanup() {
        // Cancel all entity tasks
        for (BukkitTask task : entityTasks.values()) {
            task.cancel();
        }
        entityTasks.clear();

        // Remove all temporary entities
        for (UUID entityId : temporaryEntities.keySet()) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity.isValid() && !entity.isDead()) {
                entity.remove();
            }
        }
        temporaryEntities.clear();

        // Cancel all block tasks
        for (BukkitTask task : blockTasks.values()) {
            task.cancel();
        }
        blockTasks.clear();

        // Restore all temporary blocks
        for (Location location : temporaryBlocks.keySet()) {
            restoreBlock(location);
        }
        temporaryBlocks.clear();
    }

    private Location findLocationKey(Location location) {
        for (Location key : temporaryBlocks.keySet()) {
            if (key.getWorld().equals(location.getWorld()) &&
                    key.getBlockX() == location.getBlockX() &&
                    key.getBlockY() == location.getBlockY() &&
                    key.getBlockZ() == location.getBlockZ()) {
                return key;
            }
        }
        return null;
    }

    private void restoreBlock(Location location) {
        // This should be handled by the BeekeeperAction now
        // Only remove plugin-placed blocks that don't have original block data
        if (location.getBlock().getType() == Material.GLASS ||
                location.getBlock().getType() == Material.SOUL_CAMPFIRE ||
                location.getBlock().getType() == Material.CAMPFIRE) {
            location.getBlock().setType(Material.AIR);
        }
        // Don't touch HONEY_BLOCK here - BeekeeperAction handles it
    }

    // Utility methods
    public int getActiveTemporaryEntitiesCount() {
        return temporaryEntities.size();
    }

    public int getActiveTemporaryBlocksCount() {
        return temporaryBlocks.size();
    }

    public Set<UUID> getTemporaryEntityIds() {
        return new HashSet<>(temporaryEntities.keySet());
    }

    public Set<Location> getTemporaryBlockLocations() {
        return new HashSet<>(temporaryBlocks.keySet());
    }
}