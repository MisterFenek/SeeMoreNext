package com.seemorenext.seemorenext.controller;

import com.destroystokyo.paper.event.player.PlayerClientOptionsChangeEvent;
import com.seemorenext.seemorenext.SeeMoreNext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for client view-distance changes and pushes them to the controller.
 */
public class ViewDistanceUpdater implements Listener {
    private final SeeMoreNext plugin;
    private final ViewDistanceController controller;
    private final Set<UUID> seenBefore = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> lastAppliedClientDistance = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> pendingRetries = new ConcurrentHashMap<>();

    public ViewDistanceUpdater(SeeMoreNext plugin, ViewDistanceController viewDistanceController) {
        this.plugin = plugin;
        this.controller = viewDistanceController;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onJoin(PlayerJoinEvent event) {
        // Immediate apply using whatever value the server has cached for the player.
        applyFor(event.getPlayer(), false);

        // Schedule delayed re-applies to catch the late client packet.
        plugin.getSchedulerHook().runEntityTaskAsap(() -> applyFor(event.getPlayer(), true), null, event.getPlayer());
        List<Integer> retries = plugin.getSeeMoreNextConfig().getOptionsChangeRetries();
        for (int delay : retries) {
            plugin.getSchedulerHook().runTaskDelayed(() -> applyFor(event.getPlayer(), true), delay);
        }

        // Log join if log level is "all"
        if ("all".equals(plugin.getSeeMoreNextConfig().getLogLevel())) {
            plugin.getLogger().info(String.format("Player %s joined — applying view distance.", event.getPlayer().getName()));
        }
    }

    @EventHandler
    private void onOptionsChange(PlayerClientOptionsChangeEvent event) {
        boolean seen = seenBefore.contains(event.getPlayer().getUniqueId());
        applyFor(event.getPlayer(), seen);

        // Schedule delayed re-applies because Paper's internal client view
        // distance cache can lag behind the event.
        UUID uuid = event.getPlayer().getUniqueId();
        if (Boolean.TRUE.equals(pendingRetries.get(uuid))) return; // already have retries scheduled

        pendingRetries.put(uuid, true);
        List<Integer> retries = plugin.getSeeMoreNextConfig().getOptionsChangeRetries();
        for (int delay : retries) {
            plugin.getSchedulerHook().runTaskDelayed(() -> {
                if (Bukkit.getPlayer(uuid) != null) {
                    applyFor(Bukkit.getPlayer(uuid), true);
                }
            }, delay);
        }
        // Clear the flag after the last retry
        if (!retries.isEmpty()) {
            int lastDelay = retries.get(retries.size() - 1) + 20;
            plugin.getSchedulerHook().runTaskDelayed(() -> pendingRetries.remove(uuid), lastDelay);
        }
    }

    @EventHandler
    private void onWorldChange(PlayerChangedWorldEvent event) {
        applyFor(event.getPlayer(), false);

        if ("all".equals(plugin.getSeeMoreNextConfig().getLogLevel())) {
            plugin.getLogger().info(String.format("Player %s changed world to %s — re-applying view distance.",
                    event.getPlayer().getName(), event.getPlayer().getWorld().getName()));
        }
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        seenBefore.remove(event.getPlayer().getUniqueId());
        lastAppliedClientDistance.remove(event.getPlayer().getUniqueId());
        pendingRetries.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Apply the player's current client view distance to their server-side
     * view distance, with deduplication so we don't spam updates for the same
     * value.
     */
    private void applyFor(Player player, boolean testDelay) {
        if (player == null || !player.isOnline()) return;

        UUID id = player.getUniqueId();
        int clientViewDistance = safeGetClientViewDistance(player);
        if (clientViewDistance <= 0) {
            return;
        }
        Integer previous = lastAppliedClientDistance.get(id);
        if (previous != null && previous == clientViewDistance) {
            return;
        }
        boolean firstTime = !seenBefore.contains(id);
        lastAppliedClientDistance.put(id, clientViewDistance);
        seenBefore.add(id);
        controller.setTargetViewDistance(player, clientViewDistance, testDelay, firstTime);
    }

    private int safeGetClientViewDistance(Player player) {
        try {
            return player.getClientViewDistance();
        } catch (Throwable t) {
            return 0;
        }
    }

    /**
     * Apply to every online player right now.
     * Used by {@code /smn reload} via the controller.
     */
    void refreshAllOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyFor(player, false);
        }
    }
}
