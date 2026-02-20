package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.handlers.ItemAction;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class FireSwordAction implements ItemAction {
    private final CustomItems plugin;

    public FireSwordAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        // Fire sword is passive - effects happen on hit in EntityListener
        return false;
    }
}