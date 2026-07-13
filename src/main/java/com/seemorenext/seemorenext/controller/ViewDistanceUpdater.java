package com.seemorenext.seemorenext.controller;

import com.destroystokyo.paper.event.player.PlayerClientOptionsChangeEvent;
import com.google.common.collect.Sets;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;

/**
 * Listens for client view-distance changes and pushes them to the controller.
 * <p>
 * We use {@code event.getPlayer().getClientViewDistance()} rather than the
 * deprecated {@code event.getViewDistance()} accessor so the plugin compiles
 * cleanly against Paper 26.2 (where the deprecated accessor still works but
 * emits warnings, and may be removed in a future release).
 */
public class ViewDistanceUpdater implements Listener {
    private final ViewDistanceController controller;
    private final Set<UUID> seenBefore = Sets.newConcurrentHashSet();

    public ViewDistanceUpdater(ViewDistanceController viewDistanceController) {
        this.controller = viewDistanceController;
    }

    @EventHandler
    private void onOptionsChange(PlayerClientOptionsChangeEvent event) {
        // The "changed" check may not detect the very first client packet, so
        // we additionally fall back to "have we ever seen this player before".
        boolean seen = seenBefore.contains(event.getPlayer().getUniqueId());
        int clientViewDistance = event.getPlayer().getClientViewDistance();

        if (event.hasViewDistanceChanged() || !seen) {
            seenBefore.add(event.getPlayer().getUniqueId());
            controller.setTargetViewDistance(event.getPlayer(), clientViewDistance, seen, !seen);
        }
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        seenBefore.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    private void onWorldChange(PlayerChangedWorldEvent event) {
        controller.setTargetViewDistance(event.getPlayer(), event.getPlayer().getClientViewDistance(), false, false);
    }

}
