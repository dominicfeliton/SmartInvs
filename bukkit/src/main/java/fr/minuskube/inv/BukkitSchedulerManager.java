package fr.minuskube.inv;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static fr.minuskube.inv.util.Misc.debugMsg;

public class BukkitSchedulerManager implements SchedulerManager {
    private final JavaPlugin plugin;
    private final Map<Player, BukkitTask> tasks = new ConcurrentHashMap<>();

    public BukkitSchedulerManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runTask(BukkitRunnable task, Player player, long delay, long period, SchedulerType type) {
        BukkitTask scheduledTask;
        debugMsg("Using BukkitSchedulerManager to run a task! Type: " + type.name() + " | Delay: " + delay + " | Period: " + period, plugin);

        if (delay == 0 && period == 0) {
            debugMsg("runTask()", plugin);
            scheduledTask = task.runTask(plugin);
        } else if (delay != 0 && period == 0) {
            debugMsg("runTaskLater() <-- delay", plugin);
            scheduledTask = task.runTaskLater(plugin, delay);
        } else {
            debugMsg("runTaskTimer() <-- delay & repeat", plugin);
            scheduledTask = task.runTaskTimer(plugin, delay, period);
        }

        if (scheduledTask != null) {
            debugMsg("Added scheduledTask to list. List size: " + tasks.size(), plugin);
            tasks.put(player, scheduledTask);
        } else {
            debugMsg("scheduledTask was null? Not adding to list! List size: " + tasks.size(), plugin);
        }
    }

    @Override
    public void cancelTaskByPlayer(Player player) {
        BukkitTask task = tasks.remove(player);
        if (task != null) {
            debugMsg("Task cancelled :) List size: " + tasks.size(), plugin);
            task.cancel();
        } else {
            debugMsg("Unable to cancel task, list remove() returned null. List size: " + tasks.size(), plugin);
        }
    }
}
