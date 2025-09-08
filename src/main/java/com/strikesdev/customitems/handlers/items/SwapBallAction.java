package com.strikesdev.customitems.handlers.items;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class SwapBallAction implements ItemAction {
    private final CustomItems plugin;

    public SwapBallAction(CustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, CustomItem item, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        // Launch ender pearl (visual effect)
        EnderPearl pearl = player.launchProjectile(EnderPearl.class);
        pearl.setMetadata("custom_item", new FixedMetadataValue(plugin, "swap_ball"));
        pearl.setMetadata("no_teleport", new FixedMetadataValue(plugin, true));
        pearl.setMetadata("item_slot", new FixedMetadataValue(plugin, event.getHand().toString()));
        pearl.setMetadata("original_item", new FixedMetadataValue(plugin, event.getItem().clone()));

        // DON'T consume item yet - only consume on successful hit

        // Sound effect
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 1.0f, 1.2f);

        return true; // Don't call consumeItem here
    }

    private void consumeItem(Player player, PlayerInteractEvent event) {
        if (event.getItem().getAmount() > 1) {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
        } else {
            player.getInventory().setItem(event.getHand(), null);
        }
    }
}