package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class EggBridgerAction implements ItemAction {
    private final CustomItems plugin;

    public EggBridgerAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        // Get configuration
        boolean disappearBlocks = item.getCustomDataBoolean("disappear-blocks", true);
        int blockDuration = item.getCustomDataInt("block-duration", 20);
        String blockTypeString = item.getCustomDataString("block-type", "COBBLESTONE");

        Material blockType = Material.matchMaterial(blockTypeString);
        if (blockType == null) {
            blockType = Material.COBBLESTONE; // Fallback
        }

        // Make blockType final for inner class access
        final Material finalBlockType = blockType;

        // Launch egg
        Egg egg = player.launchProjectile(Egg.class);
        egg.setMetadata("custom_item", new FixedMetadataValue(plugin, "egg_bridger"));

        // Track bridge blocks for manual cleanup
        List<Location> bridgeBlocks = new ArrayList<>();

        // Create bridge behind egg as it flies
        new BukkitRunnable() {
            Location lastLoc = egg.getLocation().clone();

            @Override
            public void run() {
                if (!egg.isValid() || egg.isDead()) {
                    // Schedule block removal if needed
                    if (disappearBlocks && !bridgeBlocks.isEmpty()) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                for (Location loc : bridgeBlocks) {
                                    if (loc.getBlock().getType() == finalBlockType) {
                                        loc.getBlock().setType(Material.AIR);
                                        loc.getWorld().playSound(loc, Sound.BLOCK_STONE_BREAK, 0.5f, 1.0f);
                                    }
                                }
                            }
                        }.runTaskLater(plugin, blockDuration * 20L);
                    }
                    this.cancel();
                    return;
                }

                Location currentLoc = egg.getLocation();

                // Create bridge blocks between last and current position
                Vector direction = currentLoc.toVector().subtract(lastLoc.toVector());
                double distance = direction.length();

                if (distance > 0.5) {
                    direction.normalize();

                    for (double d = 0; d < distance; d += 0.8) {
                        Location bridgeLoc = lastLoc.clone().add(direction.clone().multiply(d));
                        bridgeLoc.setY(bridgeLoc.getY() - 1);
                        bridgeLoc = bridgeLoc.getBlock().getLocation();

                        // Only place if there's air and not already in list
                        if (bridgeLoc.getBlock().getType() == Material.AIR &&
                                !bridgeBlocks.contains(bridgeLoc)) {

                            bridgeLoc.getBlock().setType(finalBlockType); // Use finalBlockType here
                            bridgeBlocks.add(bridgeLoc.clone());

                            // Sound effect
                            bridgeLoc.getWorld().playSound(bridgeLoc, Sound.BLOCK_STONE_PLACE, 0.8f, 1.2f);
                        }
                    }
                }

                lastLoc = currentLoc.clone();
            }
        }.runTaskTimer(plugin, 1L, 2L);

        // Sound effect
        player.playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 1.0f, 1.0f);

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