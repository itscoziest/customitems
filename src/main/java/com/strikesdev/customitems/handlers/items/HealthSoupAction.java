package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.handlers.ItemAction;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.Particle;

public class HealthSoupAction implements ItemAction {
    private final CustomItems plugin;

    public HealthSoupAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        double healAmount = item.getCustomDataDouble("heal-amount", 6.0);
        double currentHealth = player.getHealth();
        double maxHealth = player.getMaxHealth();

        // Only use if needed
        if (currentHealth >= maxHealth) {
            return false;
        }

        double newHealth = Math.min(maxHealth, currentHealth + healAmount);
        player.setHealth(newHealth);

        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);
        player.spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 5, 0.5, 0.5, 0.5);

        // Consume item
        if (event.getItem().getAmount() > 1) {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
        } else {
            player.getInventory().setItem(event.getHand(), null);
        }

        return true;
    }
}