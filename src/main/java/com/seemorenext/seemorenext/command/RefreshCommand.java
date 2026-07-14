package com.seemorenext.seemorenext.command;

import com.seemorenext.seemorenext.SeeMoreNext;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static net.kyori.adventure.text.Component.*;

/**
 * /smn refresh — force-refresh the player's own view distance.
 * Available to all players.
 */
public class RefreshCommand implements CommandExecutor, TabCompleter {
    private final SeeMoreNext plugin;

    public RefreshCommand(SeeMoreNext plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(text("This command can only be used by players.", NamedTextColor.RED));
            return false;
        }

        Player player = (Player) sender;
        plugin.getViewDistanceController().updateSinglePlayer(player);

        int current;
        try {
            current = player.getViewDistance();
        } catch (Throwable t) {
            current = -1;
        }

        if ("off".equals(plugin.getSeeMoreNextConfig().getLogLevel())) {
            sender.sendMessage(text("View distance refreshed.", NamedTextColor.GRAY));
        } else {
            sender.sendMessage(text("View distance refreshed. Current: " + current + " chunks.", NamedTextColor.GRAY));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
