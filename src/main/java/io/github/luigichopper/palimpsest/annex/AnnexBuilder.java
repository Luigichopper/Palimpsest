package io.github.luigichopper.palimpsest.annex;

import io.github.luigichopper.palimpsest.core.StoryState;
import io.github.luigichopper.palimpsest.landmark.ProceduralDsl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class AnnexBuilder {

    public static void build(ServerLevel level, AnnexTemplate template, StoryState storyState) {
        BlockPos origin = new BlockPos(0, 64, 0);

        // Clear and build main corridor/chamber: 7 wide (-3 to 3), 5 high (64 to 68), 16 deep (0 to 15)
        BlockPos min = origin.offset(-3, 0, 0);
        BlockPos max = origin.offset(3, 4, 15);

        ProceduralDsl.box(level, min, max, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Blocks.AIR.defaultBlockState());

        // Floor: Smooth Stone with Deepslate border
        ProceduralDsl.fill(level, origin.offset(-2, 0, 1), origin.offset(2, 0, 14), Blocks.SMOOTH_STONE.defaultBlockState());

        // Interior dividing arch / locked door between Waiting area and Frame puzzle area at Z = 7
        ProceduralDsl.fill(level, origin.offset(-2, 1, 7), origin.offset(2, 3, 7), Blocks.CHISELED_DEEPSLATE.defaultBlockState());
        level.setBlock(origin.offset(0, 1, 7), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(origin.offset(0, 2, 7), Blocks.AIR.defaultBlockState(), 3);
        ProceduralDsl.setDoor(level, origin.offset(0, 1, 7), Blocks.IRON_DOOR, Direction.SOUTH);

        // Terminal Console in Waiting area (Z = 3)
        BlockPos termPos = origin.offset(2, 1, 3);
        level.setBlock(termPos.below(), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 3);
        ProceduralDsl.setTerminal(level, termPos, "term_annex");

        // Waiting Area lighting
        level.setBlock(origin.offset(0, 4, 3), Blocks.SOUL_LANTERN.defaultBlockState(), 3);

        // Frame Puzzle Area (Z = 8 to 15)
        // Back wall at Z = 14: 3 item frames for octal code (rotations: 5, 2, 7)
        // ItemFrame 1: Compass (octal 5)
        // ItemFrame 2: Clock (octal 2)
        // ItemFrame 3: Echo Shard (octal 7)
        spawnItemFrame(level, origin.offset(-1, 2, 14), Direction.NORTH, Items.COMPASS, 5);
        spawnItemFrame(level, origin.offset(0, 2, 14), Direction.NORTH, Items.CLOCK, 2);
        spawnItemFrame(level, origin.offset(1, 2, 14), Direction.NORTH, Items.ECHO_SHARD, 7);

        // Indicator redstone lamps above frames
        level.setBlock(origin.offset(-1, 3, 14), Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
        level.setBlock(origin.offset(0, 3, 14), Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
        level.setBlock(origin.offset(1, 3, 14), Blocks.REDSTONE_LAMP.defaultBlockState(), 3);

        // Puzzle Area lighting
        level.setBlock(origin.offset(0, 4, 11), Blocks.SOUL_LANTERN.defaultBlockState(), 3);

        // Exit pressure plate at Z = 13 (trigger for completion)
        level.setBlock(origin.offset(0, 1, 13), Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE.defaultBlockState(), 3);
    }

    private static void spawnItemFrame(ServerLevel level, BlockPos pos, Direction facing, net.minecraft.world.item.Item item, int rotation) {
        ItemFrame frame = new ItemFrame(level, pos, facing);
        frame.setItem(new ItemStack(item));
        frame.setRotation(rotation);
        level.addFreshEntity(frame);
    }
}
