package io.github.luigichopper.palimpsest.core;

import net.minecraft.server.MinecraftServer;

public interface ScheduledTask extends Comparable<ScheduledTask> {
    String name();

    /**
     * Higher priority tasks run before lower priority tasks.
     */
    int priority();

    /**
     * Executes a slice of work.
     * @param server The MinecraftServer context
     * @return true if the task is completely finished, false if it should be resumed.
     */
    boolean tick(MinecraftServer server);

    void cancel();

    boolean isCancelled();

    float getProgress();

    @Override
    default int compareTo(ScheduledTask other) {
        // Sort descending by priority
        return Integer.compare(other.priority(), this.priority());
    }
}
