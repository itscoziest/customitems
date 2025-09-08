package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

// 🌀 Illusion Action
public class IllusionAction implements ItemAction {
    private final CustomItems plugin;

    public IllusionAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onIllusionZombieTarget(EntityTargetEvent event) {
        if (event.getEntity().hasMetadata("illusion_zombie")) {
            String ownerUUID = event.getEntity().getMetadata("illusion_owner").get(0).asString();

            // Don't target the owner
            if (event.getTarget() instanceof Player) {
                Player target = (Player) event.getTarget();
                if (target.getUniqueId().toString().equals(ownerUUID)) {
                    event.setCancelled(true);

                    // Find a different target (enemy player)
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
            if (player.equals(owner) || !player.getWorld().equals(center.getWorld())) {
                continue;
            }

            double distance = player.getLocation().distance(center);
            if (distance < closestDistance) {
                closest = player;
                closestDistance = distance;
            }
        }

        return closest;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        // Apply invisibility to player
        if (!item.getEffects().isEmpty()) {
            item.getEffects().forEach(player::addPotionEffect);
        } else {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,
                    item.getDuration() * 20, 0));
        }

        // Spawn invisible zombies
        int zombieCount = item.getCustomDataInt("zombie-count", 2);
        int zombieHealth = item.getCustomDataInt("zombie-health", 4);

        for (int i = 0; i < zombieCount; i++) {
            Location spawnLoc = player.getLocation().add(
                    (Math.random() - 0.5) * 4, 0, (Math.random() - 0.5) * 4);

            Zombie zombie = (Zombie) player.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
            zombie.setMaxHealth(zombieHealth);
            zombie.setHealth(zombieHealth);
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,
                    item.getDuration() * 20, 0));

            // Equip with enchanted netherite gear
            equipZombie(zombie);

            // Find nearest enemy to attack (not the owner)
            Player target = findClosestEnemy(player, spawnLoc, 15.0);
            if (target != null) {
                zombie.setTarget(target);
                zombie.setAI(true); // Enable AI so it can attack
            }

            // Mark as illusion zombie
            zombie.setMetadata("illusion_owner", new FixedMetadataValue(plugin, player.getUniqueId()));
            zombie.setMetadata("illusion_zombie", new FixedMetadataValue(plugin, true));

            // Register as temporary entity
            plugin.getEffectManager().registerTemporaryEntity(zombie, item.getDuration());
        }

        // Consume item
        consumeItem(player, event);
        return true;
    }

    private Player findClosestEnemy(Player owner, Location center, double range) {
        Player closest = null;
        double closestDistance = range;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.equals(owner) || !player.getWorld().equals(center.getWorld())) {
                continue;
            }

            double distance = player.getLocation().distance(center);
            if (distance < closestDistance) {
                closest = player;
                closestDistance = distance;
            }
        }

        return closest;
    }



    private void equipZombie(Zombie zombie) {
        zombie.getEquipment().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
        zombie.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        zombie.getEquipment().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
        zombie.getEquipment().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
        zombie.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));

        // Add enchantments
        zombie.getEquipment().getHelmet().addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        zombie.getEquipment().getItemInMainHand().addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DAMAGE_ALL, 5);
    }

    private void consumeItem(Player player, PlayerInteractEvent event) {
        if (event.getItem().getAmount() > 1) {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
        } else {
            player.getInventory().setItem(event.getHand(), null);
        }
    }



}