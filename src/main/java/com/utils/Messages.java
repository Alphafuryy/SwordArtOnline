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
    public static String getFromTask(String message) {
        if (message.startsWith("[MSG]:")) {
            String key = message.replace("[MSG]:", "").trim();
            return get(key);
        }
        return format(message);
    }

}
