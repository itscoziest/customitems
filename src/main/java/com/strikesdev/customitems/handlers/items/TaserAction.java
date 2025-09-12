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

public class TaserAction implements ItemAction {
    private final CustomItems plugin;

    public TaserAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        // Get stun duration from config
        int stunDuration = item.getCustomDataInt("stun-duration", 60); // 3 seconds default (in ticks)
        double range = item.getCustomDataDouble("taser-range", 25.0);

        // Shoot an arrow as the taser projectile
        Arrow taserArrow = player.launchProjectile(Arrow.class);

        // Make the arrow faster and more accurate
        Vector direction = player.getLocation().getDirection();
        taserArrow.setVelocity(direction.multiply(4.0));

        // Prevent arrow from dealing damage
        taserArrow.setDamage(0);

        // Mark it as a taser arrow with metadata
        taserArrow.setMetadata("taser_arrow", new FixedMetadataValue(plugin, true));
        taserArrow.setMetadata("taser_caster", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        taserArrow.setMetadata("stun_duration", new FixedMetadataValue(plugin, stunDuration));
        taserArrow.setMetadata("taser_range", new FixedMetadataValue(plugin, range));

        // Visual and sound effects
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2.0f);

        // Create electric trail effect
        createElectricTrail(taserArrow);

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

    private void createElectricTrail(Arrow arrow) {
        // Create a repeating task to show the electric trail
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isDead()) {
                    return;
                }

                // Show electric particles behind the arrow
                Location arrowLoc = arrow.getLocation();
                arrowLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, arrowLoc, 3, 0.1, 0.1, 0.1, 0.1);
                arrowLoc.getWorld().spawnParticle(Particle.CRIT, arrowLoc, 1, 0.05, 0.05, 0.05, 0);
            }
        }, 0L, 1L); // Every tick
    }
}