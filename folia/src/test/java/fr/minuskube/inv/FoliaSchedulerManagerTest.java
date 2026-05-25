package fr.minuskube.inv;

import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class FoliaSchedulerManagerTest {

    @Test
    public void entityDelayedAndRepeatingCallsPassTickValuesThrough() {
        RecordingEntityScheduler entityScheduler = new RecordingEntityScheduler();
        Player player = player(entityScheduler);
        FoliaSchedulerManager manager = new FoliaSchedulerManager(null);

        manager.runTask(noopTask(), player, 7, 0, SchedulerManager.SchedulerType.ENTITY, SchedulerManager.TaskType.UPDATE);
        manager.runTask(noopTask(), player, 2, 5, SchedulerManager.SchedulerType.ENTITY, SchedulerManager.TaskType.UPDATE);

        assertEquals("delayed", entityScheduler.calls.get(0).method);
        assertEquals(7, entityScheduler.calls.get(0).delay);
        assertEquals(0, entityScheduler.calls.get(0).period);
        assertEquals("fixed", entityScheduler.calls.get(1).method);
        assertEquals(2, entityScheduler.calls.get(1).delay);
        assertEquals(5, entityScheduler.calls.get(1).period);
    }

    @Test
    public void updateAndReopenTasksAreIndependentlyCancellable() {
        RecordingEntityScheduler entityScheduler = new RecordingEntityScheduler();
        Player player = player(entityScheduler);
        FoliaSchedulerManager manager = new FoliaSchedulerManager(null);

        manager.runTask(noopTask(), player, 1, 4, SchedulerManager.SchedulerType.ENTITY, SchedulerManager.TaskType.UPDATE);
        manager.runTask(noopTask(), player, 0, 0, SchedulerManager.SchedulerType.ENTITY, SchedulerManager.TaskType.REOPEN);
        FakeScheduledTask update = entityScheduler.calls.get(0).task;
        FakeScheduledTask reopen = entityScheduler.calls.get(1).task;

        manager.cancelTaskByPlayer(player, SchedulerManager.TaskType.UPDATE);

        assertTrue(update.cancelled);
        assertFalse(reopen.cancelled);

        manager.cancelTaskByPlayer(player, SchedulerManager.TaskType.REOPEN);

        assertTrue(reopen.cancelled);
    }

    @Test
    public void cancelAllTasksByPlayerCancelsAllFoliaTasksForPlayer() {
        RecordingEntityScheduler entityScheduler = new RecordingEntityScheduler();
        Player player = player(entityScheduler);
        FoliaSchedulerManager manager = new FoliaSchedulerManager(null);

        manager.runTask(noopTask(), player, 1, 4, SchedulerManager.SchedulerType.ENTITY, SchedulerManager.TaskType.UPDATE);
        manager.runTask(noopTask(), player, 0, 0, SchedulerManager.SchedulerType.ENTITY, SchedulerManager.TaskType.REOPEN);
        FakeScheduledTask update = entityScheduler.calls.get(0).task;
        FakeScheduledTask reopen = entityScheduler.calls.get(1).task;

        manager.cancelAllTasksByPlayer(player);

        assertTrue(update.cancelled);
        assertTrue(reopen.cancelled);
    }

    private static BukkitRunnable noopTask() {
        return new BukkitRunnable() {
            @Override
            public void run() {
            }
        };
    }

    private static Player player(EntityScheduler scheduler) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("getScheduler".equals(method.getName())) {
                        return scheduler;
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("toString".equals(method.getName())) {
                        return "PlayerProxy";
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) return null;
        if (returnType == boolean.class) return false;
        if (returnType == byte.class) return (byte) 0;
        if (returnType == short.class) return (short) 0;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        if (returnType == float.class) return 0f;
        if (returnType == double.class) return 0d;
        if (returnType == char.class) return '\0';
        return null;
    }

    private static final class RecordingEntityScheduler implements EntityScheduler {
        private final List<Call> calls = new ArrayList<>();

        @Override
        public boolean execute(Plugin plugin, Runnable run, Runnable retired, long delay) {
            throw new UnsupportedOperationException("Not used by SmartInvs");
        }

        @Override
        public ScheduledTask run(Plugin plugin, Consumer<ScheduledTask> task, Runnable retired) {
            return record("run", 0, 0);
        }

        @Override
        public ScheduledTask runDelayed(Plugin plugin, Consumer<ScheduledTask> task, Runnable retired, long delay) {
            return record("delayed", delay, 0);
        }

        @Override
        public ScheduledTask runAtFixedRate(Plugin plugin, Consumer<ScheduledTask> task, Runnable retired, long delay, long period) {
            return record("fixed", delay, period);
        }

        private ScheduledTask record(String method, long delay, long period) {
            FakeScheduledTask task = new FakeScheduledTask();
            calls.add(new Call(method, delay, period, task));
            return task;
        }
    }

    private static final class Call {
        private final String method;
        private final long delay;
        private final long period;
        private final FakeScheduledTask task;

        private Call(String method, long delay, long period, FakeScheduledTask task) {
            this.method = method;
            this.delay = delay;
            this.period = period;
            this.task = task;
        }
    }

    private static final class FakeScheduledTask implements ScheduledTask {
        private boolean cancelled;

        @Override
        public Plugin getOwningPlugin() {
            return null;
        }

        @Override
        public boolean isRepeatingTask() {
            return false;
        }

        @Override
        public CancelledState cancel() {
            cancelled = true;
            return CancelledState.CANCELLED_BY_CALLER;
        }

        @Override
        public ExecutionState getExecutionState() {
            return cancelled ? ExecutionState.CANCELLED : ExecutionState.IDLE;
        }
    }
}
