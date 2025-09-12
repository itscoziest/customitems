package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HastePotionAction implements ItemAction {
    private final CustomItems plugin;

    public HastePotionAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        // Apply haste effect
        if (!item.getEffects().isEmpty()) {
            item.getEffects().forEach(player::addPotionEffect);
        } else {
            // Default haste 2 effect for 8 seconds if not configured
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.FAST_DIGGING,
                    (item.getDuration() > 0 ? item.getDuration() : 8) * 20,
                    1 // Amplifier 1 = Haste II
            ));
        }

        // Play drinking sound and particles
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);

        // Add visual effects
        Location loc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc, 8, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, loc, 15, 0.3, 0.3, 0.3, 0.1);

        // Consume item
        if (event.getItem().getAmount() > 1) {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
        } else {
            player.getInventory().setItem(event.getHand(), null);
        }

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