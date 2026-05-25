package fr.minuskube.inv;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.function.Consumer;

import static fr.minuskube.inv.util.Misc.debugMsg;

public class FoliaSchedulerManager implements SchedulerManager {
    private final JavaPlugin plugin;
    private final SchedulerTaskRegistry<ScheduledTask> tasks = new SchedulerTaskRegistry<>(ScheduledTask::cancel);

    public FoliaSchedulerManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runTask(BukkitRunnable task, Player player, long delay, long period, SchedulerType type) {
        runTask(task, player, delay, period, type, TaskType.UPDATE);
    }

    @Override
    public void runTask(BukkitRunnable task, Player player, long delay, long period, SchedulerType type, TaskType taskType) {
        // Convert BukkitRunnable to Folia task
        debugMsg("Using FoliaSchedulerManager, converting bukkitrun to task...", plugin);
        Consumer<ScheduledTask> consumerTask = (scheduledTask) -> task.run();

        // Begin type check
        switch (type) {
            case ENTITY:
                debugMsg("Entity scheduler requested", plugin);
                ScheduledTask scheduledTask;
                if (delay == 0 && period == 0) {
                    debugMsg("run()", plugin);
                    scheduledTask = player.getScheduler().run(plugin, consumerTask, null);
                } else if (delay != 0 && period == 0) {
                    debugMsg("runDelayed() <-- delay", plugin);
                    scheduledTask = player.getScheduler().runDelayed(plugin, consumerTask, null, delay);
                } else {
                    debugMsg("runAtFixedRate() <-- delay + repeat", plugin);
                    scheduledTask = player.getScheduler().runAtFixedRate(plugin, consumerTask, null, delay, period);
                }
                if (scheduledTask != null) {
                    tasks.replace(player, taskType, scheduledTask);
                    debugMsg("Added scheduledTask to list. Player task count: " + tasks.taskCount(player), plugin);
                } else {
                    debugMsg("scheduledTask was null? Not adding to list! Player task count: " + tasks.taskCount(player), plugin);
                }
                break;
            case GLOBAL:
            case REGION:
            case ASYNC:
            default:
                plugin.getLogger().warning("Scheduler type " + type + " is not implemented yet.");
                break;
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
