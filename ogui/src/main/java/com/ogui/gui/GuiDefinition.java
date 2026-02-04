package com.ogui.gui;

import com.ogui.util.ColorUtil;
import fr.minuskube.inv.InventoryManager;
import fr.minuskube.inv.SmartInventory;
import java.util.Collections;
import java.util.Map;

public class GuiDefinition {

    private final String id;
    private final String title;
    private final int rows;
    private final Map<Integer, GuiItem> items;

    public GuiDefinition(String id, String title, int rows, Map<Integer, GuiItem> items) {
        this.id = id;
        this.title = title;
        this.rows = rows;
        this.items = items;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getRows() {
        return rows;
    }

    public Map<Integer, GuiItem> getItems() {
        return Collections.unmodifiableMap(items);
    }

    public SmartInventory createInventory(InventoryManager manager) {
        return SmartInventory.builder()
                .id("ogui:" + id)
                .provider(new GuiInventoryProvider(this))
                .size(rows, 9)
                .title(ColorUtil.color(title))
                .manager(manager)
                .build();
    }
}
