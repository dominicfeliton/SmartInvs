package com.ogui.gui;

import com.ogui.OGUIPlugin;
import com.ogui.util.ColorUtil;
import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class GuiRegistry {

    private final OGUIPlugin plugin;
    private final Map<String, GuiDefinition> definitions = new LinkedHashMap<>();

    public GuiRegistry(OGUIPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        definitions.clear();
        File file = new File(plugin.getDataFolder(), "guis.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection guisSection = config.getConfigurationSection("guis");
        if (guisSection == null) {
            plugin.getLogger().warning("No guis section found in guis.yml.");
            return;
        }
        for (String id : guisSection.getKeys(false)) {
            ConfigurationSection guiSection = guisSection.getConfigurationSection(id);
            if (guiSection == null) {
                continue;
            }
            String title = guiSection.getString("title", id);
            int rows = Math.min(6, Math.max(1, guiSection.getInt("rows", 1)));
            Map<Integer, GuiItem> items = new LinkedHashMap<>();
            ConfigurationSection itemsSection = guiSection.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String slotKey : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(slotKey);
                    if (itemSection == null) {
                        continue;
                    }
                    Integer slot = parseSlot(slotKey);
                    if (slot == null) {
                        plugin.getLogger().warning("Invalid slot '" + slotKey + "' in GUI " + id);
                        continue;
                    }
                    String material = itemSection.getString("material", "STONE");
                    String name = itemSection.getString("name", "");
                    GuiItem guiItem = new GuiItem(
                            slot,
                            material,
                            name,
                            ColorUtil.colorList(itemSection.getStringList("lore")),
                            itemSection.getStringList("commands"),
                            itemSection.getBoolean("close", false)
                    );
                    items.put(slot, guiItem);
                }
            }
            definitions.put(id, new GuiDefinition(id, title, rows, items));
        }
        plugin.getLogger().info("Loaded " + definitions.size() + " GUI definition(s).");
    }

    public GuiDefinition getGui(String id) {
        return definitions.get(id);
    }

    public Set<String> getGuiIds() {
        return Collections.unmodifiableSet(definitions.keySet());
    }

    private Integer parseSlot(String slotKey) {
        try {
            return Integer.parseInt(slotKey);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
