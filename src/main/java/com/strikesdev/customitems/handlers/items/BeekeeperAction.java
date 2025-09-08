package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

// 🐝 Beekeeper Action
public class BeekeeperAction implements ItemAction {
    private final CustomItems plugin;

    public BeekeeperAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        Location center = player.getLocation();
        int radius = (int) (item.getRadius() > 0 ? item.getRadius() : 3);

        // Turn ground into honey blocks and store original blocks
        Map<Location, Material> originalBlocks = createHoneyArea(center, radius, 7, item);

        // Spawn angry bees
        spawnAngryBees(player, center, 3, originalBlocks, item);

        // Consume item
        consumeItem(player, event);
        return true;
    }

    private Map<Location, Material> createHoneyArea(Location center, int radius, int duration, CustomItem item) {
        World world = center.getWorld();
        Map<Location, Material> originalBlocks = new HashMap<>();

        // Get configurable block type
        String blockTypeString = item.getCustomDataString("honey-block-type", "HONEY_BLOCK");
        Material honeyBlockType = Material.matchMaterial(blockTypeString);
        if (honeyBlockType == null) {
            honeyBlockType = Material.HONEY_BLOCK;
        }

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x*x + z*z <= radius*radius) {
                    Location blockLoc = center.clone().add(x, -1, z);

                    // Store original block type
                    Material original = blockLoc.getBlock().getType();
                    originalBlocks.put(blockLoc.clone(), original);

                    // Set to configured honey block type
                    blockLoc.getBlock().setType(honeyBlockType);
                }
            }
        }

        // Schedule restoration of original blocks
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<Location, Material> entry : originalBlocks.entrySet()) {
                    entry.getKey().getBlock().setType(entry.getValue());
                }
            }
        }.runTaskLater(plugin, duration * 20L);

        return originalBlocks;
    }

    private void spawnAngryBees(Player owner, Location center, int count, Map<Location, Material> originalBlocks, CustomItem item) {
        int beeCount = item.getCustomDataInt("bee-count", 3);
        int beeDuration = item.getCustomDataInt("bee-duration", 30);

        for (int i = 0; i < beeCount; i++) {
            Location spawnLoc = center.clone().add(
                    (Math.random() - 0.5) * 6, 1, (Math.random() - 0.5) * 6);

            Bee bee = (Bee) center.getWorld().spawnEntity(spawnLoc, EntityType.BEE);
            bee.setAnger(Integer.MAX_VALUE); // Permanent anger
            bee.setMetadata("beekeeper_owner", new FixedMetadataValue(plugin, owner.getUniqueId()));
            bee.setMetadata("beekeeper_bee", new FixedMetadataValue(plugin, true));

            // Find and attack closest enemy
            Player target = findClosestEnemy(owner, center, 15.0);
            if (target != null) {
                bee.setTarget(target);
            }

            // Task to keep bees attacking continuously
            new BukkitRunnable() {
                int ticks = 0;
                int maxTicks = beeDuration * 20;

                @Override
                public void run() {
                    if (!bee.isValid() || bee.isDead() || ticks >= maxTicks) {
                        if (bee.isValid()) bee.remove();
                        this.cancel();
                        return;
                    }

                    // Every 10 ticks (0.5 seconds), ensure bee is attacking
                    if (ticks % 10 == 0) {
                        bee.setAnger(Integer.MAX_VALUE);

                        // Find target if none or target is gone
                        if (bee.getTarget() == null || !bee.getTarget().isValid() ||
                                !(bee.getTarget() instanceof Player) || !((Player)bee.getTarget()).isOnline()) {

                            Player newTarget = findClosestEnemy(owner, bee.getLocation(), 20.0);
                            if (newTarget != null) {
                                bee.setTarget(newTarget);
                            }
                        }

                        // If close to target, force attack
                        if (bee.getTarget() instanceof Player) {
                            Player targetPlayer = (Player) bee.getTarget();
                            if (bee.getLocation().distance(targetPlayer.getLocation()) < 3.0) {
                                // Direct damage to ensure continuous attacking
                                targetPlayer.damage(1.0, bee);
                                targetPlayer.spawnParticle(Particle.CRIT, targetPlayer.getLocation().add(0, 1, 0), 5);
                            }
                        }
                    }

                    ticks++;
                }
            }.runTaskTimer(plugin, 1L, 1L);
        }
    }

    private Player findClosestEnemy(Player owner, Location center, double range) {
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