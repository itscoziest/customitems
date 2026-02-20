package com.strikesdev.customitems.listeners;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import java.util.concurrent.ThreadLocalRandom;

public class EntityListener implements Listener {
    private final CustomItems plugin;

    public EntityListener(CustomItems plugin) {
        this.plugin = plugin;
    }

    // --- FIX: TASER PREVENT HIT ---
    @EventHandler(priority = EventPriority.HIGH)
    public void onTasedPlayerAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            if (attacker.hasPotionEffect(PotionEffectType.SLOW) &&
                    attacker.hasPotionEffect(PotionEffectType.JUMP) &&
                    attacker.hasPotionEffect(PotionEffectType.SLOW_DIGGING)) {

                event.setCancelled(true);
                attacker.sendMessage(ChatColor.RED + "You are tased and cannot attack!");
            }
        }
    }

    // --- COMBAT DAMAGE & SWORD EFFECTS ---
    @EventHandler
    public void onCombatDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();

        CustomItem customItem = plugin.getItemManager().getCustomItem(item);
        if (customItem != null) {
            double configDamage = customItem.getDamage();
            if (configDamage > 0) {
                event.setDamage(configDamage);
            }

            String type = customItem.getCustomDataString("type", "");

            // VAMPIRE SWORD (On Hit)
            if ("vampire_sword".equals(type)) {
                double chance = customItem.getCustomDataDouble("heal-chance", 25.0);
                if (ThreadLocalRandom.current().nextDouble() * 100 <= chance) {
                    double healAmount = customItem.getCustomDataDouble("heal-amount", 2.0);
                    double currentHealth = player.getHealth();
                    double maxHealth = player.getMaxHealth();

                    if (currentHealth < maxHealth) {
                        player.setHealth(Math.min(maxHealth, currentHealth + healAmount));
                        player.spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 1);
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
                    }
                }
            }

            // FIRE SWORD (On Hit)
            if ("fire_sword".equals(type)) {
                double chance = customItem.getCustomDataDouble("fire-chance", 40.0);
                if (ThreadLocalRandom.current().nextDouble() * 100 <= chance) {
                    int seconds = customItem.getCustomDataInt("fire-duration", 5);
                    // Use setFireTicks (20 ticks = 1 second)
                    event.getEntity().setFireTicks(seconds * 20);
                    event.getEntity().getWorld().spawnParticle(Particle.FLAME, event.getEntity().getLocation(), 15, 0.3, 0.5, 0.3, 0.05);
                    event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
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