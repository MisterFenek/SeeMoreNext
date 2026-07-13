package com.seemorenext.seemorenext.scheduler;

import com.seemorenext.seemorenext.SeeMoreNext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Regionised (Folia) scheduler hook.
 *
 * <p>The Folia scheduler API ({@code io.papermc.paper.threadedregions.*}) was
 * introduced in Paper 1.20.4. To keep a single universal JAR that also runs on
 * Paper 1.19.4, every Folia-specific call is performed via reflection.
 * At runtime we check {@link #isCompatible()} and fall back to
 * {@link BukkitSchedulerHook} on older servers.</p>
 */
public class RegionisedSchedulerHook implements SchedulerHook {
    private final SeeMoreNext plugin;

    // Cached reflective handles. Populated lazily by the FoliaHooks helper
    // so that we don't blow up the static initializer on servers without
    // the Folia API.
    private final FoliaHooks hooks;

    public RegionisedSchedulerHook(SeeMoreNext plugin) {
        this.plugin = plugin;
        this.hooks = FoliaHooks.LOADED;
        if (this.hooks == null) {
            // Should never happen — SeeMoreNext only constructs this class
            // after isCompatible() returns true.
            throw new IllegalStateException("Folia hooks not loaded");
        }
    }

    private Object globalScheduler() {
        return hooks.invoke(hooks.bukkitGetGlobalRegionScheduler, null);
    }

    @Override
    public ScheduledTask runTask(Runnable runnable) {
        Consumer<Object> consumer = task -> runnable.run();
        Object scheduled = hooks.invoke(hooks.globalRegionRun, globalScheduler(), plugin, consumer);
        return new RegionisedScheduledTask(scheduled);
    }

    @Override
    public ScheduledTask runTaskDelayed(Runnable runnable, long delay) {
        Consumer<Object> consumer = task -> runnable.run();
        Object scheduled = hooks.invoke(hooks.globalRegionRunDelayed, globalScheduler(), plugin, consumer, delay);
        return new RegionisedScheduledTask(scheduled);
    }

    @Override
    public ScheduledTask runRepeatingTask(Runnable runnable, long initDelay, long period) {
        Consumer<Object> consumer = task -> runnable.run();
        Object scheduled = hooks.invoke(hooks.globalRegionRunAtFixedRate, globalScheduler(), plugin, consumer, initDelay, period);
        return new RegionisedScheduledTask(scheduled);
    }

    @Override
    public ScheduledTask runEntityTask(Runnable runnable, Runnable retired, Entity entity) {
        Object scheduler = hooks.invoke(hooks.entityGetScheduler, entity);
        Consumer<Object> consumer = task -> runnable.run();
        Object scheduled = hooks.invoke(hooks.entitySchedulerRun, scheduler, plugin, consumer, retired);
        if (scheduled == null) {
            return null;
        }
        return new RegionisedScheduledTask(scheduled);
    }

    @Override
    public ScheduledTask runEntityTaskAsap(Runnable runnable, Runnable retired, Entity entity) {
        Object owned = hooks.invoke(hooks.bukkitIsOwnedByCurrentRegion, null, entity);
        if (Boolean.TRUE.equals(owned)) {
            runnable.run();
            return new ScheduledTask() {
                @Override
                public void cancel() {}

                @Override
                public boolean isCancelled() {
                    return false;
                }
            };
        }
        return runEntityTask(runnable, retired, entity);
    }

    public static boolean isCompatible() {
        return FoliaHooks.LOADED != null;
    }

    private static class RegionisedScheduledTask implements ScheduledTask {
        private final Object scheduledTask;
        private final FoliaHooks hooks = FoliaHooks.LOADED;

        private RegionisedScheduledTask(Object scheduledTask) {
            this.scheduledTask = scheduledTask;
        }

        @Override
        public void cancel() {
            hooks.invoke(hooks.scheduledTaskCancel, scheduledTask);
        }

        @Override
        public boolean isCancelled() {
            return (boolean) hooks.invoke(hooks.scheduledTaskIsCancelled, scheduledTask);
        }
    }

    /**
     * Reflective handles to the Folia scheduler API. Loaded lazily on first
     * class access; if the API is not present, {@link #LOADED} stays null.
     */
    private static final class FoliaHooks {
        static final FoliaHooks LOADED = tryLoad();

        final Method bukkitGetGlobalRegionScheduler;
        final Method bukkitIsOwnedByCurrentRegion;
        final Method globalRegionRun;
        final Method globalRegionRunDelayed;
        final Method globalRegionRunAtFixedRate;
        final Method entityGetScheduler;
        final Method entitySchedulerRun;
        final Method scheduledTaskCancel;
        final Method scheduledTaskIsCancelled;

        private FoliaHooks() {
            this.bukkitGetGlobalRegionScheduler = lookup(Bukkit.class, "getGlobalRegionScheduler");
            this.bukkitIsOwnedByCurrentRegion = lookup(Bukkit.class, "isOwnedByCurrentRegion", Entity.class);
            this.globalRegionRun = lookupByName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler", "run", org.bukkit.plugin.Plugin.class, Consumer.class);
            this.globalRegionRunDelayed = lookupByName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler", "runDelayed", org.bukkit.plugin.Plugin.class, Consumer.class, long.class);
            this.globalRegionRunAtFixedRate = lookupByName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler", "runAtFixedRate", org.bukkit.plugin.Plugin.class, Consumer.class, long.class, long.class);
            this.entityGetScheduler = lookup(Entity.class, "getScheduler");
            this.entitySchedulerRun = lookupByName("io.papermc.paper.threadedregions.scheduler.EntityScheduler", "run", org.bukkit.plugin.Plugin.class, Consumer.class, Runnable.class);
            this.scheduledTaskCancel = lookupByName("io.papermc.paper.threadedregions.scheduler.ScheduledTask", "cancel");
            this.scheduledTaskIsCancelled = lookupByName("io.papermc.paper.threadedregions.scheduler.ScheduledTask", "isCancelled");
        }

        private static FoliaHooks tryLoad() {
            try {
                Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
                return new FoliaHooks();
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        private static Method lookup(Class<?> owner, String name, Class<?>... params) {
            try {
                return owner.getMethod(name, params);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Method not found: " + owner.getName() + "#" + name, e);
            }
        }

        private static Method lookupByName(String className, String name, Class<?>... params) {
            try {
                return Class.forName(className).getMethod(name, params);
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                throw new IllegalStateException("Method not found: " + className + "#" + name, e);
            }
        }

        Object invoke(Method method, Object target, Object... args) {
            try {
                return method.invoke(target, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to invoke " + method, e);
            }
        }
    }
}
