package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

// 💨 Smoke Bomb Action - Heavy Smoke Coverage
public class SmokeBombAction implements ItemAction {
    private final CustomItems plugin;

    public SmokeBombAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        Location smokeLoc = player.getLocation().add(0, 1, 0);
        int duration = item.getDuration() > 0 ? item.getDuration() : 10;
        double radius = item.getRadius() > 0 ? item.getRadius() : 5.0; // Increased radius

        // Start heavy smoke effect
        createHeavySmokeEffect(smokeLoc, duration, radius);

        // Sound effect
        player.playSound(player.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1.0f, 0.8f);

        // Consume item
        consumeItem(player, event);
        return true;
    }

    private void createHeavySmokeEffect(Location center, int duration, double radius) {
        new BukkitRunnable() {
            int ticks = 0;
            int maxTicks = duration * 20;

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    this.cancel();
                    return;
                }

                // Heavy particle effects for maximum coverage
                center.getWorld().spawnParticle(Particle.SMOKE_LARGE, center, 80, radius, 2, radius, 0.2);
                center.getWorld().spawnParticle(Particle.CLOUD, center, 60, radius, 2, radius, 0.15);
                center.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center, 40, radius, 3, radius, 0.1);

                // Fireworks every 2 seconds (reduce frequency to prevent lag)
                if (ticks % 40 == 0) { // Every 2 seconds
                    for (int i = 0; i < 4; i++) { // 4 fireworks per burst
                        double x = (Math.random() - 0.5) * radius * 2;
                        double z = (Math.random() - 0.5) * radius * 2;
                        double y = Math.random() * 3;

                        Location fireworkLoc = center.clone().add(x, y, z);
                        spawnSmokeFirework(fireworkLoc);
                    }
                }

                ticks += 10; // Update every 0.5 seconds
            }
        }.runTaskTimer(plugin, 0L, 10L); // Run every 0.5 seconds
    }

    private void spawnSmokeFirework(Location location) {
        Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
        FireworkMeta meta = firework.getFireworkMeta();

        // Heavy gray smoke effect
        FireworkEffect effect = FireworkEffect.builder()
                .withColor(Color.GRAY, Color.fromRGB(64, 64, 64))
                .withFade(Color.WHITE, Color.fromRGB(128, 128, 128))
                .with(FireworkEffect.Type.BURST)
                .withFlicker()
                .withTrail()
                .build();

        meta.addEffect(effect);
        meta.setPower(0);
        firework.setFireworkMeta(meta);

        // Mark as no-damage firework
        firework.setMetadata("no_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, true));

        // Immediate explosion
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (firework.isValid() && !firework.isDead()) {
                firework.detonate();
            }
        }, 1L);

        plugin.getEffectManager().registerTemporaryEntity(firework, 3);
    }

    private void consumeItem(Player player, PlayerInteractEvent event) {
        if (event.getItem().getAmount() > 1) {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
        } else {
            player.getInventory().setItem(event.getHand(), null);
        }
    }
}