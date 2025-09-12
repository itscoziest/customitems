package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

public class LassoAction implements ItemAction {
    private final CustomItems plugin;

    public LassoAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        // Get pull strength from config
        double basePullStrength = item.getCustomDataDouble("pull-strength", 1.5);
        double maxRange = item.getCustomDataDouble("max-range", 30.0);
        boolean distanceBased = item.getCustomDataBoolean("distance-based-pull", true);

        // Shoot an arrow as the lasso projectile
        Arrow lassoArrow = player.launchProjectile(Arrow.class);

        // Make the arrow faster and straighter
        Vector direction = player.getLocation().getDirection();
        lassoArrow.setVelocity(direction.multiply(3.0));

        // Prevent arrow from dealing damage
        lassoArrow.setDamage(0);

        // Mark it as a lasso arrow with metadata
        lassoArrow.setMetadata("lasso_arrow", new FixedMetadataValue(plugin, true));
        lassoArrow.setMetadata("lasso_caster", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        lassoArrow.setMetadata("pull_strength", new FixedMetadataValue(plugin, basePullStrength));
        lassoArrow.setMetadata("max_range", new FixedMetadataValue(plugin, maxRange));
        lassoArrow.setMetadata("distance_based", new FixedMetadataValue(plugin, distanceBased));

        // Visual and sound effects
        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 1.0f, 1.2f);

        // Create lasso trail effect
        createLassoTrail(lassoArrow);

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

    private void createLassoTrail(Arrow arrow) {
        // Create a repeating task to show the lasso trail
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isDead()) {
                    return;
                }

                // Show rope-like particles behind the arrow
                Location arrowLoc = arrow.getLocation();
                arrowLoc.getWorld().spawnParticle(Particle.CRIT, arrowLoc, 2, 0.1, 0.1, 0.1, 0);
            }
        }, 0L, 1L); // Every tick
    }
}