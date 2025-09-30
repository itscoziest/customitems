package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.Set;

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

        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);

        String requiredRegion = item.getCustomDataString("destruction-region", "grave");
        int destructionRadius = item.getCustomDataInt("destruction-radius", 1);
        double speed = item.getCustomDataDouble("speed", 0.7);

        Location playerEyeLoc = player.getEyeLocation();
        Vector direction = playerEyeLoc.getDirection();
        Vector rightOffset = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize().multiply(0.5);
        Location spawnLocation = playerEyeLoc.clone().add(direction.multiply(1.0)).add(rightOffset).subtract(0, 0.5, 0);

        ArmorStand digger = player.getWorld().spawn(spawnLocation, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setMarker(true);
            as.setInvulnerable(true);
            as.setArms(true);
            as.getEquipment().setItemInMainHand(new ItemStack(Material.CRYING_OBSIDIAN));
        });

        final Vector moveDirection = player.getLocation().getDirection().normalize().multiply(speed);

        new BukkitRunnable() {
            private int ticksLived = 0;
            private final Set<Material> indestructible = EnumSet.of(Material.AIR, Material.BEDROCK, Material.BARRIER, Material.END_PORTAL_FRAME);

            @Override
            public void run() {
                int currentMaxLifetime = plugin.getItemManager().getCustomItem("grave_digger").getCustomDataInt("max-lifetime-ticks", 200);

                if (!digger.isValid() || digger.isDead() || ticksLived++ > currentMaxLifetime) {
                    if (digger.isValid()) digger.remove();
                    this.cancel();
                    return;
                }

                // ############### THIS IS THE PRECISION FIX ###############
                // We move the armor stand to its next position FIRST.
                Location oldLocation = digger.getLocation();
                Location newLocation = oldLocation.clone().add(moveDirection);
                newLocation.setYaw(oldLocation.getYaw() + 25); // Apply spin
                digger.teleport(newLocation);

                // NOW, we check if this new, updated location is outside the region.
                if (!plugin.getRegionManager().isInRegion(newLocation, requiredRegion)) {
                    // If it is outside, it means we just crossed the border.
                    // We fizzle and stop BEFORE breaking any blocks at this new location.
                    newLocation.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, newLocation, 40, 0.5, 0.5, 0.5, 0.1);
                    newLocation.getWorld().playSound(newLocation, Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1.0f, 0.5f);
                    digger.remove();
                    this.cancel();
                    return; // Stop the task.
                }
                // ################# END OF THE FIX #####################

                // If we are here, it means the new location is safe. We can now destroy blocks.
                digger.getWorld().spawnParticle(Particle.SQUID_INK, newLocation, 3, 0.1, 0.1, 0.1, 0);

                for (int x = -destructionRadius; x <= destructionRadius; x++) {
                    for (int y = -destructionRadius; y <= destructionRadius; y++) {
                        for (int z = -destructionRadius; z <= destructionRadius; z++) {
                            Block block = newLocation.clone().add(x, y, z).getBlock();
                            if (!indestructible.contains(block.getType())) {
                                Location blockCenter = block.getLocation().add(0.5, 0.5, 0.5);
                                newLocation.getWorld().spawnParticle(Particle.BLOCK_DUST, blockCenter, 10, 0.5, 0.5, 0.5, block.getBlockData());
                                newLocation.getWorld().spawnParticle(Particle.CRIT_MAGIC, blockCenter, 5, 0.5, 0.5, 0.5, 0);
                                block.setType(Material.AIR, false);
                            }
                        }
                    }
                }
                newLocation.getWorld().playSound(newLocation, Sound.BLOCK_STONE_BREAK, 0.5f, 1.5f);
            }
        }.runTaskTimer(plugin, 0L, 1L);

        boolean consume = item.getCustomDataBoolean("consume", true);
        if (consume && event.getItem() != null) {
            if (event.getItem().getAmount() > 1) {
                event.getItem().setAmount(event.getItem().getAmount() - 1);
            } else {
                player.getInventory().setItem(event.getHand(), null);
            }
        }
        return true;
    }
}