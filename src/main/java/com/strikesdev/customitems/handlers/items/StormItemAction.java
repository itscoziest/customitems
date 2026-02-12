package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.handlers.ItemAction;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.concurrent.ThreadLocalRandom;

public class StormItemAction implements ItemAction {
    private final CustomItems plugin;

    public StormItemAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        double radius = item.getCustomDataDouble("radius", 10.0);
        int durationSeconds = item.getCustomDataInt("duration", 5);

        Location center = player.getLocation();
        player.getWorld().playSound(center, Sound.WEATHER_RAIN, 1.0f, 1.0f);
        player.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = durationSeconds * 20;

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    this.cancel();
                    return;
                }

                // Spawn 2-3 entities per tick
                for (int i = 0; i < 3; i++) {
                    spawnFallingAnimal(center, radius);
                }

                if (ticks % 10 == 0) {
                    player.getWorld().spawnParticle(Particle.CLOUD, center.clone().add(0, 10, 0), 50, radius, 1, radius, 0);
                }

                ticks += 2; // Run every 2 ticks logic
            }
        }.runTaskTimer(plugin, 0L, 2L);

        // Consume item
        if (event.getItem().getAmount() > 1) {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
        } else {
            player.getInventory().setItem(event.getHand(), null);
        }

        return true;
    }

    private void spawnFallingAnimal(Location center, double radius) {
        double xOffset = (ThreadLocalRandom.current().nextDouble() * radius * 2) - radius;
        double zOffset = (ThreadLocalRandom.current().nextDouble() * radius * 2) - radius;

        Location spawnLoc = center.clone().add(xOffset, 12, zOffset);

        Entity animal;
        if (ThreadLocalRandom.current().nextBoolean()) {
            animal = center.getWorld().spawn(spawnLoc, Cat.class);
        } else {
            animal = center.getWorld().spawn(spawnLoc, Wolf.class);
        }

        // Remove animal after 2 seconds (when they hit ground)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (animal.isValid()) {
                    animal.getWorld().spawnParticle(Particle.CLOUD, animal.getLocation(), 5);
                    animal.remove();
                }
            }
        }.runTaskLater(plugin, 40L);
    }
}