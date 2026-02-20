package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.handlers.ItemAction;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.Sound;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class CatCannonAction implements ItemAction {
    private final CustomItems plugin;

    public CatCannonAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        double damage = item.getCustomDataDouble("damage", 6.0);
        double speed = item.getCustomDataDouble("speed", 1.5);
        int despawnTicks = 40; // Cats despawn after 2 seconds if they don't hit anything

        // Launch Projectile
        Snowball snowball = player.launchProjectile(Snowball.class);
        snowball.setVelocity(player.getLocation().getDirection().multiply(speed));

        // Metadata for damage handling in ProjectileListener
        snowball.setMetadata("cat_cannon", new FixedMetadataValue(plugin, true));
        snowball.setMetadata("damage", new FixedMetadataValue(plugin, damage));

        // Mount a Cat
        Cat cat = player.getWorld().spawn(player.getLocation(), Cat.class);
        cat.setTamed(false);
        cat.setInvulnerable(true);
        cat.setBaby();
        snowball.addPassenger(cat);

        player.playSound(player.getLocation(), Sound.ENTITY_CAT_AMBIENT, 1.0f, 1.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);

        // Despawn task (if it flies into void or gets stuck)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (cat.isValid() && !cat.isDead()) {
                    cat.remove();
                }
                if (snowball.isValid() && !snowball.isDead()) {
                    snowball.remove();
                }
            }
        }.runTaskLater(plugin, despawnTicks);

        return true;
    }
}