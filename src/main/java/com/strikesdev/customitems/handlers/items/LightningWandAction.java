package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.RayTraceResult;

public class LightningWandAction implements ItemAction {
    private final CustomItems plugin;

    public LightningWandAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        // Ray trace to find where the player is looking
        RayTraceResult rayTrace = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(),
                player.getLocation().getDirection(),
                100.0 // Max distance of 100 blocks
        );

        Location strikeLocation;
        if (rayTrace != null && rayTrace.getHitBlock() != null) {
            // Strike at the block they're looking at
            strikeLocation = rayTrace.getHitBlock().getLocation().add(0, 1, 0);
        } else {
            // Strike 50 blocks in front if no block hit
            strikeLocation = player.getLocation().add(player.getLocation().getDirection().multiply(50));
            strikeLocation.setY(player.getWorld().getHighestBlockYAt(strikeLocation) + 1);
        }

        // Get damage from config or use default
        double damage = item.getCustomDataDouble("lightning-damage", 8.0);

        // Strike lightning at the location
        strikeLocation.getWorld().strikeLightning(strikeLocation);

        // Add visual and sound effects
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);

        // Damage nearby entities (optional - lightning already does damage)
        strikeLocation.getWorld().getNearbyEntities(strikeLocation, 3, 3, 3).forEach(entity -> {
            if (entity instanceof Player && !entity.equals(player)) {
                ((Player) entity).damage(damage, player);
            }
        });

        if (event.getItem() != null) {
            if (event.getItem().getAmount() > 1) {
                event.getItem().setAmount(event.getItem().getAmount() - 1);
            } else {
                if (player.getInventory().getItemInMainHand().equals(event.getItem())) {
                    player.getInventory().setItemInMainHand(null);
                } else if (player.getInventory().getItemInOffHand().equals(event.getItem())) {
                    player.getInventory().setItemInOffHand(null);
                }
            }
        }

        return true;
    }
}