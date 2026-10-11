package io.github.luigichopper.palimpsest;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.luigichopper.palimpsest.core.AnchorPos;
import io.github.luigichopper.palimpsest.core.StoryState;
import io.github.luigichopper.palimpsest.level.LevelKind;
import io.github.luigichopper.palimpsest.level.LevelRegistryEntry;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class StoryStateTest {

    @Test
    public void testWorldAndPlayerFlags() {
        StoryState state = new StoryState();

        assertFalse(state.hasWorldFlag("ch0.lectern_read"));
        state.setWorldFlag("ch0.lectern_read");
        assertTrue(state.hasWorldFlag("ch0.lectern_read"));
        state.clearWorldFlag("ch0.lectern_read");
        assertFalse(state.hasWorldFlag("ch0.lectern_read"));

        UUID player = UUID.randomUUID();
        assertFalse(state.hasPlayerFlag(player, "seen_double"));
        state.setPlayerFlag(player, "seen_double");
        assertTrue(state.hasPlayerFlag(player, "seen_double"));
    }

    @Test
    public void testAnchorPosAndLandmarks() {
        StoryState state = new StoryState();
        UUID player = UUID.randomUUID();

        AnchorPos pos = new AnchorPos(Identifier.fromNamespaceAndPath("minecraft", "overworld"), 100.5, 64.0, -200.5, 90f, 0f);
        state.setLastAnchorPos(player, pos);

        AnchorPos retrieved = state.getLastAnchorPos(player);
        assertNotNull(retrieved);
        assertEquals(100.5, retrieved.x());
        assertEquals(64.0, retrieved.y());
        assertEquals(-200.5, retrieved.z());

        assertFalse(state.isLandmarkPlaced("L0_Cabin"));
        state.markLandmarkPlaced("L0_Cabin");
        assertTrue(state.isLandmarkPlaced("L0_Cabin"));
    }

    @Test
    public void testLevelRegistry() {
        StoryState state = new StoryState();
        Identifier id = Identifier.fromNamespaceAndPath("palimpsest", "mirror");
        LevelRegistryEntry entry = new LevelRegistryEntry(
            id,
            LevelKind.MIRROR,
            Identifier.fromNamespaceAndPath("palimpsest", "mirror"),
            123456789L,
            "NORMAL",
            false,
            "mirror"
        );

        state.registerLevel(entry);
        assertTrue(state.isLevelRegistered(id));
        assertEquals(LevelKind.MIRROR, state.getRegisteredLevels().get(id).kind());

        state.unregisterLevel(id);
        assertFalse(state.isLevelRegistered(id));
    }

    @Test
    public void testCodecRoundTrip() {
        StoryState state = new StoryState();
        state.setWorldFlag("test_flag_alpha");
        UUID player = UUID.randomUUID();
        state.setPlayerFlag(player, "player_flag_beta");
        state.setLastAnchorPos(player, new AnchorPos(Identifier.fromNamespaceAndPath("minecraft", "overworld"), 10.0, 70.0, 20.0, 180f, 45f));
        state.markLandmarkPlaced("L1_Annex");
        state.markPuzzleSolved("P0.1");

        Identifier mirrorId = Identifier.fromNamespaceAndPath("palimpsest", "mirror");
        state.registerLevel(new LevelRegistryEntry(
            mirrorId,
            LevelKind.MIRROR,
            Identifier.fromNamespaceAndPath("palimpsest", "mirror"),
            987654321L,
            "HARD",
            false,
            "mirror"
        ));

        // Encode to JSON using Codec
        JsonElement json = StoryState.CODEC.encodeStart(JsonOps.INSTANCE, state)
            .getOrThrow(msg -> new AssertionError("Failed to encode: " + msg));

        // Decode from JSON
        StoryState decoded = StoryState.CODEC.parse(JsonOps.INSTANCE, json)
            .getOrThrow(msg -> new AssertionError("Failed to decode: " + msg));

        // Assert all fields preserved
        assertTrue(decoded.hasWorldFlag("test_flag_alpha"));
        assertTrue(decoded.hasPlayerFlag(player, "player_flag_beta"));
        assertTrue(decoded.isLandmarkPlaced("L1_Annex"));
        assertTrue(decoded.isPuzzleSolved("P0.1"));
        assertTrue(decoded.isLevelRegistered(mirrorId));
        assertEquals(70.0, decoded.getLastAnchorPos(player).y());
        assertEquals("HARD", decoded.getRegisteredLevels().get(mirrorId).difficulty());
    }
}
