package io.makepay.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

final class TemplateRendererTest {
    @Test
    void rendersKnownPlaceholders() {
        String rendered = TemplateRenderer.render(
                "lp user {player} parent add {package} #{external_id}",
                Map.of("player", "Alex", "package", "vip", "external_id", "order_123"));

        assertEquals("lp user Alex parent add vip #order_123", rendered);
    }

    @Test
    void missingValuesBecomeEmptyStrings() {
        String rendered = TemplateRenderer.render("say {player}{suffix}", Map.of("player", "Steve", "suffix", ""));

        assertEquals("say Steve", rendered);
    }
}
