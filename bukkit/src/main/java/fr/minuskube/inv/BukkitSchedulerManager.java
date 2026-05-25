package fr.minuskube.inv;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;

import static fr.minuskube.inv.util.Misc.debugMsg;

public class BukkitSchedulerManager implements SchedulerManager {
    private final JavaPlugin plugin;
    private final SchedulerTaskRegistry<BukkitTask> tasks = new SchedulerTaskRegistry<>(BukkitTask::cancel);

    public BukkitSchedulerManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runTask(BukkitRunnable task, Player player, long delay, long period, SchedulerType type) {
        runTask(task, player, delay, period, type, TaskType.UPDATE);
    }

    @Override
    public void runTask(BukkitRunnable task, Player player, long delay, long period, SchedulerType type, TaskType taskType) {
        BukkitTask scheduledTask;
        debugMsg("Using BukkitSchedulerManager to run a task! Type: " + type.name() + " | Task: " + taskType.name() + " | Delay: " + delay + " | Period: " + period, plugin);

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
            tasks.replace(player, taskType, scheduledTask);
            debugMsg("Added scheduledTask to list. Player task count: " + tasks.taskCount(player), plugin);
        } else {
            debugMsg("scheduledTask was null? Not adding to list! Player task count: " + tasks.taskCount(player), plugin);
        }
    }

    @Override
    public void cancelTaskByPlayer(Player player) {
        cancelTaskByPlayer(player, TaskType.UPDATE);
    }

    @Override
    public void cancelTaskByPlayer(Player player, TaskType taskType) {
        if (tasks.cancel(player, taskType)) {
            debugMsg("Task cancelled :) Player task count: " + tasks.taskCount(player), plugin);
        } else {
            debugMsg("Unable to cancel task, list remove() returned null. Player task count: " + tasks.taskCount(player), plugin);
        }
    }

    @Override
    public void cancelAllTasksByPlayer(Player player) {
        int cancelled = tasks.cancelAll(player);
        debugMsg("Cancelled " + cancelled + " task(s) for player. Player task count: " + tasks.taskCount(player), plugin);
    }
}
