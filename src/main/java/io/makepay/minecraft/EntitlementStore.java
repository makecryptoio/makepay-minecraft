package io.makepay.minecraft;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.configuration.file.YamlConfiguration;

public final class EntitlementStore {
    private static final int MAX_PROCESSED_IDS = 5000;

    private final MakePayMinecraftPlugin plugin;
    private final File file;
    private final Set<String> processedIds = new LinkedHashSet<>();
    private String cursor = "";

    public EntitlementStore(MakePayMinecraftPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "state.yml");
        load();
    }

    public synchronized String cursor() {
        return cursor;
    }

    public synchronized boolean isProcessed(String entitlementId) {
        return entitlementId != null && processedIds.contains(entitlementId);
    }

    public synchronized void markProcessed(String entitlementId, String nextCursor) {
        if (entitlementId != null && !entitlementId.isBlank()) {
            processedIds.add(entitlementId);
        }
        if (nextCursor != null) {
            cursor = nextCursor;
        }
        trim();
        save();
    }

    private void load() {
        if (!file.exists()) {
            return;
        }

        YamlConfiguration state = YamlConfiguration.loadConfiguration(file);
        cursor = state.getString("cursor", "");
        processedIds.clear();
        processedIds.addAll(state.getStringList("processed"));
    }

    private void save() {
        YamlConfiguration state = new YamlConfiguration();
        state.set("cursor", cursor);
        state.set("processed", List.copyOf(processedIds));
        try {
            state.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Unable to save MakePay state: " + e.getMessage());
        }
    }

    private void trim() {
        while (processedIds.size() > MAX_PROCESSED_IDS) {
            String first = processedIds.iterator().next();
            processedIds.remove(first);
        }
    }
}
