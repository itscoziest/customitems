package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class CreeperAction implements ItemAction {
    private final CustomItems plugin;

    public CreeperAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return false;

        Location spawnLoc = clickedBlock.getRelative(event.getBlockFace()).getLocation();

        // Spawn creeper
        Creeper creeper = (Creeper) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.CREEPER);
        creeper.setMetadata("custom_creeper", new FixedMetadataValue(plugin, true));
        creeper.setMetadata("owner", new FixedMetadataValue(plugin, player.getUniqueId()));
        creeper.setMaxFuseTicks(Integer.MAX_VALUE); // Prevent normal explosion

        double damage = item.getDamage() > 0 ? item.getDamage() : 4.0;
        int moveDuration = item.getDuration() > 0 ? item.getDuration() : 3;

        // Find nearest enemy player (not the owner)
        Player target = findNearestEnemy(player, spawnLoc, 15.0);
        if (target != null) {
            creeper.setTarget(target);
        }

        // Smoother movement and explosion timer
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!creeper.isValid() || creeper.isDead()) {
                    this.cancel();
                    return;
                }

                ticks++;

                if (ticks >= moveDuration * 20) {
                    // Time to explode
                    Location explodeLoc = creeper.getLocation();

                    String ownerUUID = creeper.getMetadata("owner").get(0).asString();

                    creeper.remove();

                    // Create explosion without damaging owner
                    explodeLoc.getWorld().createExplosion(explodeLoc.getX(), explodeLoc.getY(), explodeLoc.getZ(),
                            (float) damage, false, false, Bukkit.getPlayer(java.util.UUID.fromString(ownerUUID)));

                    // Create explosion
                    explodeLoc.getWorld().createExplosion(explodeLoc, (float) damage, false, true);

                    this.cancel();
                    return;
                }

                // Smooth movement towards target every 20 ticks (1 second)
                if (ticks % 20 == 0 && target != null && target.isOnline()) {
                    creeper.setTarget(target);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);

        // Visual effects during movement
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!creeper.isValid() || creeper.isDead()) {
                    this.cancel();
                    return;
                }

                // Sparks effect
                creeper.getWorld().spawnParticle(Particle.FIREWORKS_SPARK,
                        creeper.getLocation().add(0, 1, 0), 3, 0.2, 0.5, 0.2, 0.05);
            }
        }.runTaskTimer(plugin, 1L, 10L);

        // Consume item
        consumeItem(player, event);
        return true;
    }

    private Player findNearestEnemy(Player owner, Location center, double range) {
        Player closest = null;
        double closestDistance = range;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.equals(owner) || !player.getWorld().equals(center.getWorld())) {
                continue;
            }

            double distance = player.getLocation().distance(center);
            if (distance < closestDistance) {
                closest = player;
                closestDistance = distance;
            }
        }

        return closest;
    }

    private void consumeItem(Player player, PlayerInteractEvent event) {
        if (event.getItem().getAmount() > 1) {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
        } else {
            player.getInventory().setItem(event.getHand(), null);
        }
    }
}