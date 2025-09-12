package com.strikesdev.customitems.commands;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import com.strikesdev.customitems.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.stream.Collectors;

public class CustomItemsCommand implements CommandExecutor, TabCompleter {

    private final CustomItems plugin;

    public CustomItemsCommand(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("customitems.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;
            case "give":
                handleGive(sender, args);
                break;
            case "list":
                handleList(sender);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            case "debug":
                handleDebug(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }


        return true;
    }


    private void handleListCaps(CommandSender sender) {
        Map<Material, Integer> caps = plugin.getConfigManager().getRegularItemCaps();

        sender.sendMessage("§6§l=== Regular Item Combat Caps ===");
        if (caps.isEmpty()) {
            sender.sendMessage("§7No regular item caps configured");
        } else {
            for (Map.Entry<Material, Integer> entry : caps.entrySet()) {
                sender.sendMessage("§e" + entry.getKey().name() + " §7: §6" + entry.getValue());
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== CustomItems Commands ===");
        sender.sendMessage("§e/customitems reload §7- Reload the plugin");
        sender.sendMessage("§e/customitems give <player> <item> [amount] §7- Give custom item");
        sender.sendMessage("§e/customitems list §7- List all custom items");
        sender.sendMessage("§e/customitems info <item> §7- Show item information");
        sender.sendMessage("§e/customitems debug [on|off] §7- Toggle debug mode");
    }

    private void handleReload(CommandSender sender) {
        try {
            plugin.reload();
            sender.sendMessage(plugin.getConfigManager().getMessage("admin.reload-success"));
        } catch (Exception e) {
            sender.sendMessage("§cError reloading plugin: " + e.getMessage());
            plugin.getLogger().severe("Error reloading plugin: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /customitems give <player> <item> [amount]");
            return;
        }

        String playerName = args[1];
        String itemId = args[2];
        int amount = 1;

        if (args.length > 3) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount <= 0 || amount > 64) {
                    sender.sendMessage("§cAmount must be between 1 and 64");
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid amount: " + args[3]);
                return;
            }
        }

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("admin.player-not-found",
                    "{player}", playerName));
            return;
        }

        CustomItem customItem = plugin.getItemManager().getCustomItem(itemId);
        if (customItem == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("admin.item-not-found",
                    "{item}", itemId));
            return;
        }

        ItemStack itemStack = customItem.createItemStack(amount);
        target.getInventory().addItem(itemStack);
// Schedule cap check for next tick to avoid multiple triggers
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getCombatManager().applyCombatCaps(target);
        }, 1L);

        // Get the base message without colorizing yet
        String baseMessage = plugin.getConfigManager().getMessagesConfig()
                .getString("messages.admin.item-given", "{prefix}Given {amount}x {item} to {player}!");

        // Get prefix and colorize it
        String prefix = ChatUtils.colorize(plugin.getConfigManager().getMessagesConfig()
                .getString("messages.prefix", "&8[&6CustomItems&8]&r "));

        // Replace placeholders
        String message = baseMessage
                .replace("{prefix}", prefix)
                .replace("{amount}", String.valueOf(amount))
                .replace("{item}", customItem.getName()) // Item name already has colors
                .replace("{player}", target.getName());

        // Now colorize the entire message
        sender.sendMessage(ChatUtils.colorize(message));

        target.sendMessage(ChatUtils.colorize("§aYou received " + amount + "x " + customItem.getName() + "!"));
    }

    private void handleList(CommandSender sender) {
        Collection<CustomItem> items = plugin.getItemManager().getAllCustomItems();

        sender.sendMessage(plugin.getConfigManager().getMessage("list.header"));

        for (CustomItem item : items) {
            sender.sendMessage(plugin.getConfigManager().getMessage("list.item",
                    "{item}", item.getName(),
                    "{id}", item.getId()));
        }

        sender.sendMessage(plugin.getConfigManager().getMessage("list.footer",
                "{count}", String.valueOf(items.size())));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /customitems info <item>");
            return;
        }

        String itemId = args[1];
        CustomItem item = plugin.getItemManager().getCustomItem(itemId);

        if (item == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("admin.item-not-found",
                    "{item}", itemId));
            return;
        }

        sender.sendMessage("§6§l=== " + item.getName() + " §6§l===");
        sender.sendMessage("§eID: §f" + item.getId());
        sender.sendMessage("§eMaterial: §f" + item.getMaterial());
        sender.sendMessage("§eCooldown: §f" + item.getCooldown() + "s");
        sender.sendMessage("§eCombat Cap: §f" + (item.getCombatCap() > 0 ? item.getCombatCap() : "No limit"));
        sender.sendMessage("§eDamage: §f" + item.getDamage());
        sender.sendMessage("§eRange: §f" + item.getRange());
        sender.sendMessage("§eRadius: §f" + item.getRadius());
        sender.sendMessage("§eDuration: §f" + item.getDuration() + "s");

        if (!item.getAllowedRegions().isEmpty()) {
            sender.sendMessage("§eAllowed Regions: §f" + String.join(", ", item.getAllowedRegions()));
        }

        if (!item.getPermission().isEmpty()) {
            sender.sendMessage("§ePermission: §f" + item.getPermission());
        }

        if (!item.getEffects().isEmpty()) {
            sender.sendMessage("§eEffects: §f" + item.getEffects().size() + " potion effects");
        }
    }

    private void handleDebug(CommandSender sender, String[] args) {
        if (args.length < 2) {
            boolean currentDebug = plugin.getConfigManager().isDebugMode();
            sender.sendMessage("§eDebug mode is currently: " + (currentDebug ? "§aON" : "§cOFF"));
            sender.sendMessage("§7Use: /customitems debug <on|off>");
            return;
        }

        String mode = args[1].toLowerCase();
        boolean enable;

        if ("on".equals(mode) || "true".equals(mode)) {
            enable = true;
        } else if ("off".equals(mode) || "false".equals(mode)) {
            enable = false;
        } else {
            sender.sendMessage("§cUsage: /customitems debug <on|off>");
            return;
        }

        plugin.getConfigManager().getConfig().set("debug", enable);
        plugin.getConfigManager().saveConfigs();

        sender.sendMessage("§eDebug mode " + (enable ? "§aenabled" : "§cdisabled"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("customitems.admin")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Main subcommands
            completions.addAll(Arrays.asList("reload", "give", "list", "info", "debug"));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "give":
                    // Player names
                    completions.addAll(Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList()));
                    break;
                case "info":
                    // Item IDs
                    completions.addAll(plugin.getItemManager().getCustomItemIds());
                    break;
                case "debug":
                    completions.addAll(Arrays.asList("on", "off"));
                    break;

            }
        } else if (args.length == 3 && "give".equals(args[0].toLowerCase())) {
            // Item IDs for give command
            completions.addAll(plugin.getItemManager().getCustomItemIds());
        } else if (args.length == 4 && "give".equals(args[0].toLowerCase())) {
            // Amounts for give command
            completions.addAll(Arrays.asList("1", "5", "10", "16", "32", "64"));
        }

        // Filter completions based on what the user has typed
        String input = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(input))
                .sorted()
                .collect(Collectors.toList());
    }
}