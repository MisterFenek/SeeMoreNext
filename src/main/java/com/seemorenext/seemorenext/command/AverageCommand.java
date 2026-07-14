package com.seemorenext.seemorenext.command;

import com.seemorenext.seemorenext.SeeMoreNext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.*;

public class AverageCommand implements CommandExecutor, TabCompleter {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.0");
    private final SeeMoreNext plugin;

    public AverageCommand(SeeMoreNext plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        Map<org.bukkit.World, AtomicInteger> chunkCountMap = new ConcurrentHashMap<>();
        Map<org.bukkit.World, AtomicInteger> playerCountMap = new ConcurrentHashMap<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            CompletableFuture<Void> playerFuture = new CompletableFuture<>();
            future = future.thenCompose(v -> playerFuture);
            plugin.getSchedulerHook().runEntityTaskAsap(() -> {
                try {
                    int viewDistance = player.getViewDistance();
                    chunkCountMap.compute(player.getWorld(), (world, chunkCount) -> {
                        if (chunkCount == null) {
                            chunkCount = new AtomicInteger();
                        }
                        chunkCount.addAndGet((2 * viewDistance + 1) * (2 * viewDistance + 1));
                        playerCountMap.computeIfAbsent(world, w -> new AtomicInteger()).getAndIncrement();
                        return chunkCount;
                    });
                } catch (Throwable ignored) {}
                playerFuture.complete(null);
            }, () -> playerFuture.complete(null), player);
        }

        future.thenRun(() -> {
            // Collect per-player view distances for histogram
            Map<Integer, AtomicInteger> histogram = new TreeMap<>(Collections.reverseOrder());
            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    int vd = player.getViewDistance();
                    histogram.computeIfAbsent(vd, k -> new AtomicInteger()).incrementAndGet();
                } catch (Throwable ignored) {}
            }

            Map<org.bukkit.World, Double> effectiveViewDistanceMap = new HashMap<>();
            int totalChunkCount = 0;
            int totalPlayerCount = 0;
            for (org.bukkit.World world : chunkCountMap.keySet()) {
                int chunkCount = chunkCountMap.get(world).get();
                int playerCount = playerCountMap.get(world).get();
                if (playerCount == 0) continue;

                double effectiveViewDistance = (Math.sqrt((double) chunkCount / (double) playerCount) - 1.0) / 2.0;
                effectiveViewDistanceMap.put(world, effectiveViewDistance);

                totalChunkCount += chunkCount;
                totalPlayerCount += playerCount;
            }
            double totalEffectiveViewDistance = totalPlayerCount == 0 ? 0 : (Math.sqrt((double) totalChunkCount / (double) totalPlayerCount) - 1.0) / 2.0;

            if (totalPlayerCount == 0) {
                sender.sendMessage(text("There are no players online.", NamedTextColor.GRAY));
                return;
            }

            sender.sendMessage(text("Effective average view distance:", NamedTextColor.GRAY));
            sender.sendMessage(
                    text("All worlds: ", NamedTextColor.GOLD)
                            .append(text(formatViewDistance(totalEffectiveViewDistance), NamedTextColor.RED))
            );
            sender.sendMessage(empty());
            sender.sendMessage(text("--------------------------"));
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                double effectiveViewDistance = effectiveViewDistanceMap.getOrDefault(world, 0.0);
                sender.sendMessage(
                        text(world.getName() + ": ", NamedTextColor.GOLD)
                                .append(text(formatViewDistance(effectiveViewDistance), NamedTextColor.RED))
                );
            }
            sender.sendMessage(text("--------------------------"));

            // Histogram
            sender.sendMessage(empty());
            sender.sendMessage(text("View distance distribution:", NamedTextColor.GRAY));
            int maxCount = histogram.values().stream().mapToInt(AtomicInteger::get).max().orElse(1);
            int barWidth = 20;
            for (Map.Entry<Integer, AtomicInteger> entry : histogram.entrySet()) {
                int vd = entry.getKey();
                int count = entry.getValue().get();
                int barLen = (int) Math.round((double) count / maxCount * barWidth);
                StringBuilder bar = new StringBuilder();
                for (int i = 0; i < barLen; i++) bar.append("█");
                sender.sendMessage(
                        text(String.format("  %2d: ", vd), NamedTextColor.GOLD)
                                .append(text(bar.toString(), NamedTextColor.AQUA))
                                .append(text(" " + count, NamedTextColor.WHITE))
                );
            }
        });
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }

    private static String formatViewDistance(double viewDistance) {
        if (viewDistance == 0) return "-";
        return DECIMAL_FORMAT.format(viewDistance);
    }

}
