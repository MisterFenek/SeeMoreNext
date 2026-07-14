package com.seemorenext.seemorenext.command;

import com.seemorenext.seemorenext.SeeMoreNext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.kyori.adventure.text.Component.*;

public class SeeMoreNextCommand implements CommandExecutor, TabCompleter {
    private static final Component NO_PERMISSION = text("You don't have permission to use this command.", NamedTextColor.RED);
    private final SeeMoreNext plugin;
    private final AverageCommand averageCommand;
    private final ReloadCommand reloadCommand;
    private final PlayersCommand playersCommand;
    private final UpdateCommand updateCommand;
    private final RefreshCommand refreshCommand;
    private final DebugInfoCommand debugInfoCommand;
    private final StatusCommand statusCommand;

    public SeeMoreNextCommand(SeeMoreNext plugin) {
        this.plugin = plugin;
        this.averageCommand = new AverageCommand(plugin);
        this.reloadCommand = new ReloadCommand(plugin);
        this.playersCommand = new PlayersCommand(plugin);
        this.updateCommand = new UpdateCommand(plugin);
        this.refreshCommand = new RefreshCommand(plugin);
        this.debugInfoCommand = new DebugInfoCommand(plugin);
        this.statusCommand = new StatusCommand(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("seemorenext.command.seemore")) {
            sender.sendMessage(NO_PERMISSION);
            return false;
        }

        if (args.length > 0) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "average":
                    if (sender.hasPermission("seemorenext.command.average")) {
                        return averageCommand.onCommand(sender, command, label, args);
                    }
                    break;
                case "reload":
                    if (sender.hasPermission("seemorenext.command.reload")) {
                        return reloadCommand.onCommand(sender, command, label, args);
                    }
                    break;
                case "players":
                    if (sender.hasPermission("seemorenext.command.players")) {
                        return playersCommand.onCommand(sender, command, label, args);
                    }
                    break;
                case "update":
                    if (sender.hasPermission("seemorenext.command.update")) {
                        return updateCommand.onCommand(sender, command, label, args);
                    }
                    break;
                case "refresh":
                    if (sender.hasPermission("seemorenext.command.refresh")) {
                        return refreshCommand.onCommand(sender, command, label, args);
                    }
                    break;
                case "debug-info":
                    if (sender.hasPermission("seemorenext.command.debug-info")) {
                        return debugInfoCommand.onCommand(sender, command, label, args);
                    }
                    break;
                case "status":
                    if (sender.hasPermission("seemorenext.command.status")) {
                        return statusCommand.onCommand(sender, command, label, args);
                    }
                    break;
                default:
                    break;
            }
            sender.sendMessage(NO_PERMISSION);
            return false;
        }

        // Show help
        sender.sendMessage(text("SeeMoreNext v" + plugin.getDescription().getVersion(), NamedTextColor.GRAY));
        sender.sendMessage(empty());
        if (sender.hasPermission("seemorenext.command.reload")) {
            sender.sendMessage(text("/smn reload", NamedTextColor.WHITE).append(text(" — Reload config", NamedTextColor.GRAY)));
        }
        if (sender.hasPermission("seemorenext.command.update")) {
            sender.sendMessage(text("/smn update", NamedTextColor.WHITE).append(text(" — Force update all players", NamedTextColor.GRAY)));
        }
        if (sender.hasPermission("seemorenext.command.refresh")) {
            sender.sendMessage(text("/smn refresh", NamedTextColor.WHITE).append(text(" — Refresh your own view distance", NamedTextColor.GRAY)));
        }
        if (sender.hasPermission("seemorenext.command.average")) {
            sender.sendMessage(text("/smn average", NamedTextColor.WHITE).append(text(" — Average view distance", NamedTextColor.GRAY)));
        }
        if (sender.hasPermission("seemorenext.command.players")) {
            sender.sendMessage(text("/smn players", NamedTextColor.WHITE).append(text(" — Players by view distance", NamedTextColor.GRAY)));
        }
        if (sender.hasPermission("seemorenext.command.status")) {
            sender.sendMessage(text("/smn status", NamedTextColor.WHITE).append(text(" — Plugin status", NamedTextColor.GRAY)));
        }
        if (sender.hasPermission("seemorenext.command.debug-info")) {
            sender.sendMessage(text("/smn debug-info", NamedTextColor.WHITE).append(text(" — Debug information", NamedTextColor.GRAY)));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            String[] subs = {"reload", "update", "refresh", "average", "players", "status", "debug-info"};
            for (String sub : subs) {
                if (sender.hasPermission("seemorenext.command." + sub)) {
                    suggestions.add(sub);
                }
            }
        }
        return StringUtil.copyPartialMatches(args[args.length - 1], suggestions, new ArrayList<>());
    }
}
