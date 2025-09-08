package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;

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

        // Launch fireball
        Fireball fireball = player.launchProjectile(Fireball.class);
        fireball.setYield(0);
        fireball.setIsIncendiary(false);

        // Set custom speed
        double speed = item.getCustomDataDouble("projectile-speed", 1.0);
        fireball.setVelocity(fireball.getVelocity().multiply(speed));

        // Set custom damage
        double damage = item.getDamage() > 0 ? item.getDamage() : 5.0;
        fireball.setMetadata("custom_item", new FixedMetadataValue(plugin, "fireball"));
        fireball.setMetadata("damage", new FixedMetadataValue(plugin, damage));

        // Sound effect
        player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1.0f, 1.2f);

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