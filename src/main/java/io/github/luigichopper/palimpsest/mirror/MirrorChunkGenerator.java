package io.github.luigichopper.palimpsest.mirror;

import com.mojang.serialization.MapCodec;
import io.github.luigichopper.palimpsest.level.VoidChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class MirrorChunkGenerator extends ChunkGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger("Palimpsest/MirrorChunkGenerator");
    public static final MapCodec<MirrorChunkGenerator> CODEC = MapCodec.unit(MirrorChunkGenerator::new);

    private MinecraftServer server;

    public MirrorChunkGenerator(MinecraftServer server) {
        super(new VoidChunkGenerator.VoidBiomeSource());
        this.server = server;
    }

    public MirrorChunkGenerator() {
        super(new VoidChunkGenerator.VoidBiomeSource());
        this.server = null;
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> buildTerrain(
        ChunkAccess chunk,
        Blender blender,
        RandomState randomState,
        StructureManager structureManager,
        BiomeManager biomeManager,
        WorldGenRegion region,
        Set<Holder<Biome>> biomes
    ) {
        if (server == null && region != null && region.getLevel() != null) {
            server = region.getLevel().getServer();
        }

        if (server == null) {
            return CompletableFuture.completedFuture(chunk);
        }

        ServerLevel anchorWorld = server.getLevel(Level.OVERWORLD);
        if (anchorWorld == null) {
            return CompletableFuture.completedFuture(chunk);
        }

        ChunkPos mirrorPos = chunk.getPos();
        ChunkPos anchorChunkPos = new ChunkPos(-mirrorPos.x() - 1, mirrorPos.z());

        // Read chunk data asynchronously from storage without loading or forcing generation
        return anchorWorld.getChunkSource().chunkMap.read(anchorChunkPos).thenApply(optionalTag -> {
            if (optionalTag.isEmpty()) {
                // Anchor chunk was never generated: render as void
                return chunk;
            }

            try {
                CompoundTag tag = optionalTag.get();
                PalettedContainerFactory factory = PalettedContainerFactory.create(region.registryAccess());
                SerializableChunkData data = SerializableChunkData.parse(region, factory, tag);

                for (SerializableChunkData.SectionData sectionData : data.sectionData()) {
                    int sectionY = sectionData.y();
                    LevelChunkSection sourceSection = sectionData.chunkSection();
                    if (sourceSection == null || sourceSection.hasOnlyAir()) {
                        continue;
                    }

                    int sectionIndex = chunk.getSectionIndexFromSectionY(sectionY);
                    if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) {
                        continue;
                    }

                    LevelChunkSection targetSection = chunk.getSection(sectionIndex);

                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            for (int y = 0; y < 16; y++) {
                                BlockState state = sourceSection.getBlockState(x, y, z);
                                if (!state.isAir()) {
                                    BlockState mirrored = state.mirror(Mirror.FRONT_BACK);
                                    int targetX = 15 - x;
                                    targetSection.setBlockState(targetX, y, z, mirrored);
                                }
                            }
                        }
                    }
                }

                // Copy and reflect block entities
                for (CompoundTag beTag : data.blockEntities()) {
                    CompoundTag mirroredBeTag = beTag.copy();
                    int origX = beTag.getInt("x").orElse(0);
                    int origZ = beTag.getInt("z").orElse(0);
                    int targetX = -origX - 1;
                    mirroredBeTag.putInt("x", targetX);
                    mirroredBeTag.putInt("z", origZ);
                    chunk.setBlockEntityNbt(mirroredBeTag);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to reflect anchor chunk {} into mirror chunk {}: {}", anchorChunkPos, mirrorPos, e.getMessage());
            }

            return chunk;
        });
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return 0;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        return new NoiseColumn(0, new BlockState[0]);
    }

    @Override
    public void addDebugScreenInfo(List<String> lines, RandomState randomState, BlockPos pos, SamplerContext context) {
        lines.add("MirrorChunkGenerator: Reflected Anchor");
    }
}
