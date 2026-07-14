package com.seemorenext.seemorenext.config;

import com.seemorenext.lib.nabconfiguration.*;
import com.seemorenext.lib.nabconfiguration.annotations.Entry;
import com.seemorenext.lib.nabconfiguration.annotations.SectionMap;
import com.seemorenext.seemorenext.SeeMoreNext;
import org.bukkit.World;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SeeMoreNextConfig extends NabConfiguration {
    private static final int VERSION = 2;

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
    public final ConfigEntry<String> logChanges = new ConfigEntry<>();

    @Entry(key = "wait-for-client-settings")
    public final ConfigEntry<Boolean> waitForClientSettings = new ConfigEntry<>();

    @Entry(key = "polling-interval")
    public final ConfigEntry<Integer> pollingInterval = ConfigEntries.integerEntry();

    @Entry(key = "options-change-retries")
    public final ConfigEntry<String> optionsChangeRetries = new ConfigEntry<>();

    @SectionMap(key = "world-settings", defaultKey = "default")
    public final ConfigSectionMap<World, WorldSettings> worldSettings = new ConfigSectionMap<>(World::getName, WorldSettings.class, true);

    /**
     * Get the configured log level as a normalized lowercase string.
     * Handles legacy boolean configs ("true" → "minimal", "false" → "off").
     */
    public String getLogLevel() {
        Object raw = logChanges.get();
        if (raw == null) return "minimal";
        String val = raw.toString().trim().toLowerCase();
        if (val.equals("true") || val.equals("1") || val.equals("yes")) return "minimal";
        if (val.equals("false") || val.equals("0") || val.equals("no")) return "off";
        if (val.equals("off") || val.equals("minimal") || val.equals("all")) return val;
        return "minimal";
    }

    /**
     * Get the list of tick delays for options-change retries.
     * Parses "[20, 60]" style string from config.
     */
    public List<Integer> getOptionsChangeRetries() {
        String raw = optionsChangeRetries.get();
        if (raw == null || raw.isEmpty()) return Arrays.asList(20, 60);
        try {
            return Arrays.stream(raw.replace("[", "").replace("]", "").split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            return Arrays.asList(20, 60);
        }
    }

    public static class WorldSettings extends ConfigSection {

        @Entry(key = "maximum-view-distance")
        public final ConfigEntry<Integer> maximumViewDistance = ConfigEntries.integerEntry();

        @Entry(key = "minimum-view-distance")
        public final ConfigEntry<Integer> minimumViewDistance = ConfigEntries.integerEntry();

    }

}
