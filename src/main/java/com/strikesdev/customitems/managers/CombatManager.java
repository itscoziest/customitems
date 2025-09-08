package com.strikesdev.customitems.managers;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import com.strikesdev.customitems.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class CombatManager {

    private final CustomItems plugin;
    private final Map<UUID, Long> combatPlayers;

    public CombatManager(CustomItems plugin) {
        this.plugin = plugin;
        this.combatPlayers = new ConcurrentHashMap<>();
    }

    public void enterCombat(Player player) {
        if (player.hasPermission("customitems.bypass.combat")) {
            return;
        }

        UUID playerId = player.getUniqueId();
        int combatDuration = plugin.getConfigManager().getCombatDuration();
        long combatEnd = System.currentTimeMillis() + (combatDuration * 1000L);

        boolean wasInCombat = isInCombat(player);
        combatPlayers.put(playerId, combatEnd);

        if (!wasInCombat) {
            // Player just entered combat, apply item caps
            applyCombatCaps(player);

            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Player " + player.getName() + " entered combat");
            }
        }
    }

    public boolean isInCombat(Player player) {
        if (player.hasPermission("customitems.bypass.combat")) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        Long combatEnd = combatPlayers.get(playerId);

        if (combatEnd == null) {
            return false;
        }

        if (System.currentTimeMillis() >= combatEnd) {
            combatPlayers.remove(playerId);
            return false;
        }

        return true;
    }

    public void exitCombat(Player player) {
        UUID playerId = player.getUniqueId();
        combatPlayers.remove(playerId);

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Player " + player.getName() + " exited combat");
        }
    }

    public long getRemainingCombatTime(Player player) {
        if (player.hasPermission("customitems.bypass.combat")) {
            return 0;
        }

        UUID playerId = player.getUniqueId();
        Long combatEnd = combatPlayers.get(playerId);

        if (combatEnd == null) {
            return 0;
        }

        long remaining = combatEnd - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public int getRemainingCombatSeconds(Player player) {
        return (int) Math.ceil(getRemainingCombatTime(player) / 1000.0);
    }

    private void applyCombatCaps(Player player) {
        if (!plugin.getConfigManager().isCombatCapEnabled()) {
            return;
        }

        // Apply custom item caps
        applyCustomItemCaps(player);

        // Apply regular item caps
        applyRegularItemCaps(player);
    }

    private void applyCustomItemCaps(Player player) {
        // Move your existing custom item cap logic here
        ItemManager itemManager = plugin.getItemManager();

        for (CustomItem customItem : itemManager.getItemsWithCombatCap()) {
            int cap = customItem.getCombatCap();
            if (cap <= 0) continue;

            // First pass: Count total items and collect slot information
            int totalAmount = 0;
            List<Integer> matchingSlots = new ArrayList<>();

            for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
                ItemStack item = player.getInventory().getItem(slot);
                if (item != null && customItem.isCustomItem(item)) {
                    totalAmount += item.getAmount();
                    matchingSlots.add(slot);
                }
            }

            // If total is within cap, no need to remove anything
            if (totalAmount <= cap) {
                continue;
            }

            // Calculate how many items to remove
            int excessAmount = totalAmount - cap;
            int removedCount = 0;

            // Second pass: Remove excess items from the END of inventory slots (reverse order)
            for (int i = matchingSlots.size() - 1; i >= 0 && removedCount < excessAmount; i--) {
                int slot = matchingSlots.get(i);
                ItemStack stack = player.getInventory().getItem(slot);

                if (stack == null) continue;

                int stackAmount = stack.getAmount();
                int toRemoveFromStack = Math.min(stackAmount, excessAmount - removedCount);

                if (toRemoveFromStack >= stackAmount) {
                    // Remove entire stack
                    player.getInventory().setItem(slot, null);
                    removedCount += stackAmount;
                } else {
                    // Reduce stack size
                    stack.setAmount(stackAmount - toRemoveFromStack);
                    player.getInventory().setItem(slot, stack);
                    removedCount += toRemoveFromStack;
                }
            }

            // Show message if items were removed
            if (removedCount > 0) {
                String message = plugin.getConfigManager().getMessage("combat.cap-applied",
                        "{item}", customItem.getName(),
                        "{cap}", String.valueOf(cap),
                        "{removed}", String.valueOf(removedCount));
                player.sendMessage(message);
            }
        }

        player.updateInventory();
    }


    private void applyRegularItemCaps(Player player) {
        if (!plugin.getConfigManager().isRegularItemCapsEnabled()) {
            return;
        }

        Map<Material, Integer> regularItemCaps = plugin.getConfigManager().getRegularItemCaps();

        for (Map.Entry<Material, Integer> entry : regularItemCaps.entrySet()) {
            Material material = entry.getKey();
            int cap = entry.getValue();

            // First pass: Count total items across ALL inventory slots
            int totalAmount = 0;
            List<Integer> matchingSlots = new ArrayList<>();

            for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
                ItemStack item = player.getInventory().getItem(slot);
                if (item != null && item.getType() == material) {
                    totalAmount += item.getAmount();
                    matchingSlots.add(slot);
                }
            }

            // If total is within cap, no need to remove anything
            if (totalAmount <= cap) {
                continue;
            }

            // Calculate how many items to remove
            int excessAmount = totalAmount - cap;
            int removedCount = 0;

            // Second pass: Remove excess items from the END of inventory slots
            for (int i = matchingSlots.size() - 1; i >= 0 && removedCount < excessAmount; i--) {
                int slot = matchingSlots.get(i);
                ItemStack stack = player.getInventory().getItem(slot);

                if (stack == null) continue;

                int stackAmount = stack.getAmount();
                int toRemoveFromStack = Math.min(stackAmount, excessAmount - removedCount);

                if (toRemoveFromStack >= stackAmount) {
                    // Remove entire stack
                    player.getInventory().setItem(slot, null);
                    removedCount += stackAmount;
                } else {
                    // Reduce stack size
                    stack.setAmount(stackAmount - toRemoveFromStack);
                    player.getInventory().setItem(slot, stack);
                    removedCount += toRemoveFromStack;
                }
            }

            // Show message if items were removed
            if (removedCount > 0) {
                String itemName = ChatUtils.formatItemName(material.name());
                String message = plugin.getConfigManager().getMessage("combat.regular-cap-applied",
                        "{item}", itemName,
                        "{cap}", String.valueOf(cap),
                        "{removed}", String.valueOf(removedCount));
                player.sendMessage(message);
            }
        }

        player.updateInventory();
    }


    public void cleanupExpiredCombat() {
        long currentTime = System.currentTimeMillis();
        combatPlayers.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
    }

    public void clearAll() {
        combatPlayers.clear();
    }

    public void cleanup() {
        combatPlayers.clear();
    }

    // Get all players currently in combat
    public Set<UUID> getPlayersInCombat() {
        Set<UUID> players = new HashSet<>();
        long currentTime = System.currentTimeMillis();

        for (Map.Entry<UUID, Long> entry : combatPlayers.entrySet()) {
            if (entry.getValue() > currentTime) {
                players.add(entry.getKey());
            }
        }

        return players;
    }

    // Check if item usage should be restricted due to combat caps
    public boolean canUseItemInCombat(Player player, CustomItem item) {
        if (!isInCombat(player) || !plugin.getConfigManager().isCombatCapEnabled()) {
            return true;
        }

        if (item.getCombatCap() <= 0) {
            return true; // No combat restriction
        }

        // Count current items in inventory
        int currentAmount = 0;
        for (ItemStack invItem : player.getInventory().getContents()) {
            if (invItem != null && item.isCustomItem(invItem)) {
                currentAmount += invItem.getAmount();
            }
        }

        return currentAmount <= item.getCombatCap();
    }
}