package io.github.luigichopper.palimpsest.diff;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LevelDiffServiceTest {

    @BeforeAll
    public static void initMinecraft() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    public void testIdenticalRegionsProduceZeroDiffs() {
        Map<BlockPos, BlockState> levelA = new HashMap<>();
        Map<BlockPos, BlockState> levelB = new HashMap<>();

        BlockPos center = new BlockPos(0, 64, 0);
        levelA.put(center, Blocks.STONE.defaultBlockState());
        levelB.put(center, Blocks.STONE.defaultBlockState());

        List<DiffEntry> diffs = LevelDiffService.compare(
            pos -> levelA.getOrDefault(pos, Blocks.AIR.defaultBlockState()),
            pos -> levelB.getOrDefault(pos, Blocks.AIR.defaultBlockState()),
            60, 70, center, 5, DiffTransform.IDENTITY, 100
        );

        assertTrue(diffs.isEmpty(), "Identical regions should produce 0 diffs");
    }

    @Test
    public void testPlantedDiffsCategorization() {
        Map<BlockPos, BlockState> levelA = new HashMap<>();
        Map<BlockPos, BlockState> levelB = new HashMap<>();

        BlockPos center = new BlockPos(0, 64, 0);

        // 1. ADDED: Air in A, Stone in B at (1, 64, 0)
        BlockPos addedPos = new BlockPos(1, 64, 0);
        levelB.put(addedPos, Blocks.STONE.defaultBlockState());

        // 2. REMOVED: Oak Planks in A, Air in B at (0, 64, 2)
        BlockPos removedPos = new BlockPos(0, 64, 2);
        levelA.put(removedPos, Blocks.OAK_PLANKS.defaultBlockState());

        // 3. CHANGED: Dirt in A, Cobblestone in B at (-1, 64, 0)
        BlockPos changedPos = new BlockPos(-1, 64, 0);
        levelA.put(changedPos, Blocks.DIRT.defaultBlockState());
        levelB.put(changedPos, Blocks.COBBLESTONE.defaultBlockState());

        List<DiffEntry> diffs = LevelDiffService.compare(
            pos -> levelA.getOrDefault(pos, Blocks.AIR.defaultBlockState()),
            pos -> levelB.getOrDefault(pos, Blocks.AIR.defaultBlockState()),
            60, 70, center, 5, DiffTransform.IDENTITY, 100
        );

        assertEquals(3, diffs.size(), "Should report exactly 3 planted diffs");

        DiffEntry entryAdded = diffs.stream().filter(d -> d.pos().equals(addedPos)).findFirst().orElse(null);
        assertNotNull(entryAdded);
        assertEquals(DiffCategory.ADDED, entryAdded.category());

        DiffEntry entryRemoved = diffs.stream().filter(d -> d.pos().equals(removedPos)).findFirst().orElse(null);
        assertNotNull(entryRemoved);
        assertEquals(DiffCategory.REMOVED, entryRemoved.category());

        DiffEntry entryChanged = diffs.stream().filter(d -> d.pos().equals(changedPos)).findFirst().orElse(null);
        assertNotNull(entryChanged);
        assertEquals(DiffCategory.CHANGED, entryChanged.category());
    }

    @Test
    public void testDistanceSortingAndResultCapping() {
        Map<BlockPos, BlockState> levelA = new HashMap<>();
        Map<BlockPos, BlockState> levelB = new HashMap<>();

        BlockPos center = new BlockPos(0, 64, 0);

        // Far diff at dist 4
        levelB.put(new BlockPos(4, 64, 0), Blocks.STONE.defaultBlockState());
        // Near diff at dist 1
        levelB.put(new BlockPos(1, 64, 0), Blocks.STONE.defaultBlockState());
        // Mid diff at dist 2
        levelB.put(new BlockPos(2, 64, 0), Blocks.STONE.defaultBlockState());

        List<DiffEntry> diffs = LevelDiffService.compare(
            pos -> levelA.getOrDefault(pos, Blocks.AIR.defaultBlockState()),
            pos -> levelB.getOrDefault(pos, Blocks.AIR.defaultBlockState()),
            60, 70, center, 5, DiffTransform.IDENTITY, 2
        );

        // Capped at 2 results
        assertEquals(2, diffs.size());
        // Closest first
        assertEquals(new BlockPos(1, 64, 0), diffs.get(0).pos());
        assertEquals(new BlockPos(2, 64, 0), diffs.get(1).pos());
    }

    @Test
    public void testReflectTransform() {
        Map<BlockPos, BlockState> levelA = new HashMap<>();
        Map<BlockPos, BlockState> levelB = new HashMap<>();

        BlockPos center = new BlockPos(0, 64, 0);

        // In level A: position (5, 64, 3)
        // With REFLECT transform: x' = -5 - 1 = -6, z' = 3, y' = 64
        BlockPos posA = new BlockPos(5, 64, 3);
        BlockPos posB = new BlockPos(-6, 64, 3);

        levelA.put(posA, Blocks.GLASS.defaultBlockState());
        levelB.put(posB, Blocks.GLASS.defaultBlockState());

        List<DiffEntry> diffs = LevelDiffService.compare(
            pos -> levelA.getOrDefault(pos, Blocks.AIR.defaultBlockState()),
            pos -> levelB.getOrDefault(pos, Blocks.AIR.defaultBlockState()),
            60, 70, center, 10, DiffTransform.REFLECT, 100
        );

        assertTrue(diffs.isEmpty(), "Reflected matching blocks should produce 0 diffs under REFLECT transform");
    }
}
