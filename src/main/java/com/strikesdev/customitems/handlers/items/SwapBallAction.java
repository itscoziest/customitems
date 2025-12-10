package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

public class SwapBallAction implements ItemAction {
    private final CustomItems plugin;

    public SwapBallAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        // Launch a Snowball instead of EnderPearl to prevent "Combat Teleport" blocks
        Snowball projectile = player.launchProjectile(Snowball.class);

        // Disguise the snowball as an Ender Pearl visually
        projectile.setItem(new ItemStack(Material.ENDER_PEARL));

        // Set metadata for the listener
        projectile.setMetadata("custom_item", new FixedMetadataValue(plugin, "swap_ball"));
        projectile.setMetadata("caster_uuid", new FixedMetadataValue(plugin, player.getUniqueId().toString()));

        // Sound effect
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 1.0f, 1.0f);

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