package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class GrapplingHookAction implements ItemAction {
    private final CustomItems plugin;

    public GrapplingHookAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        event.setCancelled(true);

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        double maxRange = item.getCustomDataDouble("max-range", 40.0);
        double pullStrength = item.getCustomDataDouble("pull-strength", 1.5);
        boolean consume = item.getCustomDataBoolean("consume", false);

        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        RayTraceResult rayTrace = player.getWorld().rayTraceBlocks(eyeLocation, direction, maxRange);

        if (rayTrace == null || rayTrace.getHitBlock() == null) {
            return false;
        }

        Location hitLocation = rayTrace.getHitPosition().toLocation(player.getWorld());

        FishHook hook = player.launchProjectile(FishHook.class);
        hook.setVelocity(direction.multiply(3.0));

        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 1.0f, 1.0f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                if (ticks <= 10) {
                    Vector pullDirection = hitLocation.toVector().subtract(player.getLocation().toVector()).normalize();
                    Vector velocity = pullDirection.multiply(pullStrength);
                    player.setVelocity(velocity);
                }

                if (ticks >= 15 || !hook.isValid()) {
                    if (hook.isValid()) {
                        hook.remove();
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 3L, 1L);

        if (consume) {
            consumeItem(player, event);
        }

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