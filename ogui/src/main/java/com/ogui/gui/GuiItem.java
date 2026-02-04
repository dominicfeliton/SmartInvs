package com.ogui.gui;

import java.util.Collections;
import java.util.List;

public class GuiItem {

    private final int slot;
    private final String material;
    private final String name;
    private final List<String> lore;
    private final List<String> commands;
    private final boolean closeOnClick;

    public GuiItem(int slot, String material, String name, List<String> lore, List<String> commands, boolean closeOnClick) {
        this.slot = slot;
        this.material = material;
        this.name = name;
        this.lore = lore == null ? Collections.emptyList() : lore;
        this.commands = commands == null ? Collections.emptyList() : commands;
        this.closeOnClick = closeOnClick;
    }

    public int getSlot() {
        return slot;
    }

    public String getMaterial() {
        return material;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return Collections.unmodifiableList(lore);
    }

    public List<String> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public boolean isCloseOnClick() {
        return closeOnClick;
    }
}
