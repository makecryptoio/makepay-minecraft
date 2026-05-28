package io.makepay.minecraft;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

public final class BackendClient {
    private static final Gson GSON = new Gson();

    private final PluginConfig config;
    private final HttpClient httpClient;

    public BackendClient(PluginConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .build();
    }

    public CompletableFuture<CheckoutResponse> createCheckout(Player player, PluginConfig.PackageConfig packageConfig) {
        JsonObject payload = new JsonObject();
        payload.addProperty("serverId", config.serverId());
        payload.addProperty("playerUuid", player.getUniqueId().toString());
        payload.addProperty("playerName", player.getName());
        payload.addProperty("packageId", packageConfig.id());
        payload.addProperty("displayName", packageConfig.displayName());
        payload.addProperty("amountMinor", packageConfig.amountMinor());
        payload.addProperty("currency", packageConfig.currency());
        payload.addProperty("description", packageConfig.description());

        HttpRequest request = requestBuilder(config.checkoutUri())
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
                .build();

        return send(request).thenApply(body -> GSON.fromJson(body, CheckoutResponse.class));
    }

    public CompletableFuture<EntitlementsResponse> fetchEntitlements(String cursor) {
        HttpRequest request = requestBuilder(config.entitlementsUri(cursor))
                .GET()
                .build();

        return send(request).thenApply(this::parseEntitlements);
    }

    public CompletableFuture<Void> acknowledge(String entitlementId) {
        JsonObject payload = new JsonObject();
        payload.addProperty("serverId", config.serverId());
        payload.addProperty("entitlementId", entitlementId);
        payload.addProperty("acknowledgedAt", Instant.now().toString());

        HttpRequest request = requestBuilder(config.ackUri(entitlementId))
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
                .build();

        return send(request).thenApply(ignored -> null);
    }

    private HttpRequest.Builder requestBuilder(URI uri) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(config.requestTimeout())
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "makepay-minecraft/0.1.0");

        if (!config.relayToken().isBlank()) {
            builder.header("X-MakePay-Relay-Token", config.relayToken());
        }

        return builder;
    }

    private CompletableFuture<String> send(HttpRequest request) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int status = response.statusCode();
                    if (status < 200 || status >= 300) {
                        throw new BackendException("Backend returned HTTP " + status + ": " + response.body());
                    }
                    return response.body();
                });
    }

    private EntitlementsResponse parseEntitlements(String body) {
        JsonObject root = GSON.fromJson(body, JsonObject.class);
        String nextCursor = root.has("nextCursor") && !root.get("nextCursor").isJsonNull()
                ? root.get("nextCursor").getAsString()
                : "";
        List<Entitlement> entitlements = new ArrayList<>();
        JsonArray items = root.has("entitlements") && root.get("entitlements").isJsonArray()
                ? root.getAsJsonArray("entitlements")
                : new JsonArray();

        for (int i = 0; i < items.size(); i++) {
            JsonObject item = items.get(i).getAsJsonObject();
            Entitlement entitlement = new Entitlement();
            entitlement.id = string(item, "id");
            entitlement.externalId = string(item, "externalId");
            entitlement.playerUuid = string(item, "playerUuid");
            entitlement.playerName = string(item, "playerName");
            entitlement.packageId = string(item, "packageId");
            entitlement.status = string(item, "status");
            if (item.has("commands") && item.get("commands").isJsonArray()) {
                for (int j = 0; j < item.getAsJsonArray("commands").size(); j++) {
                    entitlement.commands.add(item.getAsJsonArray("commands").get(j).getAsString());
                }
            }
            entitlements.add(entitlement);
        }

        return new EntitlementsResponse(nextCursor, entitlements);
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    public static final class CheckoutResponse {
        public String checkoutUrl;
        public String externalId;
        public String expiresAt;

        public boolean isValid() {
            return checkoutUrl != null && !checkoutUrl.isBlank();
        }
    }

    public static final class Entitlement {
        public String id;
        public String externalId;
        public String playerUuid;
        public String playerName;
        public String packageId;
        public String status;
        public List<String> commands = new ArrayList<>();

        public boolean isPaid() {
            return status == null || status.isBlank() || status.equalsIgnoreCase("paid") || status.equalsIgnoreCase("completed");
        }
    }

    public record EntitlementsResponse(String nextCursor, List<Entitlement> entitlements) {
    }

    public static final class BackendException extends RuntimeException {
        public BackendException(String message) {
            super(message);
        }

        public BackendException(String message, IOException cause) {
            super(message, cause);
        }
    }
}
