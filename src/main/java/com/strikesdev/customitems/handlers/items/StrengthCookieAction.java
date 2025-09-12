package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class StrengthCookieAction implements ItemAction {
    private final CustomItems plugin;

    public StrengthCookieAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        // Apply strength effect
        if (!item.getEffects().isEmpty()) {
            item.getEffects().forEach(player::addPotionEffect);
        } else {
            // Default strength 3 effect for 8 seconds if not configured
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.INCREASE_DAMAGE,
                    (item.getDuration() > 0 ? item.getDuration() : 8) * 20, // Convert seconds to ticks
                    2 // Amplifier 2 = Strength III (0-based indexing)
            ));
        }

        // Play eating sound and particles
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);

        // Add some visual effects
        Location loc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.HEART, loc, 5, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.CRIT, loc, 10, 0.3, 0.3, 0.3, 0.1);

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