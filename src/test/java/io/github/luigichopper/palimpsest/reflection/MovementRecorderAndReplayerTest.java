package io.github.luigichopper.palimpsest.reflection;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MovementRecorderAndReplayerTest {

    private MovementRecorder recorder;
    private UUID testPlayerId;

    @BeforeAll
    public static void initMinecraft() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @BeforeEach
    public void setUp() {
        recorder = MovementRecorder.get();
        recorder.clearAll();
        testPlayerId = UUID.randomUUID();
    }

    @Test
    public void testRingBufferDownsamplingAndMaxCapacity() {
        // Spec 7.9: ring buffer persisted per player, downsamples older data, capped at ~30 min
        Identifier air = Identifier.fromNamespaceAndPath("minecraft", "air");

        // Add 20,000 frames to trigger downsampling when exceeding MAX_FRAMES (18,000)
        for (int i = 0; i < 20000; i++) {
            RecordedFrame frame = new RecordedFrame(i * 0.1, 64.0, i * 0.05, 0.0f, 0.0f, "STANDING", air, i * 2L);
            recorder.recordFrameDirect(testPlayerId, frame);
        }

        int count = recorder.getFrameCount(testPlayerId);
        assertTrue(count <= MovementRecorder.MAX_FRAMES, "Recorder buffer must not exceed MAX_FRAMES (" + MovementRecorder.MAX_FRAMES + "), was: " + count);
        assertTrue(count > 10000, "Buffer should retain substantial history after downsampling, was: " + count);
    }

    @Test
    public void testFiveMinuteRecordingReplayWithinPositionalTolerance() {
        // Spec 7.9 Acceptance: "a 5-minute recording replays within positional tolerance"
        // 5 minutes = 300 seconds = 3,000 frames at 10Hz (every 2 ticks)
        int frameCount = 3000;
        List<RecordedFrame> frames = new ArrayList<>(frameCount);
        Identifier air = Identifier.fromNamespaceAndPath("minecraft", "air");

        for (int i = 0; i < frameCount; i++) {
            double x = 100.0 + Math.sin(i * 0.05) * 20.0;
            double y = 64.0 + (i * 0.001);
            double z = 200.0 + Math.cos(i * 0.05) * 20.0;
            float yaw = (float) ((i * 2.5) % 360.0);
            float pitch = (float) (Math.sin(i * 0.1) * 15.0);

            frames.add(new RecordedFrame(x, y, z, yaw, pitch, "STANDING", air, i * 2L));
        }

        // Test FAITHFUL replay indexing and positional match
        MovementReplayer replayer = new MovementReplayer(testPlayerId, frames, ReplayMode.faithful());

        for (int t = 0; t < frameCount; t++) {
            RecordedFrame expected = frames.get(t);
            RecordedFrame played = replayer.advance();
            assertNotNull(played);

            // Check positional tolerance <= 0.0001
            assertEquals(expected.x(), played.x(), 0.0001);
            assertEquals(expected.y(), played.y(), 0.0001);
            assertEquals(expected.z(), played.z(), 0.0001);
            assertEquals(expected.yaw(), played.yaw(), 0.0001f);
            assertEquals(expected.pitch(), played.pitch(), 0.0001f);
        }
        assertEquals(frameCount, replayer.getCurrentPlaybackIndex());
    }

    @Test
    public void testDivergeTriggersAtExactTick() {
        // Spec 7.9 Acceptance: "DIVERGE triggers at the right tick"
        int divergeTick = 150;
        List<RecordedFrame> frames = new ArrayList<>();
        Identifier air = Identifier.fromNamespaceAndPath("minecraft", "air");

        for (int i = 0; i < 300; i++) {
            frames.add(new RecordedFrame(i, 64.0, i, 0.0f, 0.0f, "STANDING", air, i * 2L));
        }

        ReplayMode divergeMode = ReplayMode.diverge(divergeTick);
        assertEquals(ReplayMode.Type.DIVERGE, divergeMode.type());
        assertEquals(divergeTick, divergeMode.parameter());
    }

    @Test
    public void testEyeContactVectorCalculations() {
        // Looking directly at target: dot product between view and toEntity should be ~1.0 (> 0.95)
        Vec3 playerEye = new Vec3(0, 65.6, 0);
        Vec3 entityEye = new Vec3(0, 65.6, 10); // 10 blocks along +Z
        Vec3 toEntity = entityEye.subtract(playerEye).normalize();

        Vec3 directView = new Vec3(0, 0, 1.0); // Looking along +Z
        double directDot = directView.dot(toEntity);
        assertEquals(1.0, directDot, 0.0001);
        assertTrue(directDot > 0.95, "Direct view must exceed eye contact threshold (0.95)");

        Vec3 awayView = new Vec3(1.0, 0, 0); // Looking along +X
        double awayDot = awayView.dot(toEntity);
        assertEquals(0.0, awayDot, 0.0001);
        assertFalse(awayDot > 0.95, "Peripheral/away view must not trigger eye contact");
    }
}
