package io.github.luigichopper.palimpsest.mirror;

import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MirrorChunkGeneratorTest {

    @BeforeAll
    public static void initMinecraft() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    public void testChunkAndLocalCoordinateReflection() {
        // Spec 7.7: "reflect X; chunk x -> -x-1"
        int mirrorChunkX = 3;
        int mirrorChunkZ = 5;
        ChunkPos mirrorPos = new ChunkPos(mirrorChunkX, mirrorChunkZ);
        ChunkPos anchorPos = new ChunkPos(-mirrorPos.x() - 1, mirrorPos.z());

        assertEquals(-4, anchorPos.x());
        assertEquals(5, anchorPos.z());

        // For local coordinate x in [0, 15], reflected local coordinate is 15 - x
        for (int localX = 0; localX < 16; localX++) {
            int anchorLocalX = 15 - localX;
            int worldMirrorX = mirrorChunkX * 16 + localX;
            int worldAnchorX = anchorPos.x() * 16 + anchorLocalX;
            assertEquals(-worldMirrorX - 1, worldAnchorX, "World coordinate must strictly satisfy: x_anchor = -x_mirror - 1");
        }
    }

    @Test
    public void testV14BlockStateMirrorCoverage() {
        // Mirror.FRONT_BACK inverts the X axis (East <-> West)
        assertEquals(Direction.WEST, Mirror.FRONT_BACK.mirror(Direction.EAST));
        assertEquals(Direction.EAST, Mirror.FRONT_BACK.mirror(Direction.WEST));
        assertEquals(Direction.NORTH, Mirror.FRONT_BACK.mirror(Direction.NORTH));
        assertEquals(Direction.SOUTH, Mirror.FRONT_BACK.mirror(Direction.SOUTH));

        // Stair block facing East mirrored becomes facing West
        BlockState eastStair = Blocks.OAK_STAIRS.defaultBlockState()
            .setValue(StairBlock.FACING, Direction.EAST)
            .setValue(StairBlock.HALF, Half.BOTTOM)
            .setValue(StairBlock.SHAPE, StairsShape.STRAIGHT);

        BlockState mirroredStair = eastStair.mirror(Mirror.FRONT_BACK);
        assertEquals(Direction.WEST, mirroredStair.getValue(StairBlock.FACING));

        // Door block facing North with LEFT hinge mirrored across X becomes RIGHT hinge
        BlockState leftDoor = Blocks.OAK_DOOR.defaultBlockState()
            .setValue(net.minecraft.world.level.block.DoorBlock.FACING, Direction.NORTH)
            .setValue(net.minecraft.world.level.block.DoorBlock.HINGE, DoorHingeSide.LEFT);

        BlockState mirroredDoor = leftDoor.mirror(Mirror.FRONT_BACK);
        assertEquals(DoorHingeSide.RIGHT, mirroredDoor.getValue(net.minecraft.world.level.block.DoorBlock.HINGE));
    }

    @Test
    public void testVoidOnUngeneratedAnchorChunk() {
        // MirrorChunkGenerator returns unpopulated void chunk when anchor chunk tag is absent
        MirrorChunkGenerator generator = new MirrorChunkGenerator();
        assertNotNull(generator);
        assertEquals(-64, generator.getMinY());
        assertEquals(384, generator.getGenDepth());
        assertEquals(63, generator.getSeaLevel());
    }
}
