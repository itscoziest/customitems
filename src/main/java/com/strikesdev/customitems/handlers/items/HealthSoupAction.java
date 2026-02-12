package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.handlers.ItemAction;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class HealthSoupAction implements ItemAction {
    private final CustomItems plugin;

    public HealthSoupAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        if (player.getHealth() >= player.getMaxHealth()) {
            return false;
        }

        double healAmount = item.getCustomDataDouble("heal-amount", 6.0); // 3 hearts
        double newHealth = Math.min(player.getMaxHealth(), player.getHealth() + healAmount);

        player.setHealth(newHealth);
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);

        // Replace soup with bowl
        if (event.getItem().getAmount() > 1) {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
            player.getInventory().addItem(new ItemStack(Material.BOWL));
        } else {
            player.getInventory().setItem(event.getHand(), new ItemStack(Material.BOWL));
        }

        return true;
    }
}