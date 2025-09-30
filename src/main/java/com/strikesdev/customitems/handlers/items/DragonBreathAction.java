package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;

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

        double configDamage = 3.0;
        int configDuration = 100; // 5 seconds

        Snowball dragonProjectile = player.launchProjectile(Snowball.class);
        dragonProjectile.setVelocity(player.getLocation().getDirection().multiply(2.0));

        dragonProjectile.setMetadata("dragon_breath_projectile", new FixedMetadataValue(plugin, true));
        dragonProjectile.setMetadata("caster_name", new FixedMetadataValue(plugin, player.getName()));
        dragonProjectile.setMetadata("config_damage", new FixedMetadataValue(plugin, configDamage));
        dragonProjectile.setMetadata("config_duration", new FixedMetadataValue(plugin, configDuration));

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.5f, 0.8f);

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