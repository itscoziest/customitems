package com.strikesdev.customitems.listeners;

import com.strikesdev.customitems.CustomItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

// Player Combat Listener
public class PlayerCombatListener implements Listener {
    private final CustomItems plugin;

    public PlayerCombatListener(CustomItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Enter combat when players damage each other
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            plugin.getCombatManager().enterCombat(attacker);
        }

        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            plugin.getCombatManager().enterCombat(victim);
        }
    }
}