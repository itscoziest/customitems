package com.strikesdev.customitems.listeners;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.handlers.ItemHandler;
import com.strikesdev.customitems.managers.*;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractListener implements Listener {

    private final CustomItems plugin;
    private final ItemHandler itemHandler;

    public PlayerInteractListener(CustomItems plugin) {
        this.plugin = plugin;
        this.itemHandler = new ItemHandler(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        // Check if it's a custom item
        CustomItem customItem = plugin.getItemManager().getCustomItem(item);
        if (customItem == null) return;

        // Check permissions
        if (!customItem.getPermission().isEmpty() && !player.hasPermission(customItem.getPermission())) {
            String message = plugin.getConfigManager().getMessage("no-permission");
            player.sendMessage(message);
            event.setCancelled(true);
            return;
        }

        // Check region restrictions
        RegionManager regionManager = plugin.getRegionManager();
        if (regionManager != null && !regionManager.canUseItemInRegion(player, customItem, player.getLocation())) {
            String message = regionManager.getRegionDenialMessage(customItem);
            player.sendMessage(message);
            event.setCancelled(true);
            return;
        }

        // Check cooldown
        CooldownManager cooldownManager = plugin.getCooldownManager();
        if (cooldownManager.hasCooldown(player, customItem.getId())) {
            int remaining = cooldownManager.getRemainingCooldownSeconds(player, customItem.getId());
            String message = plugin.getConfigManager().getMessage("item.on-cooldown",
                    "{item}", customItem.getName(),
                    "{time}", String.valueOf(remaining));
            player.sendMessage(message);

            // Show action bar cooldown
            ActionBarManager actionBarManager = plugin.getActionBarManager();
            actionBarManager.sendCooldownMessage(player, customItem.getName(), remaining);

            event.setCancelled(true);
            return;
        }

        // Check combat restrictions
        CombatManager combatManager = plugin.getCombatManager();
        if (!combatManager.canUseItemInCombat(player, customItem)) {
            String message = plugin.getConfigManager().getMessage("combat.cannot-use-item",
                    "{item}", customItem.getName());
            player.sendMessage(message);
            event.setCancelled(true);
            return;
        }

        // Handle the item usage
        boolean success = itemHandler.handleItemUse(player, customItem, event);

        if (success) {
            // Set cooldown
            if (customItem.getCooldown() > 0) {
                cooldownManager.setCooldown(player, customItem.getId(), customItem.getCooldown());

                // Show action bar cooldown
                ActionBarManager actionBarManager = plugin.getActionBarManager();
                actionBarManager.sendCooldownMessage(player, customItem.getName(), customItem.getCooldown());
            }

            // Cancel the event to prevent normal item behavior
            event.setCancelled(true);
        }


    }

}