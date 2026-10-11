package io.github.luigichopper.palimpsest.landmark;

import io.github.luigichopper.palimpsest.terminal.ModBlocks;
import io.github.luigichopper.palimpsest.terminal.TerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class ProceduralDsl {

    public static void fill(ServerLevel level, BlockPos min, BlockPos max, BlockState state) {
        int minX = Math.min(min.getX(), max.getX());
        int maxX = Math.max(min.getX(), max.getX());
        int minY = Math.min(min.getY(), max.getY());
        int maxY = Math.max(min.getY(), max.getY());
        int minZ = Math.min(min.getZ(), max.getZ());
        int maxZ = Math.max(min.getZ(), max.getZ());

        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    mut.set(x, y, z);
                    level.setBlock(mut, state, Block.UPDATE_ALL);
                }
            }
        }
    }

    public static void box(ServerLevel level, BlockPos min, BlockPos max, BlockState wallState, BlockState interiorState) {
        int minX = Math.min(min.getX(), max.getX());
        int maxX = Math.max(min.getX(), max.getX());
        int minY = Math.min(min.getY(), max.getY());
        int maxY = Math.max(min.getY(), max.getY());
        int minZ = Math.min(min.getZ(), max.getZ());
        int maxZ = Math.max(min.getZ(), max.getZ());

        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    mut.set(x, y, z);
                    boolean isWall = (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ);
                    level.setBlock(mut, isWall ? wallState : interiorState, Block.UPDATE_ALL);
                }
            }
        }
    }

    public static void setDoor(ServerLevel level, BlockPos pos, Block doorBlock, Direction facing) {
        BlockState lower = doorBlock.defaultBlockState()
            .setValue(DoorBlock.FACING, facing)
            .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
            .setValue(DoorBlock.OPEN, false);
        BlockState upper = doorBlock.defaultBlockState()
            .setValue(DoorBlock.FACING, facing)
            .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER)
            .setValue(DoorBlock.OPEN, false);

        level.setBlock(pos, lower, Block.UPDATE_ALL);
        level.setBlock(pos.above(), upper, Block.UPDATE_ALL);
    }

    public static void setTerminal(ServerLevel level, BlockPos pos, String nodeId) {
        level.setBlock(pos, ModBlocks.TERMINAL_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TerminalBlockEntity terminal) {
            terminal.setCustomNodeId(nodeId);
        }
    }

    public static void setLectern(ServerLevel level, BlockPos pos, Direction facing, ItemStack book) {
        BlockState state = Blocks.LECTERN.defaultBlockState()
            .setValue(LecternBlock.FACING, facing)
            .setValue(LecternBlock.HAS_BOOK, book != null && !book.isEmpty());
        level.setBlock(pos, state, Block.UPDATE_ALL);

        if (book != null && !book.isEmpty()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof LecternBlockEntity lectern) {
                lectern.setBook(book);
            }
        }
    }
}
