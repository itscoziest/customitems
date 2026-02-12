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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.Event.Result;

public class PlayerInteractListener implements Listener {

    private final CustomItems plugin;
    private final ItemHandler itemHandler;

    public PlayerInteractListener(CustomItems plugin) {
        this.plugin = plugin;
        this.itemHandler = new ItemHandler(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == null) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            ItemStack mainHandItem = player.getInventory().getItemInMainHand();
            if (plugin.getItemManager().isCustomItem(mainHandItem)) {
                return;
            }
        }

        if (item == null) return;

        CustomItem customItem = plugin.getItemManager().getCustomItem(item);
        if (customItem == null) return;

        // Permission Check
        if (!customItem.getPermission().isEmpty() && !player.hasPermission(customItem.getPermission())) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            event.setUseItemInHand(Result.DENY);
            event.setCancelled(true);
            return;
        }

        // Region Check
        RegionManager regionManager = plugin.getRegionManager();
        if (regionManager != null && !regionManager.canUseItemInRegion(player, customItem, player.getLocation())) {
            player.sendMessage(regionManager.getRegionDenialMessage(customItem));
            event.setUseItemInHand(Result.DENY);
            event.setCancelled(true);
            return;
        }

        // Cooldown Check
        CooldownManager cooldownManager = plugin.getCooldownManager();
        if (cooldownManager.hasCooldown(player, customItem.getId())) {
            int remaining = cooldownManager.getRemainingCooldownSeconds(player, customItem.getId());
            String message = plugin.getConfigManager().getMessage("item.on-cooldown",
                    "{item}", customItem.getName(),
                    "{time}", String.valueOf(remaining));
            player.sendMessage(message);

            plugin.getActionBarManager().sendCooldownMessage(player, customItem.getName(), remaining);
            event.setUseItemInHand(Result.DENY);
            event.setCancelled(true);
            return;
        }

        // Combat Check
        CombatManager combatManager = plugin.getCombatManager();
        if (!combatManager.canUseItemInCombat(player, customItem)) {
            String message = plugin.getConfigManager().getMessage("combat.cannot-use-item",
                    "{item}", customItem.getName());
            player.sendMessage(message);
            event.setUseItemInHand(Result.DENY);
            event.setCancelled(true);
            return;
        }

        // Execute Item Action
        boolean success = itemHandler.handleItemUse(player, customItem, event);

        if (success) {
            if (customItem.getCooldown() > 0) {
                cooldownManager.setCooldown(player, customItem.getId(), customItem.getCooldown());
                plugin.getActionBarManager().sendCooldownMessage(player, customItem.getName(), customItem.getCooldown());

                // --- FIX: VANILLA COOLOWN ANIMATION ---
                // This adds the white overlay to the hotbar item
                player.setCooldown(customItem.getMaterial(), customItem.getCooldown() * 20);
            }
            event.setCancelled(true);
        }
    }
}