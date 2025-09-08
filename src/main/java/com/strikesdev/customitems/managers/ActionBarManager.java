package com.strikesdev.customitems.managers;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.utils.ChatUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ActionBarManager {

    private final CustomItems plugin;
    private final Map<UUID, BukkitTask> actionBarTasks;
    private final Map<UUID, String> currentMessages;

    public ActionBarManager(CustomItems plugin) {
        this.plugin = plugin;
        this.actionBarTasks = new ConcurrentHashMap<>();
        this.currentMessages = new ConcurrentHashMap<>();
    }

    public void sendCooldownMessage(Player player, String itemName, int remainingSeconds) {
        if (!plugin.getConfigManager().isActionBarCooldownEnabled()) {
            return;
        }

        String message = plugin.getConfigManager().getMessage("cooldown.action-bar",
                "{item}", itemName,
                "{time}", String.valueOf(remainingSeconds));

        sendActionBar(player, message, remainingSeconds);
    }

    public void sendActionBar(Player player, String message) {
        sendActionBar(player, message, 0);
    }

    public void sendActionBar(Player player, String message, int durationSeconds) {
        UUID playerId = player.getUniqueId();

        // Cancel existing task
        BukkitTask existingTask = actionBarTasks.get(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }

        // Store current message
        currentMessages.put(playerId, message);

        // Send immediate message
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(ChatUtils.colorize(message)));

        if (durationSeconds > 0) {
            // Create countdown task
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                private int remaining = durationSeconds;

                @Override
                public void run() {
                    remaining--;

                    if (remaining <= 0 || !player.isOnline()) {
                        // Clear action bar and stop task
                        clearActionBar(player);
                        return;
                    }

                    // Update countdown message
                    String updatedMessage = plugin.getConfigManager().getMessage("cooldown.action-bar",
                            "{item}", extractItemName(message),
                            "{time}", String.valueOf(remaining));

                    currentMessages.put(playerId, updatedMessage);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent(ChatUtils.colorize(updatedMessage)));
                }
            }, 20L, 20L); // Run every second

            actionBarTasks.put(playerId, task);
        }
    }

    public void clearActionBar(Player player) {
        UUID playerId = player.getUniqueId();

        // Cancel task
        BukkitTask task = actionBarTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }

        // Remove message
        currentMessages.remove(playerId);

        // Clear action bar
        if (player.isOnline()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
        }
    }

    public void sendCombatMessage(Player player, int remainingSeconds) {
        String message = plugin.getConfigManager().getMessage("combat.action-bar",
                "{time}", String.valueOf(remainingSeconds));

        sendActionBar(player, message, remainingSeconds);
    }

    public boolean hasActiveMessage(Player player) {
        return currentMessages.containsKey(player.getUniqueId());
    }

    public String getCurrentMessage(Player player) {
        return currentMessages.get(player.getUniqueId());
    }

    public void updateAllCooldownMessages() {
        if (!plugin.getConfigManager().isActionBarCooldownEnabled()) {
            return;
        }

        CooldownManager cooldownManager = plugin.getCooldownManager();

        for (Player player : Bukkit.getOnlinePlayers()) {
            Map<String, Long> playerCooldowns = cooldownManager.getPlayerCooldowns(player);

            if (playerCooldowns.isEmpty()) {
                continue;
            }

            // Find the cooldown with the longest remaining time
            String longestItem = null;
            int longestTime = 0;

            for (Map.Entry<String, Long> entry : playerCooldowns.entrySet()) {
                int remaining = (int) Math.ceil((entry.getValue() - System.currentTimeMillis()) / 1000.0);
                if (remaining > longestTime) {
                    longestTime = remaining;
                    longestItem = entry.getKey();
                }
            }

            if (longestItem != null && longestTime > 0) {
                String itemName = plugin.getItemManager().getCustomItem(longestItem).getName();
                sendCooldownMessage(player, itemName, longestTime);
            }
        }
    }

    private String extractItemName(String message) {
        // Try to extract item name from existing message
        // This is a simple implementation - could be improved
        return "Item"; // Fallback
    }

    public void cleanup() {
        // Cancel all tasks
        for (BukkitTask task : actionBarTasks.values()) {
            task.cancel();
        }
        actionBarTasks.clear();
        currentMessages.clear();
    }

    public void cleanupPlayer(Player player) {
        clearActionBar(player);
    }
}