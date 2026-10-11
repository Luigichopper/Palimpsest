package io.github.luigichopper.palimpsest;

import io.github.luigichopper.palimpsest.level.LevelKind;
import io.github.luigichopper.palimpsest.level.LevelRegistryEntry;
import io.github.luigichopper.palimpsest.level.VoidChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class LevelSpikeTest {

    @org.junit.jupiter.api.BeforeAll
    public static void initMinecraft() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    public void testV6CustomChunkGeneratorRequirements() {
        VoidChunkGenerator generator = new VoidChunkGenerator();
        assertNotNull(generator);
        assertNotNull(generator.codec());
        assertEquals(-64, generator.getMinY());
        assertEquals(384, generator.getGenDepth());
        assertEquals(63, generator.getSeaLevel());
    }

    @Test
    public void testV7ChunkTagStructure() {
        CompoundTag chunkTag = new CompoundTag();
        chunkTag.putInt("xPos", 0);
        chunkTag.putInt("zPos", 0);
        chunkTag.putString("Status", "minecraft:full");

        assertEquals(0, chunkTag.getInt("xPos").orElse(-1));
        assertEquals(0, chunkTag.getInt("zPos").orElse(-1));
        assertEquals("minecraft:full", chunkTag.getString("Status").orElse(""));
    }

    @Test
    public void testV8PersistentLevelDiskPathResolution() {
        Identifier dimId = Identifier.fromNamespaceAndPath("palimpsest", "mirror");
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimId);

        Path worldDir = Path.of("run/world");
        Path expectedDimDir = worldDir.resolve("dimensions").resolve(dimId.getNamespace()).resolve(dimId.getPath());

        assertEquals("run/world/dimensions/palimpsest/mirror".replace('/', java.io.File.separatorChar),
            expectedDimDir.toString());
    }
}
