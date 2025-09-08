package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class DynamiteAction implements ItemAction {
    private final CustomItems plugin;

    public DynamiteAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        // Throw as item entity
        Location throwLoc = player.getEyeLocation();
        Vector velocity = throwLoc.getDirection().multiply(1.2);

        Item thrownItem = player.getWorld().dropItem(throwLoc, event.getItem().clone());
        thrownItem.setVelocity(velocity);
        thrownItem.setPickupDelay(Integer.MAX_VALUE); // Cannot be picked up

        // Set metadata
        double damage = item.getDamage() > 0 ? item.getDamage() : 4.0;
        thrownItem.setMetadata("custom_item", new FixedMetadataValue(plugin, "dynamite"));
        thrownItem.setMetadata("damage", new FixedMetadataValue(plugin, damage));
        thrownItem.setMetadata("thrower", new FixedMetadataValue(plugin, player.getUniqueId()));

        // Schedule explosion after hitting ground or 3 seconds
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!thrownItem.isValid() || thrownItem.isDead()) {
                    this.cancel();
                    return;
                }

                ticks++;

                // Check if hit ground or max time reached
                if (thrownItem.isOnGround() || ticks >= 60) { // 3 seconds max
                    Location explodeLoc = thrownItem.getLocation();
                    thrownItem.remove();

                    // Create explosion
                    explodeLoc.getWorld().createExplosion(explodeLoc, (float) damage, false, true);

                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);

        // Sound effect
        player.playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 1.0f, 1.5f);

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