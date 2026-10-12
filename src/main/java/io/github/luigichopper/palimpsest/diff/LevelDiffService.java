package io.github.luigichopper.palimpsest.diff;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class LevelDiffService {

    public static List<DiffEntry> compare(
        ServerLevel levelA,
        ServerLevel levelB,
        BlockPos center,
        int radius,
        DiffTransform transform,
        int maxResults
    ) {
        int minY = Math.max(levelA.getMinY(), center.getY() - 16);
        int maxY = Math.min(levelA.getMaxY() - 1, center.getY() + 24);
        return compare(levelA::getBlockState, levelB::getBlockState, minY, maxY, center, radius, transform, maxResults);
    }

    public static List<DiffEntry> compare(
        Function<BlockPos, BlockState> blocksA,
        Function<BlockPos, BlockState> blocksB,
        int minY,
        int maxY,
        BlockPos center,
        int radius,
        DiffTransform transform,
        int maxResults
    ) {
        List<DiffEntry> diffs = new ArrayList<>();
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;

        BlockPos.MutableBlockPos mutA = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    mutA.set(x, y, z);
                    BlockPos posB = transform.apply(mutA);

                    BlockState stateA = blocksA.apply(mutA);
                    BlockState stateB = blocksB.apply(posB);

                    if (stateA == null || stateB == null || stateA.equals(stateB)) {
                        continue;
                    }

                    boolean aAir = stateA.isAir();
                    boolean bAir = stateB.isAir();

                    if (aAir && bAir) {
                        continue;
                    }

                    DiffCategory category;
                    if (aAir && !bAir) {
                        category = DiffCategory.ADDED;
                    } else if (!aAir && bAir) {
                        category = DiffCategory.REMOVED;
                    } else {
                        category = DiffCategory.CHANGED;
                    }

                    double distSq = mutA.distSqr(center);
                    diffs.add(new DiffEntry(mutA.immutable(), category, distSq));
                }
            }
        }

        diffs.sort(Comparator.comparingDouble(DiffEntry::distanceSq));

        if (diffs.size() > maxResults) {
            return new ArrayList<>(diffs.subList(0, maxResults));
        }

        return diffs;
    }
}
