package io.makepay.minecraft;

import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.scheduler.BukkitTask;

public final class EntitlementPoller {
    private final MakePayMinecraftPlugin plugin;
    private final PluginConfig config;
    private final EntitlementStore store;
    private final BackendClient backendClient;
    private final EntitlementExecutor entitlementExecutor;
    private final AtomicBoolean polling = new AtomicBoolean(false);
    private BukkitTask task;

    public EntitlementPoller(
            MakePayMinecraftPlugin plugin,
            PluginConfig config,
            EntitlementStore store,
            BackendClient backendClient,
            EntitlementExecutor entitlementExecutor) {
        this.plugin = plugin;
        this.config = config;
        this.store = store;
        this.backendClient = backendClient;
        this.entitlementExecutor = entitlementExecutor;
    }

    public void start() {
        stop();
        task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::poll,
                40L,
                config.pollIntervalTicks());
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void poll() {
        if (!polling.compareAndSet(false, true)) {
            return;
        }

        backendClient.fetchEntitlements(store.cursor())
                .whenComplete((response, error) -> {
                    polling.set(false);
                    if (error != null) {
                        plugin.getLogger().warning("MakePay entitlement poll failed: " + error.getMessage());
                        return;
                    }

                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        for (BackendClient.Entitlement entitlement : response.entitlements()) {
                            entitlementExecutor.grant(entitlement, response.nextCursor());
                        }
                        if (response.entitlements().isEmpty()) {
                            store.markProcessed("", response.nextCursor());
                        }
                    });
                });
    }
}
