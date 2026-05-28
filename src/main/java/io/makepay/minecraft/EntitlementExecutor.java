package io.makepay.minecraft;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class EntitlementExecutor {
    private final MakePayMinecraftPlugin plugin;
    private final PluginConfig config;
    private final EntitlementStore store;
    private final BackendClient backendClient;

    public EntitlementExecutor(
            MakePayMinecraftPlugin plugin,
            PluginConfig config,
            EntitlementStore store,
            BackendClient backendClient) {
        this.plugin = plugin;
        this.config = config;
        this.store = store;
        this.backendClient = backendClient;
    }

    public void grant(BackendClient.Entitlement entitlement, String nextCursor) {
        if (entitlement.id == null || entitlement.id.isBlank() || store.isProcessed(entitlement.id) || !entitlement.isPaid()) {
            return;
        }

        List<String> commands = entitlement.commands.isEmpty()
                ? config.packageById(entitlement.packageId).map(PluginConfig.PackageConfig::commands).orElse(List.of())
                : entitlement.commands;

        if (commands.isEmpty()) {
            plugin.getLogger().warning("No commands configured for MakePay entitlement " + entitlement.id);
            store.markProcessed(entitlement.id, nextCursor);
            return;
        }

        Map<String, String> values = values(entitlement);
        for (String commandTemplate : commands) {
            String command = TemplateRenderer.render(commandTemplate, values);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }

        notifyPlayer(entitlement);
        store.markProcessed(entitlement.id, nextCursor);
        backendClient.acknowledge(entitlement.id).exceptionally(error -> {
            plugin.getLogger().warning("Unable to acknowledge MakePay entitlement " + entitlement.id + ": " + error.getMessage());
            return null;
        });
    }

    private Map<String, String> values(BackendClient.Entitlement entitlement) {
        Map<String, String> values = new HashMap<>();
        values.put("player", entitlement.playerName == null ? "" : entitlement.playerName);
        values.put("uuid", entitlement.playerUuid == null ? "" : entitlement.playerUuid);
        values.put("package", entitlement.packageId == null ? "" : entitlement.packageId);
        values.put("external_id", entitlement.externalId == null ? "" : entitlement.externalId);
        values.put("entitlement_id", entitlement.id == null ? "" : entitlement.id);
        return values;
    }

    private void notifyPlayer(BackendClient.Entitlement entitlement) {
        if (entitlement.playerUuid == null || entitlement.playerUuid.isBlank()) {
            return;
        }

        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(java.util.UUID.fromString(entitlement.playerUuid));
            Player player = offlinePlayer.getPlayer();
            if (player != null && player.isOnline()) {
                player.sendMessage(Messages.render(config.messages().prefix() + config.messages().entitlementGranted(), Map.of()));
            }
        } catch (IllegalArgumentException ignored) {
            plugin.getLogger().fine("MakePay entitlement had invalid player UUID: " + entitlement.playerUuid);
        }
    }
}
