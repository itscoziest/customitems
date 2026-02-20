package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.handlers.ItemAction;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StormItemAction implements ItemAction {
    private final CustomItems plugin;
    private final Random random = new Random();

    public StormItemAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        double radius = item.getCustomDataDouble("radius", 8.0);

        // FIX: Calculate duration first and store in a FINAL variable for the inner class
        int configDuration = item.getDuration();
        final int durationSeconds = (configDuration <= 0) ? 5 : configDuration;

        double damage = item.getCustomDataDouble("damage", 4.0);
        int rainHeight = item.getCustomDataInt("rain-height", 10);

        Location center = player.getLocation();

        // Consume item
        if (event.getItem().getAmount() > 1) {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
        } else {
            player.getInventory().setItem(event.getHand(), null);
        }

        player.sendMessage("§bThe storm has begun!");

        // Run the storm
        new BukkitRunnable() {
            int ticks = 0;
            // Now using the final variable
            final int maxTicks = durationSeconds * 20;
            final List<LivingEntity> activeStormMobs = new ArrayList<>();

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    // Cleanup remaining mobs
                    for (LivingEntity entity : activeStormMobs) {
                        if (entity.isValid()) {
                            entity.getWorld().spawnParticle(Particle.CLOUD, entity.getLocation(), 5);
                            entity.remove();
                        }
                    }
                    this.cancel();
                    return;
                }

                // Spawn cats/dogs every 5 ticks
                if (ticks % 5 == 0) {
                    double offsetX = (random.nextDouble() * radius * 2) - radius;
                    double offsetZ = (random.nextDouble() * radius * 2) - radius;
                    Location spawnLoc = center.clone().add(offsetX, rainHeight, offsetZ);

                    LivingEntity stormMob;
                    // Randomly spawn cat or wolf
                    if (random.nextBoolean()) {
                        stormMob = center.getWorld().spawn(spawnLoc, Cat.class);
                    } else {
                        stormMob = center.getWorld().spawn(spawnLoc, Wolf.class);
                    }

                    stormMob.setInvulnerable(true); // Don't take fall damage immediately
                    activeStormMobs.add(stormMob);
                }

                // Monitor active mobs for impact
                activeStormMobs.removeIf(mob -> {
                    if (!mob.isValid() || mob.isDead()) return true;

                    // Check if on ground
                    if (mob.isOnGround()) {
                        mob.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, mob.getLocation(), 1);
                        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);

                        // Damage nearby entities
                        for (Entity nearby : mob.getNearbyEntities(2, 2, 2)) {
                            if (nearby instanceof LivingEntity && !nearby.equals(player)) {
                                ((LivingEntity) nearby).damage(damage, player);
                            }
                        }
                        mob.remove();
                        return true;
                    }
                    return false;
                });

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }
}