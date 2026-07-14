package com.seemorenext.seemorenext.controller;

import com.seemorenext.seemorenext.SeeMoreNext;
import com.seemorenext.seemorenext.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class ViewDistanceController {
    private static final int MAX_UPDATE_ATTEMPTS = 10;
    private final SeeMoreNext plugin;
    private final Map<UUID, Integer> targetViewDistanceMap = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> targetSendDistanceMap = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> viewDistanceUpdateTasks = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> sendDistanceUpdateTasks = new ConcurrentHashMap<>();
    private final ViewDistanceUpdateLogger viewDistanceUpdateLogger;

    public ViewDistanceController(SeeMoreNext plugin) {
        this.plugin = plugin;
        this.viewDistanceUpdateLogger = new ViewDistanceUpdateLogger(plugin);
        plugin.getSchedulerHook().runRepeatingTask(this::cleanMaps, 1200, 1200);
        Bukkit.getPluginManager().registerEvents(new ViewDistanceUpdater(plugin, this), plugin);
    }

    public void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getSchedulerHook().runEntityTaskAsap(() -> {
                setTargetViewDistance(player, player.getClientViewDistance(), false, true);
            }, null, player);
        }
    }

    public void setTargetViewDistance(Player player, int clientViewDistance, boolean testDelay, boolean initialUpdate) {
        int floor = player.getWorld().getSimulationDistance();
        int ceiling = Math.min(plugin.getSeeMoreNextConfig().worldSettings.of(player.getWorld()).maximumViewDistance.get(), 32);

        // Default to the world's view distance if the configured ceiling is negative
        ceiling = ceiling < 0 ? player.getWorld().getViewDistance() : ceiling;

        targetViewDistanceMap.put(player.getUniqueId(), Math.max(floor, Math.min(ceiling, clientViewDistance)));
        targetSendDistanceMap.put(player.getUniqueId(), Math.max(2, Math.min(ceiling, clientViewDistance)) + 1);

        // Update the view distance with a delay if it is being lowered
        long delay = 0;
        try {
            if (testDelay && player.getViewDistance() > targetViewDistanceMap.get(player.getUniqueId())) {
                delay = plugin.getSeeMoreNextConfig().updateDelay.get();
            }
        } catch (Exception ignored) {}

        updateSendDistance(player);
        updateViewDistance(player, delay, clientViewDistance, initialUpdate);
    }

    private void updateSendDistance(Player player) {
        updateDistance(player, 0, 0, targetSendDistanceMap, sendDistanceUpdateTasks, Player::setSendViewDistance);
    }

    private void updateViewDistance(Player player, long delay, int clientViewDistance, boolean initialUpdate) {
        updateDistance(player, delay, 0, targetViewDistanceMap, viewDistanceUpdateTasks, (p, viewDistance) -> {
            if (p.getViewDistance() != viewDistance || initialUpdate) { // always update if we've not seen them before
                p.setViewDistance(viewDistance);
                if (plugin.getSeeMoreNextConfig().logChanges.get()) {
                    viewDistanceUpdateLogger.logUpdate(player, String.format("Set view distance of %s to %s (client view distance is %s).", p.getName(), viewDistance, clientViewDistance));
                }
            }
        });
    }

    private void updateDistance(Player player, long delay, int attempts, Map<UUID, Integer> distanceMap, Map<UUID, ScheduledTask> taskMap, BiConsumer<Player, Integer> distanceConsumer) {
        if (attempts >= MAX_UPDATE_ATTEMPTS) {
            return; // give up if attempted too many times
        }
        Integer distance = distanceMap.get(player.getUniqueId());
        if (distance == null) {
            return; // might be null if the player has left
        }
        taskMap.compute(player.getUniqueId(), (uuid, task) -> {
            if (task != null) {
                task.cancel(); // cancel the previous task in case it is still running
            }
            if (delay > 0) {
                return plugin.getSchedulerHook().runTaskDelayed(() -> updateDistance(player, 0, attempts, distanceMap, taskMap, distanceConsumer), delay);
            }
            CompletableFuture<ScheduledTask> retryTask = new CompletableFuture<>();
            ScheduledTask updateTask = plugin.getSchedulerHook().runEntityTaskAsap(() -> {
                try {
                    distanceConsumer.accept(player, distance);
                } catch (Throwable ex) {

                    // will sometimes fail if the player is not attached to a world yet, so retry after 20 ticks
                    retryTask.complete(plugin.getSchedulerHook().runTask(() -> updateDistance(player, 20, attempts + 1, distanceMap, taskMap, distanceConsumer)));
                }
            }, null, player);
            return retryTask.getNow(updateTask);
        });
    }

    private void cleanMaps() {
        sendDistanceUpdateTasks.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        viewDistanceUpdateTasks.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        targetSendDistanceMap.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        targetViewDistanceMap.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
    }

}
