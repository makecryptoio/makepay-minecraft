package io.makepay.minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MakePayCommand implements CommandExecutor, TabCompleter {
    private final MakePayMinecraftPlugin plugin;

    public MakePayCommand(MakePayMinecraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "buy" -> buy(sender, args);
            case "packages" -> packages(sender);
            case "reload" -> reload(sender);
            case "poll" -> poll(sender);
            default -> sendHelp(sender, label);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("buy", "packages", "help"));
            if (sender.hasPermission("makepay.admin")) {
                options.add("reload");
                options.add("poll");
            }
            return options.stream().filter(option -> option.startsWith(args[0].toLowerCase())).toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("buy")) {
            return plugin.pluginConfig().packages().keySet().stream()
                    .filter(id -> id.startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return List.of();
    }

    private void buy(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can create checkout links.");
            return;
        }

        if (!sender.hasPermission("makepay.use")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use MakePay.");
            return;
        }

        if (args.length < 2) {
            packages(sender);
            return;
        }

        plugin.pluginConfig().packageById(args[1]).ifPresentOrElse(packageConfig -> {
            player.sendMessage(ChatColor.YELLOW + "Creating MakePay checkout...");
            plugin.backendClient().createCheckout(player, packageConfig)
                    .whenComplete((response, error) -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (error != null) {
                            player.sendMessage(ChatColor.RED + "MakePay checkout failed: " + rootMessage(error));
                            return;
                        }
                        if (response == null || !response.isValid()) {
                            player.sendMessage(ChatColor.RED + "MakePay backend did not return a checkout URL.");
                            return;
                        }
                        sendCheckout(player, response.checkoutUrl);
                    }));
        }, () -> sender.sendMessage(Messages.render(
                plugin.pluginConfig().messages().prefix() + plugin.pluginConfig().messages().packageNotFound(),
                Map.of())));
    }

    private void packages(CommandSender sender) {
        if (plugin.pluginConfig().packages().isEmpty()) {
            sender.sendMessage(Messages.render(
                    plugin.pluginConfig().messages().prefix() + plugin.pluginConfig().messages().noPackages(),
                    Map.of()));
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "MakePay packages:");
        for (PluginConfig.PackageConfig packageConfig : plugin.pluginConfig().packages().values()) {
            sender.sendMessage(ChatColor.YELLOW + " - " + packageConfig.id() + " "
                    + ChatColor.WHITE + packageConfig.displayName()
                    + ChatColor.GRAY + " " + packageConfig.amountMinor() + " " + packageConfig.currency());
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("makepay.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to reload MakePay.");
            return;
        }
        plugin.reloadMakePay();
        sender.sendMessage(ChatColor.GREEN + "MakePay config reloaded.");
    }

    private void poll(CommandSender sender) {
        if (!sender.hasPermission("makepay.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to poll MakePay.");
            return;
        }
        plugin.entitlementPoller().poll();
        sender.sendMessage(ChatColor.GREEN + "MakePay entitlement poll triggered.");
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " packages");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " buy <package>");
        if (sender.hasPermission("makepay.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " poll");
        }
    }

    private void sendCheckout(Player player, String checkoutUrl) {
        String rendered = Messages.render(
                        plugin.pluginConfig().messages().prefix() + plugin.pluginConfig().messages().checkoutCreated(),
                        Map.of("url", checkoutUrl));
        TextComponent message = new TextComponent(rendered);
        message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, checkoutUrl));
        message.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Open MakePay checkout").color(ChatColor.GREEN).create()));
        player.spigot().sendMessage(message);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable instanceof CompletionException && throwable.getCause() != null
                ? throwable.getCause()
                : throwable;
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
