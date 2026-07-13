package com.seemorenext.seemorenext.config;

import com.seemorenext.lib.nabconfiguration.*;
import com.seemorenext.lib.nabconfiguration.annotations.Entry;
import com.seemorenext.lib.nabconfiguration.annotations.SectionMap;
import com.seemorenext.seemorenext.SeeMoreNext;
import org.bukkit.World;

import java.io.File;

public class SeeMoreNextConfig extends NabConfiguration {
    private static final int VERSION = 1;

    public SeeMoreNextConfig(SeeMoreNext plugin) {
        super(
                new File(plugin.getDataFolder(), "config.yml"),
                () -> plugin.getResource("config.yml"),
                i -> plugin.getResource("config-patches/" + i + ".patch"),
                VERSION
        );
    }

    @Entry(key = "update-delay")
    public final ConfigEntry<Integer> updateDelay = ConfigEntries.integerEntry();

    @Entry(key = "log-changes")
    public final ConfigEntry<Boolean> logChanges = new ConfigEntry<>();

    @SectionMap(key = "world-settings", defaultKey = "default")
    public final ConfigSectionMap<World, WorldSettings> worldSettings = new ConfigSectionMap<>(World::getName, WorldSettings.class, true);

    public static class WorldSettings extends ConfigSection {

        @Entry(key = "maximum-view-distance")
        public final ConfigEntry<Integer> maximumViewDistance = ConfigEntries.integerEntry();

    }

}
