package com.seemorenext.seemorenext.command;

import com.seemorenext.seemorenext.SeeMoreNext;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static net.kyori.adventure.text.Component.*;

public class ReloadCommand implements CommandExecutor, TabCompleter {
    private final SeeMoreNext plugin;

    public ReloadCommand(SeeMoreNext plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        try {
            plugin.reload();
            sender.sendMessage(text("SeeMoreNext reloaded.", NamedTextColor.GRAY));
        } catch (Exception e) {
            sender.sendMessage(text("There was an error while reloading SeeMoreNext. See console for details.", NamedTextColor.RED));
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
