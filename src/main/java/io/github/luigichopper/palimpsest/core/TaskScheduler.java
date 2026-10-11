package io.github.luigichopper.palimpsest.core;

import io.github.luigichopper.palimpsest.config.PalimpsestConfig;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TaskScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger("Palimpsest/Scheduler");
    private static final TaskScheduler INSTANCE = new TaskScheduler();

    private final PriorityQueue<ScheduledTask> queue = new PriorityQueue<>();
    private final ConcurrentLinkedQueue<ScheduledTask> incoming = new ConcurrentLinkedQueue<>();
    private final Object lock = new Object();

    private long lastTickNanos = 0;
    private int completedTaskCount = 0;

    public static TaskScheduler get() {
        return INSTANCE;
    }

    public void submit(ScheduledTask task) {
        if (task != null) {
            incoming.add(task);
        }
    }

    public void tick(MinecraftServer server) {
        long start = System.nanoTime();
        int budgetMs = PalimpsestConfig.get().scheduler.msPerTick;
        long budgetNs = Math.max(1, budgetMs) * 1_000_000L;

        // Drain incoming tasks into priority queue
        ScheduledTask newTask;
        synchronized (lock) {
            while ((newTask = incoming.poll()) != null) {
                if (!newTask.isCancelled()) {
                    queue.add(newTask);
                }
            }

            if (queue.isEmpty()) {
                lastTickNanos = System.nanoTime() - start;
                return;
            }

            List<ScheduledTask> incompleteTasks = new ArrayList<>();

            while (!queue.isEmpty()) {
                if (System.nanoTime() - start >= budgetNs) {
                    break;
                }

                ScheduledTask current = queue.poll();
                if (current.isCancelled()) {
                    continue;
                }

                boolean finished = false;
                try {
                    finished = current.tick(server);
                } catch (Exception e) {
                    LOGGER.error("Error executing scheduled task '{}'", current.name(), e);
                    finished = true; // drop failing task
                }

                if (finished) {
                    completedTaskCount++;
                } else if (!current.isCancelled()) {
                    incompleteTasks.add(current);
                }
            }

            queue.addAll(incompleteTasks);
        }

        lastTickNanos = System.nanoTime() - start;
    }

    public int getPendingTaskCount() {
        synchronized (lock) {
            return queue.size() + incoming.size();
        }
    }

    public int getCompletedTaskCount() {
        return completedTaskCount;
    }

    public long getLastTickNanos() {
        return lastTickNanos;
    }

    public void clear() {
        synchronized (lock) {
            queue.clear();
            incoming.clear();
        }
    }
}
