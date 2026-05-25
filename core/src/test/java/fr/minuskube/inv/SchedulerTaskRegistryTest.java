package fr.minuskube.inv;

import org.bukkit.entity.Player;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.*;

public class SchedulerTaskRegistryTest {

    @Test
    public void samePlayerAndTypeReplacementCancelsPreviousTask() {
        SchedulerTaskRegistry<FakeTask> registry = new SchedulerTaskRegistry<>(FakeTask::cancel);
        Player player = player();
        FakeTask first = new FakeTask();
        FakeTask second = new FakeTask();

        registry.replace(player, SchedulerManager.TaskType.UPDATE, first);
        registry.replace(player, SchedulerManager.TaskType.UPDATE, second);

        assertTrue(first.cancelled);
        assertFalse(second.cancelled);
        assertEquals(1, registry.taskCount(player));
    }

    @Test
    public void differentTaskTypesForSamePlayerCoexist() {
        SchedulerTaskRegistry<FakeTask> registry = new SchedulerTaskRegistry<>(FakeTask::cancel);
        Player player = player();
        FakeTask update = new FakeTask();
        FakeTask reopen = new FakeTask();

        registry.replace(player, SchedulerManager.TaskType.UPDATE, update);
        registry.replace(player, SchedulerManager.TaskType.REOPEN, reopen);

        assertFalse(update.cancelled);
        assertFalse(reopen.cancelled);
        assertEquals(2, registry.taskCount(player));
    }

    @Test
    public void cancellingUpdateLeavesReopenRunning() {
        SchedulerTaskRegistry<FakeTask> registry = new SchedulerTaskRegistry<>(FakeTask::cancel);
        Player player = player();
        FakeTask update = new FakeTask();
        FakeTask reopen = new FakeTask();

        registry.replace(player, SchedulerManager.TaskType.UPDATE, update);
        registry.replace(player, SchedulerManager.TaskType.REOPEN, reopen);

        assertTrue(registry.cancel(player, SchedulerManager.TaskType.UPDATE));

        assertTrue(update.cancelled);
        assertFalse(reopen.cancelled);
        assertEquals(1, registry.taskCount(player));
    }

    @Test
    public void cancellingAllTasksCancelsEveryTaskForPlayer() {
        SchedulerTaskRegistry<FakeTask> registry = new SchedulerTaskRegistry<>(FakeTask::cancel);
        Player player = player();
        FakeTask update = new FakeTask();
        FakeTask reopen = new FakeTask();

        registry.replace(player, SchedulerManager.TaskType.UPDATE, update);
        registry.replace(player, SchedulerManager.TaskType.REOPEN, reopen);

        assertEquals(2, registry.cancelAll(player));

        assertTrue(update.cancelled);
        assertTrue(reopen.cancelled);
        assertEquals(0, registry.taskCount(player));
    }

    @Test
    public void nullScheduledTasksAreNotRegistered() {
        SchedulerTaskRegistry<FakeTask> registry = new SchedulerTaskRegistry<>(FakeTask::cancel);
        Player player = player();

        registry.replace(player, SchedulerManager.TaskType.UPDATE, null);

        assertEquals(0, registry.taskCount(player));
        assertFalse(registry.cancel(player, SchedulerManager.TaskType.UPDATE));
    }

    private static Player player() {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
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

    private static final class FakeTask {
        private boolean cancelled;

        private void cancel() {
            cancelled = true;
        }
    }
}
