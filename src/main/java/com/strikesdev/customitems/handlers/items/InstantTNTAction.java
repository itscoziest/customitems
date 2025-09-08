package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

// 🧨 Instant TNT Action
public class InstantTNTAction implements ItemAction {
    private final CustomItems plugin;

    public InstantTNTAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return false;

        Location loc = clickedBlock.getRelative(event.getBlockFace()).getLocation();

        // Create explosion immediately without damaging owner
        double damage = item.getDamage() > 0 ? item.getDamage() : 4.0;
        loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(),
                (float) damage, false, true, player);

        // Consume item
        if (event.getItem().getAmount() > 1) {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
        } else {
            player.getInventory().setItem(event.getHand(), null);
        }

        return true;
    }
}