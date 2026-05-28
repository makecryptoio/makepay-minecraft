package io.makepay.minecraft;

import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class MakePayMinecraftPlugin extends JavaPlugin {
    private PluginConfig pluginConfig;
    private EntitlementStore entitlementStore;
    private BackendClient backendClient;
    private EntitlementExecutor entitlementExecutor;
    private EntitlementPoller entitlementPoller;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadMakePay();

        PluginCommand command = Objects.requireNonNull(getCommand("makepay"), "makepay command missing from plugin.yml");
        MakePayCommand makePayCommand = new MakePayCommand(this);
        command.setExecutor(makePayCommand);
        command.setTabCompleter(makePayCommand);
    }

    @Override
    public void onDisable() {
        if (entitlementPoller != null) {
            entitlementPoller.stop();
        }
    }

    public void reloadMakePay() {
        reloadConfig();

        if (entitlementPoller != null) {
            entitlementPoller.stop();
        }

        pluginConfig = PluginConfig.load(this);
        entitlementStore = new EntitlementStore(this);
        backendClient = new BackendClient(pluginConfig);
        entitlementExecutor = new EntitlementExecutor(this, pluginConfig, entitlementStore, backendClient);
        entitlementPoller = new EntitlementPoller(this, pluginConfig, entitlementStore, backendClient, entitlementExecutor);

        if (pluginConfig.pollerEnabled()) {
            entitlementPoller.start();
        }
    }

    public PluginConfig pluginConfig() {
        return pluginConfig;
    }

    public BackendClient backendClient() {
        return backendClient;
    }

    public EntitlementPoller entitlementPoller() {
        return entitlementPoller;
    }
}
