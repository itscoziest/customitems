package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class ExplodingSnowmanAction implements ItemAction {
    private final CustomItems plugin;

    public ExplodingSnowmanAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        // ONLY work on block placement, prevent throwing completely
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            // Cancel the event to prevent throwing
            event.setCancelled(true);
            return false;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return false;

        Location spawnLoc = clickedBlock.getRelative(event.getBlockFace()).getLocation();

        // Spawn snowman
        Snowman snowman = (Snowman) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.SNOWMAN);
        snowman.setMetadata("exploding_snowman", new FixedMetadataValue(plugin, true));
        snowman.setMetadata("owner", new FixedMetadataValue(plugin, player.getUniqueId()));

        double damage = item.getDamage() > 0 ? item.getDamage() : 3.0;
        double radius = item.getRadius() > 0 ? item.getRadius() : 5.0;
        int duration = item.getDuration() > 0 ? item.getDuration() : 5;

        // Get configurable slowness values
        int slownessDuration = item.getCustomDataInt("slowness-duration", 100);
        int slownessAmplifier = item.getCustomDataInt("slowness-amplifier", 1);

        // Schedule explosion
        new BukkitRunnable() {
            @Override
            public void run() {
                if (snowman.isValid() && !snowman.isDead()) {
                    Location explodeLoc = snowman.getLocation();

                    // Apply slowness to nearby enemies BEFORE removing snowman
                    for (Entity entity : explodeLoc.getWorld().getNearbyEntities(explodeLoc, radius, radius, radius)) {
                        if (entity instanceof Player && !entity.equals(player)) {
                            Player target = (Player) entity;
                            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slownessDuration, slownessAmplifier));

                            // Visual effect
                            target.spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 20, 0.5, 1, 0.5, 0.1);
                        }
                    }

                    // Remove snowman
                    snowman.remove();

                    // Get owner
                    String ownerUUID = snowman.getMetadata("owner").get(0).asString();
                    Player owner = Bukkit.getPlayer(java.util.UUID.fromString(ownerUUID));

                    // Create explosion without damaging owner
                    explodeLoc.getWorld().createExplosion(explodeLoc.getX(), explodeLoc.getY(), explodeLoc.getZ(),
                            (float) damage, false, false, owner);

                }
            }
        }.runTaskLater(plugin, duration * 20L);

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