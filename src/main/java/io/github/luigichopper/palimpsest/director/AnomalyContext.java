package io.github.luigichopper.palimpsest.director;

import io.github.luigichopper.palimpsest.core.StoryState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

public record AnomalyContext(
    ServerLevel level,
    ServerPlayer player,
    TensionTracker tracker,
    long worldSeed,
    int eventIndex,
    RandomSource random,
    StoryState storyState
) {}
