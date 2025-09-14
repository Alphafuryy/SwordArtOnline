package com.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String colorize(String text) {
        if (text == null) return "";

        // Convert hex first
        Matcher matcher = HEX_PATTERN.matcher(text);
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            ChatColor hexColor = ChatColor.of("#" + hexCode);
            text = text.replace("&#" + hexCode, hexColor.toString());
        }

        // Then legacy & codes
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
