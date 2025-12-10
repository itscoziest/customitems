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

        // Apply effects
        if (!item.getEffects().isEmpty()) {
            item.getEffects().forEach(player::addPotionEffect);
        } else {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.INCREASE_DAMAGE,
                    (item.getDuration() > 0 ? item.getDuration() : 8) * 20,
                    1 // Strength II
            ));
        }

        // Sound & Visuals
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
        Location loc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.HEART, loc, 5, 0.5, 0.5, 0.5, 0.1);

        // FIX: You had the consumption code pasted TWICE here in your original file.
        // I have removed the duplicate. Now it only eats 1.
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