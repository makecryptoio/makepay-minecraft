package io.makepay.minecraft;

import java.util.Map;
import org.bukkit.ChatColor;

public final class Messages {
    private Messages() {
    }

    public static String render(String template, Map<String, String> values) {
        return ChatColor.translateAlternateColorCodes('&', TemplateRenderer.render(template, values));
    }
}
