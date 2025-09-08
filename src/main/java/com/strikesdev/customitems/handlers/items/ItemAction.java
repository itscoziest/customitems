package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public interface ItemAction {

    /**
     * Execute the item action
     * @param player The player using the item
     * @param item The custom item being used
     * @param event The interaction event
     * @return true if the action was successful and the event should be cancelled
     */
    boolean execute(Player player, CustomItem item, PlayerInteractEvent event);
}