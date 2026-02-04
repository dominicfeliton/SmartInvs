package com.ogui.util;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;

public final class ColorUtil {

    private ColorUtil() {
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static List<String> color(List<String> lines) {
        List<String> colored = new ArrayList<>();
        for (String line : lines) {
            colored.add(color(line));
        }
        return colored;
    }

    public static List<String> colorList(List<String> lines) {
        if (lines == null) {
            return new ArrayList<>();
        }
        return color(lines);
    }
}
