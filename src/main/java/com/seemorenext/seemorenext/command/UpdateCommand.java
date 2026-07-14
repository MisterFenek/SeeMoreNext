package com.seemorenext.seemorenext.command;

import com.seemorenext.seemorenext.SeeMoreNext;
import net.kyori.adventure.text.format.NamedTextColor;
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
 * /smn update — force-update all online players' view distances.
 * Available to ops only.
 */
public class UpdateCommand implements CommandExecutor, TabCompleter {
    private final SeeMoreNext plugin;

    public UpdateCommand(SeeMoreNext plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        int playerCount = org.bukkit.Bukkit.getOnlinePlayers().size();
        plugin.getViewDistanceController().updateAllPlayers();
        sender.sendMessage(text("Forced view distance update for " + playerCount + " online player(s).", NamedTextColor.GRAY));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
