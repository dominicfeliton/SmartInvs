package fr.minuskube.inv;

import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.opener.InventoryOpener;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InventoryManagerLifecycleTest {

    @Test
    public void scheduleUpdateTaskUsesUpdateTaskTypeAndInventoryFrequency() {
        RecordingScheduler scheduler = new RecordingScheduler();
        InventoryManager manager = manager(scheduler);
        Player player = mock(Player.class);
        InventoryContents contents = mock(InventoryContents.class);
        InventoryProvider provider = mock(InventoryProvider.class);
        SmartInventory inventory = mock(SmartInventory.class);
        when(inventory.getProvider()).thenReturn(provider);
        when(inventory.getUpdateFrequency()).thenReturn(5);

        manager.setContents(player, contents);
        manager.scheduleUpdateTask(player, inventory);

        assertEquals(1, scheduler.runCalls.size());
        RunCall call = scheduler.runCalls.get(0);
        assertSame(player, call.player);
        assertEquals(1, call.delay);
        assertEquals(5, call.period);
        assertEquals(SchedulerManager.SchedulerType.ENTITY, call.schedulerType);
        assertEquals(SchedulerManager.TaskType.UPDATE, call.taskType);
    }

    @Test
    public void nonCloseableCloseSchedulesReopenWithoutCancellingUpdateTask() {
        RecordingScheduler scheduler = new RecordingScheduler();
        InventoryManager manager = manager(scheduler);
        Player player = mock(Player.class);
        SmartInventory inventory = mockInventory(false);
        InventoryCloseEvent event = closeEvent(player, mock(Inventory.class));
        manager.setInventory(player, inventory);
        manager.setContents(player, mock(InventoryContents.class));

        manager.new InvListener().onInventoryClose(event);

        assertTrue(scheduler.cancelAllPlayers.isEmpty());
        assertTrue(manager.getInventory(player).isPresent());
        assertTrue(manager.getContents(player).isPresent());
        assertEquals(1, scheduler.runCalls.size());
        RunCall call = scheduler.runCalls.get(0);
        assertEquals(SchedulerManager.TaskType.REOPEN, call.taskType);
        assertEquals(SchedulerManager.SchedulerType.ENTITY, call.schedulerType);
        assertEquals(0, call.delay);
        assertEquals(0, call.period);
    }

    @Test
    public void closeableCloseCancelsAllPlayerTasksAndClearsState() {
        RecordingScheduler scheduler = new RecordingScheduler();
        InventoryManager manager = manager(scheduler);
        Player player = mock(Player.class);
        SmartInventory inventory = mockInventory(true);
        Inventory eventInventory = mock(Inventory.class);
        InventoryCloseEvent event = closeEvent(player, eventInventory);
        manager.setInventory(player, inventory);
        manager.setContents(player, mock(InventoryContents.class));

        manager.new InvListener().onInventoryClose(event);

        assertEquals(Collections.singletonList(player), scheduler.cancelAllPlayers);
        assertFalse(manager.getInventory(player).isPresent());
        assertFalse(manager.getContents(player).isPresent());
        verify(eventInventory).clear();
    }

    @Test
    public void playerQuitCancelsAllPlayerTasksAndClearsState() {
        RecordingScheduler scheduler = new RecordingScheduler();
        InventoryManager manager = manager(scheduler);
        Player player = mock(Player.class);
        SmartInventory inventory = mockInventory(true);
        PlayerQuitEvent event = new PlayerQuitEvent(player, null);
        manager.setInventory(player, inventory);
        manager.setContents(player, mock(InventoryContents.class));

        manager.new InvListener().onPlayerQuit(event);

        assertEquals(Collections.singletonList(player), scheduler.cancelAllPlayers);
        assertFalse(manager.getInventory(player).isPresent());
        assertFalse(manager.getContents(player).isPresent());
    }

    @Test
    public void openingNewInventoryCancelsAllExistingPlayerTasksBeforeSchedulingUpdate() {
        RecordingScheduler scheduler = new RecordingScheduler();
        InventoryManager manager = manager(scheduler);
        Player player = mock(Player.class);
        SmartInventory oldInventory = mockInventory(true);
        InventoryView openView = mock(InventoryView.class);
        when(player.getOpenInventory()).thenReturn(openView);
        manager.setInventory(player, oldInventory);
        manager.setContents(player, mock(InventoryContents.class));

        Inventory handle = mock(Inventory.class);
        InventoryOpener opener = mock(InventoryOpener.class);
        when(opener.supports(InventoryType.CHEST)).thenReturn(true);
        when(opener.open(any(SmartInventory.class), eq(player))).thenReturn(handle);
        manager.registerOpeners(opener);

        InventoryProvider provider = mock(InventoryProvider.class);
        SmartInventory newInventory = SmartInventory.builder()
                .manager(manager)
                .provider(provider)
                .type(InventoryType.CHEST)
                .size(1, 9)
                .updateFrequency(3)
                .build();

        Inventory result = newInventory.open(player);

        assertSame(handle, result);
        assertEquals(Collections.singletonList(player), scheduler.cancelAllPlayers);
        assertSame(newInventory, manager.getInventory(player).get());
        assertEquals(1, scheduler.runCalls.size());
        assertEquals(SchedulerManager.TaskType.UPDATE, scheduler.runCalls.get(0).taskType);
        assertEquals(1, scheduler.runCalls.get(0).delay);
        assertEquals(3, scheduler.runCalls.get(0).period);
        verify(provider).init(eq(player), any(InventoryContents.class));
    }

    private static InventoryManager manager(SchedulerManager scheduler) {
        return new InventoryManager(mock(JavaPlugin.class), mock(PluginManager.class), scheduler);
    }

    private static SmartInventory mockInventory(boolean closeable) {
        SmartInventory inventory = mock(SmartInventory.class);
        when(inventory.isCloseable()).thenReturn(closeable);
        when(inventory.getListeners()).thenReturn(Collections.emptyList());
        return inventory;
    }

    private static InventoryCloseEvent closeEvent(Player player, Inventory inventory) {
        InventoryView view = mock(InventoryView.class);
        when(view.getPlayer()).thenReturn(player);
        when(view.getTopInventory()).thenReturn(inventory);
        return new InventoryCloseEvent(view);
    }

    private static final class RecordingScheduler implements SchedulerManager {
        private final List<RunCall> runCalls = new ArrayList<>();
        private final List<Player> cancelAllPlayers = new ArrayList<>();

        @Override
        public void runTask(BukkitRunnable task, Player player, long delay, long period, SchedulerType type) {
            runTask(task, player, delay, period, type, TaskType.UPDATE);
        }

        @Override
        public void runTask(BukkitRunnable task, Player player, long delay, long period, SchedulerType type, TaskType taskType) {
            runCalls.add(new RunCall(player, delay, period, type, taskType));
        }

        @Override
        public void cancelTaskByPlayer(Player player) {
            cancelTaskByPlayer(player, TaskType.UPDATE);
        }

        @Override
        public void cancelTaskByPlayer(Player player, TaskType taskType) {
        }

        @Override
        public void cancelAllTasksByPlayer(Player player) {
            cancelAllPlayers.add(player);
        }
    }

    private static final class RunCall {
        private final Player player;
        private final long delay;
        private final long period;
        private final SchedulerManager.SchedulerType schedulerType;
        private final SchedulerManager.TaskType taskType;

        private RunCall(Player player, long delay, long period,
                        SchedulerManager.SchedulerType schedulerType, SchedulerManager.TaskType taskType) {
            this.player = player;
            this.delay = delay;
            this.period = period;
            this.schedulerType = schedulerType;
            this.taskType = taskType;
        }
    }
}
