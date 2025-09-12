package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class GraveDiggerAction implements ItemAction {
    private final CustomItems plugin;

    public GraveDiggerAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        // Get settings from config
        int duration = item.getCustomDataInt("digger-duration", 100); // 5 seconds default
        boolean dropItems = item.getCustomDataBoolean("drop-items", false);

        // Launch a fireball as the digger projectile
        Fireball digger = player.launchProjectile(Fireball.class);

        // Configure the fireball
        digger.setDirection(player.getLocation().getDirection());
        digger.setVelocity(player.getLocation().getDirection().multiply(0.5)); // Smooth movement
        digger.setYield(0f); // No explosion damage
        digger.setIsIncendiary(false); // No fire

        // Add metadata to identify it as a grave digger
        digger.setMetadata("grave_digger", new FixedMetadataValue(plugin, true));
        digger.setMetadata("digger_owner", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        digger.setMetadata("drop_items", new FixedMetadataValue(plugin, dropItems));

        // Create the digging task
        createDiggingTask(digger, duration);

        // Effects
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 2.0f);
        player.getLocation().getWorld().spawnParticle(Particle.SMOKE_LARGE,
                player.getEyeLocation(), 10, 0.5, 0.5, 0.5, 0.1);

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

    private void createDiggingTask(Fireball digger, int duration) {
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!digger.isValid()) {
                return;
            }

            // Dig blocks in 3x3 area around the fireball
            digBlocks(digger);

            // Visual effects
            digger.getWorld().spawnParticle(Particle.BLOCK_CRACK,
                    digger.getLocation(), 10, 0.8, 0.8, 0.8, 0.1,
                    Material.STONE.createBlockData());
            digger.getWorld().spawnParticle(Particle.SMOKE_NORMAL,
                    digger.getLocation(), 3, 0.3, 0.3, 0.3, 0.02);

        }, 0L, 4L).getTaskId();

        // Remove the digger after duration
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getScheduler().cancelTask(taskId);
            if (digger.isValid()) {
                digger.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,
                        digger.getLocation(), 2, 0.5, 0.5, 0.5, 0);
                digger.getWorld().playSound(digger.getLocation(),
                        Sound.ENTITY_GENERIC_EXPLODE, 0.3f, 1.2f);
                digger.remove();
            }
        }, duration);
    }

    private void digBlocks(Fireball digger) {
        Location center = digger.getLocation();
        boolean dropItems = digger.getMetadata("drop_items").get(0).asBoolean();

        // Get owner for WorldGuard checks
        String ownerUUID = digger.getMetadata("digger_owner").get(0).asString();
        Player owner = Bukkit.getPlayer(java.util.UUID.fromString(ownerUUID));

        // Dig in 3x3x3 area centered on the fireball
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Location blockLoc = center.clone().add(x, y, z);
                    Block block = blockLoc.getBlock();

                    // Check if we can break this block
                    if (canBreakBlock(block, owner)) {
                        // Drop items if configured
                        if (dropItems && block.getType() != Material.AIR) {
                            block.breakNaturally();
                        } else {
                            block.setType(Material.AIR);
                        }
                    }
                }
            }
        }
    }

    private boolean canBreakBlock(Block block, Player owner) {
        Material type = block.getType();

        // Don't break air or bedrock
        if (type == Material.AIR || type == Material.BEDROCK) {
            return false;
        }

        // Don't break unbreakable blocks
        if (type == Material.BARRIER || type == Material.COMMAND_BLOCK ||
                type == Material.STRUCTURE_VOID || type == Material.END_PORTAL_FRAME ||
                type == Material.END_PORTAL || type == Material.NETHER_PORTAL) {
            return false;
        }

        // Check WorldGuard by simulating a block break event
        if (owner != null) {
            try {
                // Create a fake block break event to test if it's allowed
                BlockBreakEvent testEvent = new BlockBreakEvent(block, owner);
                Bukkit.getPluginManager().callEvent(testEvent);

                // If the event was cancelled by WorldGuard or other plugins, don't break
                if (testEvent.isCancelled()) {
                    return false;
                }
            } catch (Exception e) {
                // If there's any error, don't break the block (safe fallback)
                return false;
            }
        }

        return true;
    }

}