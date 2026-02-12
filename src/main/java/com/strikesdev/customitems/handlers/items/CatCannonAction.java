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

        double speed = item.getCustomDataDouble("speed", 1.5);
        double damage = item.getCustomDataDouble("damage", 6.0);

        // We use a snowball as the "vehicle" for physics, and mount a cat to it
        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setVelocity(player.getLocation().getDirection().multiply(speed));
        projectile.setMetadata("cat_cannon", new FixedMetadataValue(plugin, true));
        projectile.setMetadata("damage", new FixedMetadataValue(plugin, damage));
        projectile.setVisibleByDefault(false); // Hide the snowball if possible (1.20+)

        // Spawn the visual Cat
        Cat cat = player.getWorld().spawn(player.getLocation(), Cat.class, c -> {
            c.setInvulnerable(true);
            c.setTamed(false);
            // FIX: Spigot API uses setAdult() or setBaby() (no boolean args)
            c.setAdult();
            // Randomize cat type
            c.setCatType(Cat.Type.values()[(int) (Math.random() * Cat.Type.values().length)]);
        });

        projectile.addPassenger(cat);

        player.playSound(player.getLocation(), Sound.ENTITY_CAT_AMBIENT, 1.0f, 1.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);

        return true;
    }
}