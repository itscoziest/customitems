package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class DragonBreathAction implements ItemAction {
    private final CustomItems plugin;

    public DragonBreathAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        // Get damage and duration from config
        double damage = item.getCustomDataDouble("breath-damage", 3.0);
        int duration = item.getCustomDataInt("breath-duration", 100); // In ticks (5 seconds default)

        // Create dragon fireball
        DragonFireball fireball = player.launchProjectile(DragonFireball.class);

        // Set velocity for better control
        Vector direction = player.getLocation().getDirection();
        fireball.setVelocity(direction.multiply(2.0));

        // Store custom data on the fireball for when it explodes
        fireball.setMetadata("custom_dragon_breath", new FixedMetadataValue(plugin, true));
        fireball.setMetadata("breath_damage", new FixedMetadataValue(plugin, damage));
        fireball.setMetadata("breath_duration", new FixedMetadataValue(plugin, duration));
        fireball.setMetadata("breath_caster", new FixedMetadataValue(plugin, player.getUniqueId().toString()));

        // Play dragon breath sound
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 1.0f);

        // Visual effect at launch
        Location launchLoc = player.getEyeLocation();
        launchLoc.getWorld().spawnParticle(Particle.DRAGON_BREATH, launchLoc, 20, 0.3, 0.3, 0.3, 0.1);


        if (event.getItem() != null) {
            if (event.getItem().getAmount() > 1) {
                event.getItem().setAmount(event.getItem().getAmount() - 1);
            } else {
                player.getInventory().setItem(event.getHand(), null);
            }
        }

        return true;
    }
}