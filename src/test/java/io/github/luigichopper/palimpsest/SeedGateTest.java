package io.github.luigichopper.palimpsest;

import io.github.luigichopper.palimpsest.config.PalimpsestConfig;
import io.github.luigichopper.palimpsest.core.SeedGate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class SeedGateTest {
    private SeedGate seedGate;

    @BeforeEach
    public void setup() {
        PalimpsestConfig.get().devMode = false;
        PalimpsestConfig.get().forceActivate = false;
        seedGate = new SeedGate();
    }

    @Test
    public void testKnownHashComputation() {
        String salt = "test_salt_123";
        String seedStr = "123456789";
        String hash = SeedGate.computeHash(salt, seedStr);
        assertNotNull(hash);
        assertEquals(64, hash.length());
        // Verify deterministic output
        assertEquals(hash, SeedGate.computeHash(salt, seedStr));
    }

    @Test
    public void testIneligibleSeedFails() {
        assertFalse(seedGate.isEligible(99999999999999L));
        assertFalse(seedGate.isEligible(0L));
    }

    @Test
    public void testBypassMode() {
        PalimpsestConfig.get().devMode = true;
        PalimpsestConfig.get().forceActivate = true;

        assertTrue(seedGate.isEligible(99999999999999L));
        assertTrue(seedGate.isEligible(0L));
    }

    @Test
    public void testMatchesPrivateSeedIfPresent() {
        Path seedFile = Path.of("private/seed.txt");
        if (Files.exists(seedFile)) {
            try {
                String seedStr = Files.readString(seedFile).trim();
                long seed = Long.parseLong(seedStr);
                assertTrue(seedGate.isEligible(seed), "The seed stored in private/seed.txt must be eligible");
            } catch (IOException | NumberFormatException e) {
                fail("Failed to read private/seed.txt: " + e.getMessage());
            }
        }
    }
}
