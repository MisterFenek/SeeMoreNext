package com.seemorenext.seemorenext.command;

import com.seemorenext.seemorenext.SeeMoreNext;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static net.kyori.adventure.text.Component.*;

/**
 * /smn status — show plugin status overview.
 * Available to all players.
 */
public class StatusCommand implements CommandExecutor, TabCompleter {
    private final SeeMoreNext plugin;

    public StatusCommand(SeeMoreNext plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        sender.sendMessage(text("SeeMoreNext v" + plugin.getDescription().getVersion(), NamedTextColor.GRAY));

        int pollingInterval = plugin.getSeeMoreNextConfig().pollingInterval.get();
        sender.sendMessage(text("Polling: ", NamedTextColor.GRAY)
                .append(text(pollingInterval > 0 ? "every " + pollingInterval + " ticks" : "disabled", NamedTextColor.WHITE)));

        sender.sendMessage(text("Players tracked: ", NamedTextColor.GRAY)
                .append(text(String.valueOf(plugin.getViewDistanceController().getTargetViewDistanceMap().size()), NamedTextColor.WHITE)));

        sender.sendMessage(text("Online: ", NamedTextColor.GRAY)
                .append(text(String.valueOf(Bukkit.getOnlinePlayers().size()), NamedTextColor.WHITE)));

        String logLevel = plugin.getSeeMoreNextConfig().getLogLevel();
        sender.sendMessage(text("Log level: ", NamedTextColor.GRAY)
                .append(text(logLevel, NamedTextColor.WHITE)));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
