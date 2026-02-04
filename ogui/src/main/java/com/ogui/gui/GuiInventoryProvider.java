package com.ogui.gui;

import com.ogui.util.ColorUtil;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiInventoryProvider implements InventoryProvider {

    private final GuiDefinition definition;

    public GuiInventoryProvider(GuiDefinition definition) {
        this.definition = definition;
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        for (GuiItem guiItem : definition.getItems().values()) {
            Material material = Material.matchMaterial(guiItem.getMaterial());
            if (material == null) {
                continue;
            }
            ItemStack stack = new ItemStack(material);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                if (guiItem.getName() != null && !guiItem.getName().isEmpty()) {
                    meta.setDisplayName(ColorUtil.color(guiItem.getName()));
                }
                List<String> lore = guiItem.getLore();
                if (!lore.isEmpty()) {
                    meta.setLore(ColorUtil.color(lore));
                }
                stack.setItemMeta(meta);
            }
            ClickableItem clickable = ClickableItem.of(stack, event -> {
                for (String command : guiItem.getCommands()) {
                    String resolved = command.replace("{player}", player.getName());
                    Bukkit.dispatchCommand(player, resolved);
                }
                if (guiItem.isCloseOnClick()) {
                    player.closeInventory();
                }
            });
            contents.set(guiItem.getSlot(), clickable);
        }
    }

    @Override
    public void update(Player player, InventoryContents contents) {
    }
}
