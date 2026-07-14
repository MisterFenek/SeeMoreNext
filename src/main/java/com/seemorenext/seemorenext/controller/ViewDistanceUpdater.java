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

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for client view-distance changes and pushes them to the controller.
 *
 * <p>Implementation note: Paper's {@link PlayerClientOptionsChangeEvent} is
 * unreliable on the very first packet a client sends (it sometimes fires only
 * after the player has already moved a few chunks, or doesn't fire at all on
 * the initial spawn). We therefore additionally handle:</p>
 * <ul>
 *   <li>{@link PlayerJoinEvent} — apply immediately and again after a short
 *       delay (20 ticks) to catch the late client packet.</li>
 *   <li>{@link PlayerClientOptionsChangeEvent} — re-apply on every reported
 *       change, with deduplication against the last known client distance.</li>
 *   <li>{@link PlayerChangedWorldEvent} — re-apply when the player switches
 *       worlds (config max may differ).</li>
 * </ul>
 */
public class ViewDistanceUpdater implements Listener {
    private final SeeMoreNext plugin;
    private final ViewDistanceController controller;
    private final Set<UUID> seenBefore = ConcurrentHashMap.newKeySet();
    private final java.util.Map<UUID, Integer> lastAppliedClientDistance = new ConcurrentHashMap<>();

    public ViewDistanceUpdater(SeeMoreNext plugin, ViewDistanceController viewDistanceController) {
        this.plugin = plugin;
        this.controller = viewDistanceController;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onJoin(PlayerJoinEvent event) {
        // Immediate apply using whatever value the server has cached for the player.
        applyFor(event.getPlayer(), false);

        // Schedule a delayed apply to catch the case where the first client
        // options packet arrives after the join event.
        plugin.getSchedulerHook().runEntityTaskAsap(() -> applyFor(event.getPlayer(), true), null, event.getPlayer());
        plugin.getSchedulerHook().runTaskDelayed(() -> applyFor(event.getPlayer(), true), 20);
        plugin.getSchedulerHook().runTaskDelayed(() -> applyFor(event.getPlayer(), true), 60);
    }

    @EventHandler
    private void onOptionsChange(PlayerClientOptionsChangeEvent event) {
        // The "changed" check may not detect the very first client packet, so
        // we additionally fall back to "have we ever seen this player before".
        boolean seen = seenBefore.contains(event.getPlayer().getUniqueId());
        Player player = event.getPlayer();
        applyFor(player, seen);

        // Schedule delayed re-applies because Paper's internal client view
        // distance cache can lag behind the event (the value returned by
        // getClientViewDistance() may still be stale at event time).
        plugin.getSchedulerHook().runTaskDelayed(() -> applyFor(player, true), 20);
        plugin.getSchedulerHook().runTaskDelayed(() -> applyFor(player, true), 60);
    }

    @EventHandler
    private void onWorldChange(PlayerChangedWorldEvent event) {
        // The world switch resets Paper's per-world cached client view distance
        // in some cases; force an apply without the update-delay.
        applyFor(event.getPlayer(), false);
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        seenBefore.remove(event.getPlayer().getUniqueId());
        lastAppliedClientDistance.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Apply the player's current client view distance to their server-side
     * view distance, with deduplication so we don't spam updates for the same
     * value.
     *
     * @param player       target player
     * @param testDelay    whether the update-delay should apply (true on the
     *                     first event for a player, false on subsequent ones
     *                     and on world change / join)
     */
    private void applyFor(Player player, boolean testDelay) {
        UUID id = player.getUniqueId();
        int clientViewDistance = safeGetClientViewDistance(player);
        if (clientViewDistance <= 0) {
            // Paper hasn't received the first client options packet yet; skip.
            return;
        }
        Integer previous = lastAppliedClientDistance.get(id);
        if (previous != null && previous == clientViewDistance) {
            // No change since the last apply; ignore.
            return;
        }
        boolean firstTime = !seenBefore.contains(id);
        lastAppliedClientDistance.put(id, clientViewDistance);
        seenBefore.add(id);
        controller.setTargetViewDistance(player, clientViewDistance, testDelay, firstTime);
    }

    /**
     * Safely read the client's view distance. On rare occasions (during world
     * change, teleport) Paper may throw or return 0; we treat both as "no
     * information available yet" and skip the update.
     */
    private int safeGetClientViewDistance(Player player) {
        try {
            int v = player.getClientViewDistance();
            return v;
        } catch (Throwable t) {
            return 0;
        }
    }

    /**
     * Test-only entry point: apply to every online player right now.
     * Used by {@code /seemorenext reload} via the controller.
     */
    void refreshAllOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyFor(player, false);
        }
    }
}
