package io.makepay.minecraft;

import java.util.Map;

public final class TemplateRenderer {
    private TemplateRenderer() {
    }

    public static String render(String template, Map<String, String> values) {
        String rendered = template == null ? "" : template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return rendered;
    }
}
