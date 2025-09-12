package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class ExplosiveCrossbowAction implements ItemAction {
    private final CustomItems plugin;

    public ExplosiveCrossbowAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        // Don't interfere with crossbow mechanics on right-click
        // Consumption happens when arrow is shot (handled in EntityShootBowEvent)
        return false; // Let vanilla crossbow mechanics work
    }
}