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
    private final Map<UUID, Integer> lastKnownClientViewDistance = new ConcurrentHashMap<>();
    private final ViewDistanceUpdateLogger viewDistanceUpdateLogger;
    private ScheduledTask pollingTask;

    public ViewDistanceController(SeeMoreNext plugin) {
        this.plugin = plugin;
        this.viewDistanceUpdateLogger = new ViewDistanceUpdateLogger(plugin);
        plugin.getSchedulerHook().runRepeatingTask(this::cleanMaps, 1200, 1200);
        Bukkit.getPluginManager().registerEvents(new ViewDistanceUpdater(plugin, this), plugin);
        startPolling();
    }

    /**
     * Start or restart the periodic polling task based on config.
     */
    public void startPolling() {
        if (pollingTask != null) {
            pollingTask.cancel();
            pollingTask = null;
        }
        int interval = plugin.getSeeMoreNextConfig().pollingInterval.get();
        if (interval > 0) {
            pollingTask = plugin.getSchedulerHook().runRepeatingTask(this::pollClientViewDistances, interval, interval);
        }
    }

    /**
     * Force-update all online players' view distances (bypasses deduplication).
     * Used by /smn update and /smn reload.
     */
    public void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getSchedulerHook().runEntityTaskAsap(() -> {
                setTargetViewDistance(player, player.getClientViewDistance(), false, true);
            }, null, player);
        }
    }

    /**
     * Force-update a single player's view distance (bypasses deduplication).
     * Used by /smn refresh for individual players.
     */
    public void updateSinglePlayer(Player player) {
        plugin.getSchedulerHook().runEntityTaskAsap(() -> {
            int clientViewDistance;
            try {
                clientViewDistance = player.getClientViewDistance();
            } catch (Throwable t) {
                clientViewDistance = player.getViewDistance();
            }
            if (clientViewDistance <= 0) clientViewDistance = player.getViewDistance();
            setTargetViewDistance(player, clientViewDistance, false, true);
        }, null, player);
    }

    public void setTargetViewDistance(Player player, int clientViewDistance, boolean testDelay, boolean initialUpdate) {
        String logLevel = plugin.getSeeMoreNextConfig().getLogLevel();

        // If wait-for-client-settings is disabled and this is the first contact, skip
        if (!plugin.getSeeMoreNextConfig().waitForClientSettings.get() && !lastKnownClientViewDistance.containsKey(player.getUniqueId())) {
            lastKnownClientViewDistance.put(player.getUniqueId(), clientViewDistance);
            return;
        }

        int floor = player.getWorld().getSimulationDistance();
        int ceiling = Math.min(plugin.getSeeMoreNextConfig().worldSettings.of(player.getWorld()).maximumViewDistance.get(), 32);

        // Default to the world's view distance if the configured ceiling is negative
        ceiling = ceiling < 0 ? player.getWorld().getViewDistance() : ceiling;

        // Apply minimum view distance if configured
        int configuredMin = plugin.getSeeMoreNextConfig().worldSettings.of(player.getWorld()).minimumViewDistance.get();
        floor = Math.max(floor, configuredMin);

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
        String logLevel = plugin.getSeeMoreNextConfig().getLogLevel();
        boolean shouldLog = !"off".equals(logLevel);

        updateDistance(player, delay, 0, targetViewDistanceMap, viewDistanceUpdateTasks, (p, viewDistance) -> {
            if (p.getViewDistance() != viewDistance || initialUpdate) {
                p.setViewDistance(viewDistance);
                if (shouldLog) {
                    if ("all".equals(logLevel) || p.getViewDistance() != viewDistance) {
                        viewDistanceUpdateLogger.logUpdate(player, String.format("Set view distance of %s to %s (client view distance is %s).", p.getName(), viewDistance, clientViewDistance));
                    }
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

    /**
     * Periodically checks every online player's client view distance against
     * the last known value. If a change is detected (one that the event-based
     * listener may have missed), pushes an update to the controller.
     */
    private void pollClientViewDistances() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            int clientViewDistance;
            try {
                clientViewDistance = player.getClientViewDistance();
            } catch (Throwable t) {
                continue;
            }
            if (clientViewDistance <= 0) continue;

            Integer lastKnown = lastKnownClientViewDistance.get(uuid);
            if (lastKnown == null || lastKnown != clientViewDistance) {
                lastKnownClientViewDistance.put(uuid, clientViewDistance);
                plugin.getSchedulerHook().runEntityTaskAsap(() ->
                    setTargetViewDistance(player, clientViewDistance, false, false)
                , null, player);
            }
        }
    }

    private void cleanMaps() {
        sendDistanceUpdateTasks.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        viewDistanceUpdateTasks.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        targetSendDistanceMap.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        targetViewDistanceMap.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        lastKnownClientViewDistance.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
    }

    /**
     * Get a snapshot of the current tracking state for debug/status commands.
     */
    public Map<UUID, Integer> getTargetViewDistanceMap() {
        return targetViewDistanceMap;
    }

    public Map<UUID, Integer> getLastKnownClientViewDistance() {
        return lastKnownClientViewDistance;
    }

}
