package fr.minuskube.inv;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public interface SchedulerManager {

    enum SchedulerType {
        GLOBAL,
        ENTITY,
        REGION,
        ASYNC
    }

    enum TaskType {
        UPDATE,
        REOPEN
    }

    // If we ever generalize these methods, replace player with Object
    void runTask(BukkitRunnable task, Player player, long delay, long period, SchedulerType type);

    default void runTask(BukkitRunnable task, Player player, long delay, long period, SchedulerType type, TaskType taskType) {
        runTask(task, player, delay, period, type);
    }

    void cancelTaskByPlayer(Player player);

    default void cancelTaskByPlayer(Player player, TaskType taskType) {
        cancelTaskByPlayer(player);
    }

    default void cancelAllTasksByPlayer(Player player) {
        cancelTaskByPlayer(player);
    }

}
