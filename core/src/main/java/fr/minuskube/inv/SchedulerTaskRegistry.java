package fr.minuskube.inv;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

final class SchedulerTaskRegistry<T> {
    private final Map<Player, Map<SchedulerManager.TaskType, T>> tasks = new ConcurrentHashMap<>();
    private final Consumer<T> canceller;

    SchedulerTaskRegistry(Consumer<T> canceller) {
        this.canceller = Objects.requireNonNull(canceller, "canceller");
    }

    void replace(Player player, SchedulerManager.TaskType taskType, T task) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(taskType, "taskType");

        if (task == null) {
            return;
        }

        Map<SchedulerManager.TaskType, T> playerTasks = tasks.computeIfAbsent(player, ignored -> new ConcurrentHashMap<>());
        T previous = playerTasks.put(taskType, task);
        if (previous != null && previous != task) {
            canceller.accept(previous);
        }
    }

    boolean cancel(Player player, SchedulerManager.TaskType taskType) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(taskType, "taskType");

        Map<SchedulerManager.TaskType, T> playerTasks = tasks.get(player);
        if (playerTasks == null) {
            return false;
        }

        T removed = playerTasks.remove(taskType);
        if (playerTasks.isEmpty()) {
            tasks.remove(player, playerTasks);
        }

        if (removed == null) {
            return false;
        }

        canceller.accept(removed);
        return true;
    }

    int cancelAll(Player player) {
        Objects.requireNonNull(player, "player");

        Map<SchedulerManager.TaskType, T> playerTasks = tasks.remove(player);
        if (playerTasks == null) {
            return 0;
        }

        int cancelled = 0;
        for (T task : playerTasks.values()) {
            if (task != null) {
                canceller.accept(task);
                cancelled++;
            }
        }
        return cancelled;
    }

    int taskCount(Player player) {
        Map<SchedulerManager.TaskType, T> playerTasks = tasks.get(player);
        return playerTasks == null ? 0 : playerTasks.size();
    }
}
