package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

public class FireballAction implements ItemAction {

    private final CustomItems plugin;

    public FireballAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        Fireball fireball = player.launchProjectile(Fireball.class);
        fireball.setYield(0); // No block damage
        fireball.setIsIncendiary(false); // No fire

        double speed = item.getCustomDataDouble("projectile-speed", 1.5);
        Vector velocity = player.getLocation().getDirection().multiply(speed);
        fireball.setVelocity(velocity);

        double damage = item.getDamage() > 0 ? item.getDamage() : 5.0;
        fireball.setMetadata("custom_item", new FixedMetadataValue(plugin, "fireball"));
        fireball.setMetadata("damage", new FixedMetadataValue(plugin, damage));

        player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1.0f, 1.2f);

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