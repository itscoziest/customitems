package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
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
                100.0 // Max distance
        );

        Location strikeLocation;
        if (rayTrace != null && rayTrace.getHitBlock() != null) {
            strikeLocation = rayTrace.getHitBlock().getLocation().add(0.5, 1, 0.5);
        } else {
            // Strike 50 blocks in front if no block hit
            strikeLocation = player.getLocation().add(player.getLocation().getDirection().multiply(50));
            strikeLocation.setY(player.getWorld().getHighestBlockYAt(strikeLocation) + 1);
        }

        // Get damage from config
        // FIX: Use the specific "lightning-damage" if set, otherwise fallback to item generic damage
        double damage = item.getCustomDataDouble("lightning-damage", item.getDamage());
        if (damage <= 0) damage = 8.0;

        // FIX: Use strikeLightningEffect (Visual Only) instead of strikeLightning.
        // This prevents the vanilla lightning from dealing random extra damage/fire.
        strikeLocation.getWorld().strikeLightningEffect(strikeLocation);

        // Visuals
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);

        // Apply controlled damage
        final double finalDamage = damage;
        // FIX: target LivingEntity (Mobs + Players) instead of just Players
        strikeLocation.getWorld().getNearbyEntities(strikeLocation, 4, 4, 4).forEach(entity -> {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                ((LivingEntity) entity).damage(finalDamage, player);
            }
        });

        // Consume Item
        consumeItem(player, event);

        return true;
    }

    private void consumeItem(Player player, PlayerInteractEvent event) {
        if (event.getItem() == null) return;

        if (event.getItem().getAmount() > 1) {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
        } else {
            player.getInventory().setItem(event.getHand(), null);
        }
    }
}