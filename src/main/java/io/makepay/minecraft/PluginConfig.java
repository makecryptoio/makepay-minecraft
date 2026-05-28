package io.makepay.minecraft;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginConfig {
    private final String backendBaseUrl;
    private final String serverId;
    private final String relayToken;
    private final String checkoutPath;
    private final String entitlementsPath;
    private final String ackPath;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final boolean pollerEnabled;
    private final long pollIntervalTicks;
    private final Map<String, PackageConfig> packages;
    private final MessageConfig messages;

    private PluginConfig(
            String backendBaseUrl,
            String serverId,
            String relayToken,
            String checkoutPath,
            String entitlementsPath,
            String ackPath,
            Duration connectTimeout,
            Duration requestTimeout,
            boolean pollerEnabled,
            long pollIntervalTicks,
            Map<String, PackageConfig> packages,
            MessageConfig messages) {
        this.backendBaseUrl = trimTrailingSlash(backendBaseUrl);
        this.serverId = serverId;
        this.relayToken = relayToken;
        this.checkoutPath = normalizePath(checkoutPath);
        this.entitlementsPath = normalizePath(entitlementsPath);
        this.ackPath = normalizePath(ackPath);
        this.connectTimeout = connectTimeout;
        this.requestTimeout = requestTimeout;
        this.pollerEnabled = pollerEnabled;
        this.pollIntervalTicks = pollIntervalTicks;
        this.packages = Collections.unmodifiableMap(packages);
        this.messages = messages;
    }

    public static PluginConfig load(JavaPlugin plugin) {
        ConfigurationSection backend = requireSection(plugin, "backend");
        ConfigurationSection poller = requireSection(plugin, "poller");

        String backendBaseUrl = backend.getString("base-url", "");
        if (!backendBaseUrl.startsWith("http://") && !backendBaseUrl.startsWith("https://")) {
            plugin.getLogger().warning("backend.base-url should be an absolute HTTP(S) URL.");
        }

        Map<String, PackageConfig> packages = new LinkedHashMap<>();
        ConfigurationSection packageSection = plugin.getConfig().getConfigurationSection("packages");
        if (packageSection != null) {
            for (String packageId : packageSection.getKeys(false)) {
                ConfigurationSection item = packageSection.getConfigurationSection(packageId);
                if (item == null) {
                    continue;
                }

                packages.put(packageId, new PackageConfig(
                        packageId,
                        item.getString("display-name", packageId),
                        item.getLong("amount-minor", 0),
                        item.getString("currency", "USD"),
                        item.getString("description", ""),
                        item.getStringList("commands")));
            }
        }

        MessageConfig messages = new MessageConfig(
                plugin.getConfig().getString("messages.prefix", "<gold>[MakePay]</gold> "),
                plugin.getConfig().getString("messages.checkout-created", "<green>Checkout ready:</green> <aqua>{url}</aqua>"),
                plugin.getConfig().getString("messages.package-not-found", "<red>Unknown package.</red>"),
                plugin.getConfig().getString("messages.no-packages", "<yellow>No MakePay packages are configured.</yellow>"),
                plugin.getConfig().getString("messages.entitlement-granted", "<green>Your MakePay purchase was confirmed.</green>"));

        return new PluginConfig(
                backendBaseUrl,
                backend.getString("server-id", "default"),
                backend.getString("relay-token", ""),
                backend.getString("checkout-path", "/minecraft/checkout"),
                backend.getString("entitlements-path", "/minecraft/entitlements"),
                backend.getString("ack-path", "/minecraft/entitlements/{id}/ack"),
                Duration.ofSeconds(Math.max(1, backend.getLong("connect-timeout-seconds", 10))),
                Duration.ofSeconds(Math.max(1, backend.getLong("request-timeout-seconds", 20))),
                poller.getBoolean("enabled", true),
                Math.max(20L, poller.getLong("interval-seconds", 30) * 20L),
                packages,
                messages);
    }

    public URI checkoutUri() {
        return URI.create(backendBaseUrl + checkoutPath);
    }

    public URI entitlementsUri(String cursor) {
        StringBuilder url = new StringBuilder(backendBaseUrl)
                .append(entitlementsPath)
                .append("?serverId=")
                .append(UrlEncoding.encode(serverId));
        if (cursor != null && !cursor.isBlank()) {
            url.append("&cursor=").append(UrlEncoding.encode(cursor));
        }
        return URI.create(url.toString());
    }

    public URI ackUri(String entitlementId) {
        String path = ackPath.replace("{id}", UrlEncoding.encode(entitlementId));
        return URI.create(backendBaseUrl + path);
    }

    public Optional<PackageConfig> packageById(String packageId) {
        return Optional.ofNullable(packages.get(packageId));
    }

    public Map<String, PackageConfig> packages() {
        return packages;
    }

    public String serverId() {
        return serverId;
    }

    public String relayToken() {
        return relayToken;
    }

    public Duration connectTimeout() {
        return connectTimeout;
    }

    public Duration requestTimeout() {
        return requestTimeout;
    }

    public boolean pollerEnabled() {
        return pollerEnabled;
    }

    public long pollIntervalTicks() {
        return pollIntervalTicks;
    }

    public MessageConfig messages() {
        return messages;
    }

    private static ConfigurationSection requireSection(JavaPlugin plugin, String path) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section == null) {
            throw new IllegalStateException("Missing config section: " + path);
        }
        return section;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    public record PackageConfig(String id, String displayName, long amountMinor, String currency, String description, List<String> commands) {
    }

    public record MessageConfig(
            String prefix,
            String checkoutCreated,
            String packageNotFound,
            String noPackages,
            String entitlementGranted) {
    }
}
