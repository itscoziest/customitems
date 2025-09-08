package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class SnapRingAction implements ItemAction {
    private final CustomItems plugin;

    public SnapRingAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        double radius = item.getRadius() > 0 ? item.getRadius() : 4.0;
        double totalDamage = item.getDamage() > 0 ? item.getDamage() : 6.0;

        Location center = player.getLocation();

        // Create circle of evoker fangs
        int fangCount = 12;
        double damagePerFang = totalDamage / fangCount; // Split damage across all fangs

        for (int i = 0; i < fangCount; i++) {
            double angle = (2 * Math.PI * i) / fangCount;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);

            Location fangLoc = new Location(center.getWorld(), x, center.getY(), z);

            // Spawn evoker fangs with delay for visual effect
            int delay = i * 2;
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Spawn visual fangs
                    EvokerFangs fangs = (EvokerFangs) center.getWorld().spawnEntity(fangLoc, EntityType.EVOKER_FANGS);

                    // Damage ALL players inside the circle radius (not just near this fang)
                    for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                        if (entity instanceof Player && !entity.equals(player)) {
                            Player target = (Player) entity;

                            // Small damage per fang (total damage split across all fangs)
                            target.damage(damagePerFang);

                            // Visual effects
                            target.spawnParticle(Particle.CRIT_MAGIC, target.getLocation().add(0, 1, 0), 3);
                            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.5f, 1.2f);
                        }
                    }
                }
            }.runTaskLater(plugin, delay);
        }

        // Sound effect
        player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.2f);

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