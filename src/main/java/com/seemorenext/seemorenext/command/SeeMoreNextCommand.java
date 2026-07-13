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

    public SeeMoreNextCommand(SeeMoreNext plugin) {
        this.plugin = plugin;
        this.averageCommand = new AverageCommand(plugin);
        this.reloadCommand = new ReloadCommand(plugin);
        this.playersCommand = new PlayersCommand(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("seemorenext.command.seemore")) {
            sender.sendMessage(NO_PERMISSION);
            return false;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("average")) {
                if (sender.hasPermission("seemorenext.command.average")) {
                    return averageCommand.onCommand(sender, command, label, args);
                } else {
                    sender.sendMessage(NO_PERMISSION);
                    return false;
                }
            }
            if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("seemorenext.command.reload")) {
                    return reloadCommand.onCommand(sender, command, label, args);
                } else {
                    sender.sendMessage(NO_PERMISSION);
                    return false;
                }
            }
            if (args[0].equalsIgnoreCase("players")) {
                if (sender.hasPermission("seemorenext.command.players")) {
                    return playersCommand.onCommand(sender, command, label, args);
                } else {
                    sender.sendMessage(NO_PERMISSION);
                    return false;
                }
            }
        }
        sender.sendMessage(text("SeeMoreNext v" + plugin.getDescription().getVersion(), NamedTextColor.GRAY));
        sender.sendMessage(empty());
        if (sender.hasPermission("seemorenext.command.reload")) {
            sender.sendMessage(text("/seemorenext reload"));
        }
        if (sender.hasPermission("seemorenext.command.average")) {
            sender.sendMessage(text("/seemorenext average"));
        }
        if (sender.hasPermission("seemorenext.command.players")) {
            sender.sendMessage(text("/seemorenext players"));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("seemorenext.command.reload")) {
                suggestions.add("reload");
            }
            if (sender.hasPermission("seemorenext.command.average")) {
                suggestions.add("average");
            }
            if (sender.hasPermission("seemorenext.command.players")) {
                suggestions.add("players");
            }
        }

        return StringUtil.copyPartialMatches(args[args.length - 1], suggestions, new ArrayList<>());
    }
}
