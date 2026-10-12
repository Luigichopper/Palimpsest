package io.github.luigichopper.palimpsest.resonance;

import io.github.luigichopper.palimpsest.core.StoryState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ResonanceBusTest {

    private ResonanceBus bus;
    private StoryState storyState;
    private final Identifier anchorDim = ResonanceBus.ANCHOR_DIM;
    private final long gameTime = 100L;

    @BeforeAll
    public static void initMinecraft() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @BeforeEach
    public void setUp() {
        bus = ResonanceBus.get();
        bus.loadRules();
        storyState = new StoryState();
    }

    @Test
    public void testCoordinateReflectionIsInvolution() {
        BlockPos original = new BlockPos(42, 65, -18);
        BlockPos reflected = ResonanceBus.reflectCoordinate(original);
        assertEquals(-43, reflected.getX());
        assertEquals(65, reflected.getY());
        assertEquals(-18, reflected.getZ());

        BlockPos roundTrip = ResonanceBus.reflectCoordinate(reflected);
        assertEquals(original, roundTrip, "Reflecting twice must return the original position");
    }

    @Test
    public void testLoopPreventionSuppression() {
        bus.setSuppressed(true);
        try {
            BlockPos pos = new BlockPos(10, 64, 10);
            bus.onBlockChanged(anchorDim, gameTime, pos, Blocks.AIR.defaultBlockState(), Blocks.STONE.defaultBlockState(), storyState);
            assertEquals(0, storyState.getPendingResonanceCount(), "Suppressed resonance should not enqueue any events");
        } finally {
            bus.setSuppressed(false);
        }
    }

    @Test
    public void testPlacing1000BlocksEnforcesBoundedQueue() {
        // Spec 7.7 Acceptance: "a Resonance rule round-trips without infinite loops (test: place 1000 blocks, assert bounded queue)"
        for (int i = 0; i < 1000; i++) {
            BlockPos pos = new BlockPos(i, 64, 0);
            bus.onBlockChanged(anchorDim, gameTime, pos, Blocks.AIR.defaultBlockState(), Blocks.COBBLESTONE.defaultBlockState(), storyState);
        }

        assertTrue(storyState.getPendingResonanceCount() <= ResonanceBus.MAX_QUEUE_SIZE,
            "Queue size must be bounded by MAX_QUEUE_SIZE (" + ResonanceBus.MAX_QUEUE_SIZE + "), got: " + storyState.getPendingResonanceCount());
        assertEquals(ResonanceBus.MAX_QUEUE_SIZE, storyState.getPendingResonanceCount(), "Should clamp to exact max capacity");
    }

    @Test
    public void testInvertRulesMatchExpectedBlocks() {
        // Torch -> Soul Torch
        BlockPos torchPos = new BlockPos(5, 64, 5);
        bus.onBlockChanged(anchorDim, gameTime, torchPos, Blocks.AIR.defaultBlockState(), Blocks.TORCH.defaultBlockState(), storyState);

        assertEquals(1, storyState.getPendingResonanceCount());
        PendingResonance res = storyState.getPendingResonances().get(0);
        assertEquals(ResonanceBus.MIRROR_DIM, res.dimensionId());
        assertEquals(new BlockPos(-6, 64, 5), res.pos());
        assertEquals(Blocks.SOUL_TORCH, res.state().getBlock(), "Torch should invert to Soul Torch");
    }
}
