package com.strikesdev.customitems.listeners;

import com.strikesdev.customitems.CustomItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinQuitListener implements Listener {
    private final CustomItems plugin;

    public PlayerJoinQuitListener(CustomItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Initialize player data if needed
        Player player = event.getPlayer();

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Player " + player.getName() + " joined - initializing data");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Cleanup player data
        plugin.getCooldownManager().removeAllCooldowns(player);
        plugin.getCombatManager().exitCombat(player);
        plugin.getActionBarManager().cleanupPlayer(player);

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Player " + player.getName() + " quit - cleaned up data");
        }
    }
}