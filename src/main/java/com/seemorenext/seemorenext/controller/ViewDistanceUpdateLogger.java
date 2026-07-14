package com.seemorenext.seemorenext.controller;

import com.seemorenext.seemorenext.SeeMoreNext;
import com.seemorenext.seemorenext.scheduler.ScheduledTask;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ViewDistanceUpdateLogger {
    private final SeeMoreNext plugin;
    private final Map<UUID, ScheduledTask> updateMessageTasks;

    public ViewDistanceUpdateLogger(SeeMoreNext plugin) {
        this.plugin = plugin;
        this.updateMessageTasks = new ConcurrentHashMap<>();
    }

    public void logUpdate(Player player, String logMessage) {
        String level = plugin.getSeeMoreNextConfig().getLogLevel();
        if ("off".equals(level)) return;

        updateMessageTasks.compute(player.getUniqueId(), (uuid, oldTask) -> {
            if (oldTask != null) {
                oldTask.cancel();
            }
            return plugin.getSchedulerHook().runTaskDelayed(() -> {
                plugin.getLogger().info(logMessage);
                updateMessageTasks.remove(uuid);
            }, 20); // delay by 20 ticks to avoid spam from vanilla clients using the view distance slider
        });
    }

}
