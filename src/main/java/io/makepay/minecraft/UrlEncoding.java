package io.makepay.minecraft;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class UrlEncoding {
    private UrlEncoding() {
    }

    public static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
