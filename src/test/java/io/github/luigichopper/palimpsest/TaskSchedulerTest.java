package io.github.luigichopper.palimpsest;

import io.github.luigichopper.palimpsest.config.PalimpsestConfig;
import io.github.luigichopper.palimpsest.core.ScheduledTask;
import io.github.luigichopper.palimpsest.core.TaskScheduler;
import net.minecraft.server.MinecraftServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TaskSchedulerTest {
    private TaskScheduler scheduler;

    @BeforeEach
    public void setup() {
        scheduler = TaskScheduler.get();
        scheduler.clear();
        PalimpsestConfig.get().scheduler.msPerTick = 3;
    }

    @Test
    public void testTaskPrioritization() {
        List<String> executionOrder = new ArrayList<>();

        scheduler.submit(new ScheduledTask() {
            @Override public String name() { return "Low"; }
            @Override public int priority() { return 10; }
            @Override public boolean tick(MinecraftServer server) { executionOrder.add("Low"); return true; }
            @Override public void cancel() {}
            @Override public boolean isCancelled() { return false; }
            @Override public float getProgress() { return 1f; }
        });

        scheduler.submit(new ScheduledTask() {
            @Override public String name() { return "High"; }
            @Override public int priority() { return 100; }
            @Override public boolean tick(MinecraftServer server) { executionOrder.add("High"); return true; }
            @Override public void cancel() {}
            @Override public boolean isCancelled() { return false; }
            @Override public float getProgress() { return 1f; }
        });

        scheduler.tick(null);

        assertEquals(2, executionOrder.size());
        assertEquals("High", executionOrder.get(0));
        assertEquals("Low", executionOrder.get(1));
    }

    @Test
    public void testTaskCancellability() {
        AtomicBoolean ran = new AtomicBoolean(false);

        ScheduledTask task = new ScheduledTask() {
            private boolean cancelled = false;
            @Override public String name() { return "Cancellable"; }
            @Override public int priority() { return 50; }
            @Override public boolean tick(MinecraftServer server) { ran.set(true); return true; }
            @Override public void cancel() { cancelled = true; }
            @Override public boolean isCancelled() { return cancelled; }
            @Override public float getProgress() { return 0f; }
        };

        scheduler.submit(task);
        task.cancel();

        scheduler.tick(null);
        assertFalse(ran.get(), "Cancelled task must not run");
    }

    @Test
    public void testBudgetEnforcementUnderStress() {
        // Configure strict budget: 2ms
        PalimpsestConfig.get().scheduler.msPerTick = 2;

        AtomicInteger slicesRan = new AtomicInteger(0);
        // Create a heavy resumable task (simulating 100 chunk copies)
        ScheduledTask heavyTask = new ScheduledTask() {
            private int progress = 0;
            @Override public String name() { return "HeavyChunkCopy"; }
            @Override public int priority() { return 50; }
            @Override public boolean tick(MinecraftServer server) {
                slicesRan.incrementAndGet();
                // Simulate work: burn 1ms
                long start = System.nanoTime();
                while (System.nanoTime() - start < 1_000_000L) {
                    // spin
                }
                progress++;
                return progress >= 100;
            }
            @Override public void cancel() {}
            @Override public boolean isCancelled() { return false; }
            @Override public float getProgress() { return progress / 100f; }
        };

        scheduler.submit(heavyTask);

        // Run a single server tick slice
        scheduler.tick(null);

        // In 1 tick with 2ms budget, it should only run 1-3 slices, not all 100!
        int ranInFirstTick = slicesRan.get();
        assertTrue(ranInFirstTick > 0, "At least 1 slice should run");
        assertTrue(ranInFirstTick < 10, "Scheduler must clamp execution and not run all heavy tasks at once");
        assertTrue(scheduler.getLastTickNanos() > 0);
    }
}
