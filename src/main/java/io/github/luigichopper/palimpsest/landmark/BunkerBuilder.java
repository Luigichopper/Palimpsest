package io.github.luigichopper.palimpsest.landmark;

import io.github.luigichopper.palimpsest.core.StoryState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;

public class BunkerBuilder {

    public static void build(ServerLevel level, BlockPos anchor, RandomSource random, StoryState storyState) {
        // Bunker Excavation & Shell: Stone Bricks and Reinforced Deepslate
        // 5 wide (-2 to +2), 4 high (1 to 4), 9 deep (0 to 8)
        BlockPos tunnelMin = anchor.offset(-2, 0, 0);
        BlockPos tunnelMax = anchor.offset(2, 4, 8);

        ProceduralDsl.box(level, tunnelMin, tunnelMax, Blocks.STONE_BRICKS.defaultBlockState(), Blocks.AIR.defaultBlockState());

        // Floor: Polished Deepslate
        ProceduralDsl.fill(level, anchor.offset(-2, 0, 0), anchor.offset(2, 0, 8), Blocks.POLISHED_DEEPSLATE.defaultBlockState());

        // Weathering / distress
        level.setBlock(anchor.offset(-2, 2, 2), Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 3);
        level.setBlock(anchor.offset(2, 3, 4), Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 3);
        level.setBlock(anchor.offset(-1, 4, 5), Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 3);

        // Entrance Portal / Iron Door at Z = 2
        BlockPos doorPos = anchor.offset(0, 1, 2);
        ProceduralDsl.setDoor(level, doorPos, Blocks.IRON_DOOR, Direction.SOUTH);

        // Stone frame around door
        level.setBlock(anchor.offset(-1, 1, 2), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 3);
        level.setBlock(anchor.offset(-1, 2, 2), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 3);
        level.setBlock(anchor.offset(1, 1, 2), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 3);
        level.setBlock(anchor.offset(1, 2, 2), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 3);
        level.setBlock(anchor.offset(0, 3, 2), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 3);

        // Redstone Lamp & Lever above door
        level.setBlock(anchor.offset(0, 4, 2), Blocks.REDSTONE_LAMP.defaultBlockState(), 3);

        // 4 Decorated Pots outside the door (Z = 0) on pedestals
        // Representing the 4-sherd cipher sequence
        level.setBlock(anchor.offset(-2, 1, 0), Blocks.DECORATED_POT.defaultBlockState(), 3);
        level.setBlock(anchor.offset(-1, 1, 0), Blocks.DECORATED_POT.defaultBlockState(), 3);
        level.setBlock(anchor.offset(1, 1, 0), Blocks.DECORATED_POT.defaultBlockState(), 3);
        level.setBlock(anchor.offset(2, 1, 0), Blocks.DECORATED_POT.defaultBlockState(), 3);

        // Terminal Console at the bunker gate (for code entry or node diagnostics)
        BlockPos termPos = anchor.offset(1, 1, 1);
        ProceduralDsl.setTerminal(level, termPos, "term1_bunker");

        // Inside the bunker behind the door (Z = 4 to 8): Descending stairway / chute
        ProceduralDsl.fill(level, anchor.offset(-1, 1, 4), anchor.offset(1, 1, 7), Blocks.AIR.defaultBlockState());
        level.setBlock(anchor.offset(0, 1, 7), Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.defaultBlockState(), 3);

        // Ambient Lighting inside
        level.setBlock(anchor.offset(-1, 3, 5), Blocks.SOUL_LANTERN.defaultBlockState(), 3);
    }
}
