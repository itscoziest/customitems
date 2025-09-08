package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

public class GolemFistsAction implements ItemAction {
    private final CustomItems plugin;

    public GolemFistsAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        double radius = item.getRadius() > 0 ? item.getRadius() : 3.0;
        double force = item.getRange() > 0 ? item.getRange() : 7.0;

        // Find ALL nearby entities and launch them upward
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            // Skip the player who used the item
            if (entity.equals(player)) continue;

            // Target players and living entities (mobs)
            if (entity instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) entity;

                // Calculate launch vector (straight up)
                Vector launch = new Vector(0, force / 3.0, 0);
                target.setVelocity(launch);

                // Effects on target
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.8f);
                target.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, target.getLocation(), 1);
            }
        }

        // Player effects
        player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_STEP, 1.0f, 0.5f);
        player.spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 10, 1, 0.5, 1, 0.1);

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