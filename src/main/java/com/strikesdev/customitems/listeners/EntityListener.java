package com.strikesdev.customitems.listeners;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

public class EntityListener implements Listener {
    private final CustomItems plugin;

    public EntityListener(CustomItems plugin) {
        this.plugin = plugin;
    }

    // --- FIX: GLOBAL DAMAGE HANDLER ---
    // This fixes "Damages are wrong". Vanilla tools often override damage.
    // This ensures your Config Damage is used.
    @EventHandler
    public void onCombatDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();

        CustomItem customItem = plugin.getItemManager().getCustomItem(item);
        if (customItem != null) {
            double configDamage = customItem.getDamage();
            // If damage is set in config (greater than 0), FORCE it.
            if (configDamage > 0) {
                event.setDamage(configDamage);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        if (killer.getInventory().getItemInMainHand() != null) {
            CustomItem customItem = plugin.getItemManager().getCustomItem(killer.getInventory().getItemInMainHand());
            if (customItem != null && "vampire_sword".equals(customItem.getCustomDataString("type", ""))) {
                int healAmount = customItem.getCustomDataInt("heal-on-kill", 4);
                double currentHealth = killer.getHealth();
                double maxHealth = killer.getMaxHealth();

                if (currentHealth < maxHealth) {
                    double newHealth = Math.min(maxHealth, currentHealth + healAmount);
                    killer.setHealth(newHealth);
                    killer.sendMessage(plugin.getConfigManager().getMessage("vampire-sword.healed",
                            "{amount}", String.valueOf((int)(newHealth - currentHealth))));
                }
            }
        }
    }

    @EventHandler
    public void onFireworkDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Firework) {
            Firework firework = (Firework) event.getDamager();
            if (firework.hasMetadata("no_damage")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();
            makeDogsTarget(attacker, victim);
            makeDogsTarget(victim, attacker);
        }
    }

    private void makeDogsTarget(Player owner, Player target) {
        owner.getWorld().getEntities().stream()
                .filter(entity -> entity instanceof Wolf)
                .map(entity -> (Wolf) entity)
                .filter(wolf -> wolf.isTamed() && wolf.getOwner() != null && wolf.getOwner().equals(owner))
                .filter(wolf -> wolf.getLocation().distance(owner.getLocation()) <= 50)
                .forEach(wolf -> {
                    wolf.setTarget(target);
                    wolf.setAngry(true);
                });
    }

    @EventHandler
    public void onEvokerFangsDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof EvokerFangs) {
            EvokerFangs fangs = (EvokerFangs) event.getDamager();
            if (fangs.hasMetadata("snap_ring_fangs")) {
                String ownerUUID = fangs.getMetadata("owner").get(0).asString();
                double customDamage = fangs.getMetadata("custom_damage").get(0).asDouble();

                if (event.getEntity() instanceof Player) {
                    Player target = (Player) event.getEntity();
                    if (target.getUniqueId().toString().equals(ownerUUID)) {
                        event.setCancelled(true);
                        return;
                    }
                }
                event.setDamage(customDamage);
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            Player player = (Player) event.getEntity();
            if (player.hasMetadata("leap_protection")) {
                long protectionEnd = player.getMetadata("leap_protection").get(0).asLong();
                if (System.currentTimeMillis() < protectionEnd) {
                    event.setCancelled(true);
                    player.removeMetadata("leap_protection", plugin);
                }
            }
        }
    }

    @EventHandler
    public void onEnderPearlTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            Player player = event.getPlayer();
            for (Entity entity : player.getWorld().getNearbyEntities(event.getTo(), 5, 5, 5)) {
                if (entity instanceof EnderPearl) {
                    EnderPearl pearl = (EnderPearl) entity;
                    if (pearl.hasMetadata("no_teleport") && pearl.getShooter() == player) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onIllusionZombieTarget(EntityTargetEvent event) {
        if (event.getEntity().hasMetadata("illusion_zombie")) {
            String ownerUUID = event.getEntity().getMetadata("illusion_owner").get(0).asString();
            if (event.getTarget() instanceof Player) {
                Player target = (Player) event.getTarget();
                if (target.getUniqueId().toString().equals(ownerUUID)) {
                    event.setCancelled(true);
                    Player newTarget = findNearestEnemy(target, event.getEntity().getLocation(), 10.0);
                    if (newTarget != null) {
                        event.setTarget(newTarget);
                    }
                }
            }
        }
    }

    private Player findNearestEnemy(Player owner, Location center, double range) {
        Player closest = null;
        double closestDistance = range;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.equals(owner) || !player.getWorld().equals(center.getWorld())) continue;
            double distance = player.getLocation().distance(center);
            if (distance < closestDistance) {
                closest = player;
                closestDistance = distance;
            }
        }
        return closest;
    }
}