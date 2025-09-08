package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;

// 🌬️ Super Wind Charge Action
public class SuperWindChargeAction implements ItemAction {
    private final CustomItems plugin;

    public SuperWindChargeAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        // Get configurable values
        double range = item.getRadius() > 0 ? item.getRadius() : 8.0;
        double launchPower = item.getCustomDataDouble("launch-power", 4.5);
        double upwardForce = item.getCustomDataDouble("upward-force", 0.6);

        // Launch wind charge projectile
        WindCharge windCharge = player.launchProjectile(WindCharge.class);
        windCharge.setMetadata("custom_item", new FixedMetadataValue(plugin, "super_wind_charge"));
        windCharge.setMetadata("range", new FixedMetadataValue(plugin, range));
        windCharge.setMetadata("launch_power", new FixedMetadataValue(plugin, launchPower));
        windCharge.setMetadata("upward_force", new FixedMetadataValue(plugin, upwardForce));

        // Sound effect
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.8f);

        // Launch particles
        player.spawnParticle(Particle.GUST, player.getLocation(), 15, 0.5, 0.5, 0.5, 0.1);

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