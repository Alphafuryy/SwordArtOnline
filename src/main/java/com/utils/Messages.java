package com.utils;

import com.SwordArtOnline;
import org.bukkit.ChatColor;
import org.jspecify.annotations.Nullable;

public class Messages {

    public static String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String get(String key) {
        return format(SwordArtOnline.getInstance().getMessagesManager().getMessage(key));
    }

    public static String get(String key, String... replacements) {
        String msg = get(key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }
    public static String getRegion(String key, String... replacements) {
        String msg = format(SwordArtOnline.getInstance().getMessagesManager().getMessage("region", key));
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

}
