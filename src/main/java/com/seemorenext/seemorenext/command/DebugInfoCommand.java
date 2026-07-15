package com.seemorenext.seemorenext.command;

import com.seemorenext.seemorenext.SeeMoreNext;
import com.seemorenext.seemorenext.config.SeeMoreNextConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
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
 * /smn debug-info — show all config values, plugin status, and current view distance.
 * Ops only.
 */
public class DebugInfoCommand implements CommandExecutor, TabCompleter {
    private final SeeMoreNext plugin;

    public DebugInfoCommand(SeeMoreNext plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        SeeMoreNextConfig config = plugin.getSeeMoreNextConfig();

        sender.sendMessage(text("=== SeeMoreNext Debug Info ===", NamedTextColor.GOLD));
        sender.sendMessage(text("Version: ", NamedTextColor.GRAY).append(text(plugin.getDescription().getVersion(), NamedTextColor.WHITE)));

        // Config values
        sender.sendMessage(text("--- Config ---", NamedTextColor.YELLOW));
        sender.sendMessage(text("  update-delay: ", NamedTextColor.GRAY).append(text(config.updateDelay.get() + " ticks", NamedTextColor.WHITE)));
        sender.sendMessage(text("  log-changes: ", NamedTextColor.GRAY).append(text(config.getLogLevel(), NamedTextColor.WHITE)));
        sender.sendMessage(text("  wait-for-client-settings: ", NamedTextColor.GRAY).append(text(String.valueOf(config.waitForClientSettings.get()), NamedTextColor.WHITE)));
        sender.sendMessage(text("  polling-interval: ", NamedTextColor.GRAY).append(text(config.pollingInterval.get() + " ticks", NamedTextColor.WHITE)));
        sender.sendMessage(text("  options-change-retries: ", NamedTextColor.GRAY).append(text(config.getOptionsChangeRetries().toString(), NamedTextColor.WHITE)));

        // Per-world settings
        sender.sendMessage(text("--- World Settings ---", NamedTextColor.YELLOW));
        for (World world : Bukkit.getWorlds()) {
            try {
                SeeMoreNextConfig.WorldSettings ws = config.worldSettings.of(world);
                sender.sendMessage(text("  " + world.getName() + ": ", NamedTextColor.GRAY)
                        .append(text("max=" + ws.maximumViewDistance.get(), NamedTextColor.WHITE))
                        .append(text(", min=" + ws.minimumViewDistance.get(), NamedTextColor.WHITE)));
            } catch (Throwable t) {
                sender.sendMessage(text("  " + world.getName() + ": error reading config", NamedTextColor.RED));
            }
        }

        // Plugin status
        sender.sendMessage(text("--- Status ---", NamedTextColor.YELLOW));
        sender.sendMessage(text("  Online players: ", NamedTextColor.GRAY).append(text(String.valueOf(Bukkit.getOnlinePlayers().size()), NamedTextColor.WHITE)));
        sender.sendMessage(text("  Tracking: ", NamedTextColor.GRAY)
                .append(text(plugin.getViewDistanceController().getTargetViewDistanceMap().size() + " players", NamedTextColor.WHITE)));
        boolean geyser = plugin.getViewDistanceController().getGeyserProvider().isAvailable();
        sender.sendMessage(text("  Geyser: ", NamedTextColor.GRAY).append(text(geyser ? "detected" : "not found", geyser ? NamedTextColor.GREEN : NamedTextColor.GRAY)));

        // Current sender's view distance
        if (sender instanceof Player) {
            Player player = (Player) sender;
            int clientVD = -1;
            try {
                clientVD = player.getClientViewDistance();
            } catch (Throwable ignored) {}
            int serverVD = -1;
            try {
                serverVD = player.getViewDistance();
            } catch (Throwable ignored) {}
            boolean isBedrock = plugin.getViewDistanceController().getGeyserProvider().isBedrockPlayer(player.getUniqueId());
            int geyserVD = isBedrock ? plugin.getViewDistanceController().getGeyserProvider().getBedrockRenderDistance(player.getUniqueId()) : -1;
            sender.sendMessage(text("--- Your View Distance ---", NamedTextColor.YELLOW));
            if (isBedrock) {
                sender.sendMessage(text("  Platform: ", NamedTextColor.GRAY).append(text("Bedrock (Geyser)", NamedTextColor.AQUA)));
                sender.sendMessage(text("  Bedrock RD: ", NamedTextColor.GRAY).append(text(String.valueOf(geyserVD), NamedTextColor.WHITE)));
            } else {
                sender.sendMessage(text("  Platform: ", NamedTextColor.GRAY).append(text("Java", NamedTextColor.GREEN)));
            }
            sender.sendMessage(text("  Client VD: ", NamedTextColor.GRAY).append(text(String.valueOf(clientVD), NamedTextColor.WHITE)));
            sender.sendMessage(text("  Server VD: ", NamedTextColor.GRAY).append(text(String.valueOf(serverVD), NamedTextColor.WHITE)));
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
