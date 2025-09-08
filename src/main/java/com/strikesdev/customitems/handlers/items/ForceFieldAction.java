package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class ForceFieldAction implements ItemAction {
    private final CustomItems plugin;

    public ForceFieldAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        Location center = player.getLocation();
        int radius = (int) (item.getRadius() > 0 ? item.getRadius() : 3);
        int duration = item.getDuration() > 0 ? item.getDuration() : 10;

        // Create glass sphere
        createGlassSphere(center, radius, duration);

        // Consume item
        if (event.getItem().getAmount() > 1) {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
        } else {
            player.getInventory().setItem(event.getHand(), null);
        }

        return true;
    }

    private void createGlassSphere(Location center, int radius, int duration) {
        World world = center.getWorld();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x*x + y*y + z*z);

                    // Only place blocks on the surface of the sphere
                    if (distance >= radius - 0.5 && distance <= radius + 0.5) {
                        Location blockLoc = center.clone().add(x, y, z);
                        Block block = world.getBlockAt(blockLoc);

                        if (block.getType() == Material.AIR) {
                            block.setType(Material.GLASS);

                            // Register temporary block
                            plugin.getEffectManager().registerTemporaryBlock(blockLoc, duration);
                        }
                    }
                }
            }
        }
    }
}