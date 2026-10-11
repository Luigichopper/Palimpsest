package io.github.luigichopper.palimpsest.level;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class VoidChunkGenerator extends ChunkGenerator {
    public static final MapCodec<VoidChunkGenerator> CODEC = MapCodec.unit(VoidChunkGenerator::new);

    public static class VoidBiomeSource extends BiomeSource {
        public static final MapCodec<VoidBiomeSource> CODEC = MapCodec.unit(VoidBiomeSource::new);

        @Override
        public MapCodec<? extends BiomeSource> codec() {
            return CODEC;
        }

        @Override
        protected java.util.stream.Stream<Holder<Biome>> collectPossibleBiomes() {
            return java.util.stream.Stream.empty();
        }

        @Override
        public Set<Holder<Biome>> possibleBiomes() {
            return Collections.emptySet();
        }

        @Override
        public BiomeResolver createResolver(Climate.Sampler sampler) {
            return (x, y, z) -> null;
        }
    }

    public VoidChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    public VoidChunkGenerator() {
        super(new VoidBiomeSource());
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
        return CompletableFuture.completedFuture(chunk);
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
        return -64;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        return new NoiseColumn(-64, new BlockState[0]);
    }

    @Override
    public void addDebugScreenInfo(List<String> lines, RandomState randomState, BlockPos pos, SamplerContext context) {
    }
}
