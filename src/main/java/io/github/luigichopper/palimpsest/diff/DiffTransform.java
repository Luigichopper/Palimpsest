package io.github.luigichopper.palimpsest.diff;

import net.minecraft.core.BlockPos;

public enum DiffTransform {
    IDENTITY {
        @Override
        public BlockPos apply(BlockPos pos) {
            return pos;
        }
    },
    REFLECT {
        @Override
        public BlockPos apply(BlockPos pos) {
            // Reflect across X-axis: chunk x -> -x-1, block x -> -x-1
            return new BlockPos(-pos.getX() - 1, pos.getY(), pos.getZ());
        }
    };

    public abstract BlockPos apply(BlockPos pos);
}
