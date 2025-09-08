package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;

public class VampireSwordAction implements ItemAction {
    private final CustomItems plugin;

    public VampireSwordAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        // Vampire sword is passive - healing happens on kill in EntityListener
        // This method won't be called for normal sword usage
        return false; // Don't consume or cancel the event
    }
}