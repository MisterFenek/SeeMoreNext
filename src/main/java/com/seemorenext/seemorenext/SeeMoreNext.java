package com.seemorenext.seemorenext;

import com.seemorenext.seemorenext.command.SeeMoreNextCommand;
import com.seemorenext.seemorenext.config.SeeMoreNextConfig;
import com.seemorenext.seemorenext.controller.ViewDistanceController;
import com.seemorenext.seemorenext.scheduler.BukkitSchedulerHook;
import com.seemorenext.seemorenext.scheduler.RegionisedSchedulerHook;
import com.seemorenext.seemorenext.scheduler.SchedulerHook;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class SeeMoreNext extends JavaPlugin {
    private SeeMoreNextConfig config;
    private SchedulerHook schedulerHook;
    private ViewDistanceController viewDistanceController;

    @Override
    public void onEnable() {
        config = new SeeMoreNextConfig(this);
        try {
            config.load();
        } catch (Exception e) {
            getLogger().severe("Error loading config");
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        if (RegionisedSchedulerHook.isCompatible()) {
            schedulerHook = new RegionisedSchedulerHook(this);
        } else {
            schedulerHook = new BukkitSchedulerHook(this);
        }

        viewDistanceController = new ViewDistanceController(this);

        registerCommand();
        getLogger().info("SeeMoreNext v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        // No background metrics to flush.
    }

    private void registerCommand() {
        PluginCommand pluginCommand = getCommand("seemorenext");
        if (pluginCommand != null) {
            SeeMoreNextCommand command = new SeeMoreNextCommand(this);
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
            pluginCommand.setPermission("seemorenext.command.seemore");
        }
    }

    public void reload() throws Exception {
        config.load();
        if (viewDistanceController != null) {
            viewDistanceController.startPolling(); // restart polling with new interval
            viewDistanceController.updateAllPlayers();
        }
    }

    public SeeMoreNextConfig getSeeMoreNextConfig() {
        return config;
    }

    public SchedulerHook getSchedulerHook() {
        return schedulerHook;
    }

    public ViewDistanceController getViewDistanceController() {
        return viewDistanceController;
    }
}
