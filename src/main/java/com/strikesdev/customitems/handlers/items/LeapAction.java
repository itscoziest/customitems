package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;
import org.bukkit.metadata.FixedMetadataValue;


public class LeapAction implements ItemAction {
    private final CustomItems plugin;

    public LeapAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        double range = item.getRange() > 0 ? item.getRange() : 7.0;

        // Get player's looking direction
        Vector direction = player.getLocation().getDirection();
        direction.setY(0.5);
        direction.normalize();
        direction.multiply(range / 3.0);

        // Launch player
        player.setVelocity(direction);

        // Add fall damage protection
        player.setMetadata("leap_protection", new FixedMetadataValue(plugin, System.currentTimeMillis() + 5000)); // 5 seconds

        // Visual and sound effects
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);
        player.spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);

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