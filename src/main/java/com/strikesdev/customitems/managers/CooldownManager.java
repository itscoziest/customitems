package com.strikesdev.customitems.managers;

import com.strikesdev.customitems.CustomItems;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final CustomItems plugin;
    private final Map<UUID, Map<String, Long>> cooldowns;

    public CooldownManager(CustomItems plugin) {
        this.plugin = plugin;
        this.cooldowns = new ConcurrentHashMap<>();
    }

    public void setCooldown(Player player, String itemId, int seconds) {


        UUID playerId = player.getUniqueId();
        cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(itemId, System.currentTimeMillis() + (seconds * 1000L));
    }

    public boolean hasCooldown(Player player, String itemId) {


        UUID playerId = player.getUniqueId();
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);

        if (playerCooldowns == null) {
            return false;
        }

        Long cooldownEnd = playerCooldowns.get(itemId);
        if (cooldownEnd == null) {
            return false;
        }

        if (System.currentTimeMillis() >= cooldownEnd) {
            playerCooldowns.remove(itemId);
            return false;
        }

        return true;
    }

    public long getRemainingCooldown(Player player, String itemId) {


        UUID playerId = player.getUniqueId();
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);

        if (playerCooldowns == null) {
            return 0;
        }

        Long cooldownEnd = playerCooldowns.get(itemId);
        if (cooldownEnd == null) {
            return 0;
        }

        long remaining = cooldownEnd - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public int getRemainingCooldownSeconds(Player player, String itemId) {
        return (int) Math.ceil(getRemainingCooldown(player, itemId) / 1000.0);
    }

    public void removeCooldown(Player player, String itemId) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);

        if (playerCooldowns != null) {
            playerCooldowns.remove(itemId);
            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(playerId);
            }
        }
    }

    public void removeAllCooldowns(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    public void clearAll() {
        cooldowns.clear();
    }

    public void cleanupExpiredCooldowns() {
        long currentTime = System.currentTimeMillis();

        cooldowns.entrySet().removeIf(entry -> {
            Map<String, Long> playerCooldowns = entry.getValue();
            playerCooldowns.entrySet().removeIf(cooldownEntry ->
                    cooldownEntry.getValue() <= currentTime);
            return playerCooldowns.isEmpty();
        });
    }

    public void cleanup() {
        cooldowns.clear();
    }

    // Get all active cooldowns for a player
    public Map<String, Long> getPlayerCooldowns(Player player) {
        return cooldowns.getOrDefault(player.getUniqueId(), new ConcurrentHashMap<>());
    }

    // Check if player has any active cooldowns
    public boolean hasAnyCooldowns(Player player) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null || playerCooldowns.isEmpty()) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        return playerCooldowns.values().stream().anyMatch(time -> time > currentTime);
    }
}