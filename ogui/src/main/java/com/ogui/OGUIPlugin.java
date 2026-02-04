package com.ogui;

import com.ogui.command.OGUICommand;
import com.ogui.gui.GuiRegistry;
import fr.minuskube.inv.InventoryManager;
import org.bukkit.plugin.java.JavaPlugin;

public class OGUIPlugin extends JavaPlugin {

    private InventoryManager inventoryManager;
    private GuiRegistry guiRegistry;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("guis.yml", false);

        inventoryManager = new InventoryManager(this);
        inventoryManager.init();

        guiRegistry = new GuiRegistry(this);
        guiRegistry.reload();

        OGUICommand command = new OGUICommand(this);
        if (getCommand("ogui") != null) {
            getCommand("ogui").setExecutor(command);
            getCommand("ogui").setTabCompleter(command);
        }
    }

    @Override
    public void onDisable() {
        inventoryManager = null;
        guiRegistry = null;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public GuiRegistry getGuiRegistry() {
        return guiRegistry;
    }
}
