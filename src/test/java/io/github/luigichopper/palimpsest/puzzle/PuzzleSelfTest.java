package io.github.luigichopper.palimpsest.puzzle;

import io.github.luigichopper.palimpsest.core.StoryState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PuzzleSelfTest {
    private PuzzleEngine engine;
    private StoryState storyState;
    private UUID testPlayerId;

    @BeforeEach
    void setUp() {
        engine = PuzzleEngine.get();
        engine.clear();
        storyState = new StoryState();
        testPlayerId = UUID.randomUUID();
    }

    @Test
    void testAnswerVerifierNormalizationRules() {
        // lowercase
        assertEquals("hello", AnswerVerifier.normalize("HELLO", List.of("lowercase")));

        // trim
        assertEquals("spaced", AnswerVerifier.normalize("  spaced  ", List.of("trim")));

        // collapse_ws
        assertEquals("a b c", AnswerVerifier.normalize("a    b   \t  c", List.of("collapse_ws")));

        // strip_punct
        assertEquals("hello world 123", AnswerVerifier.normalize("hello, world! #123?", List.of("strip_punct")));

        // digits_only
        assertEquals("42069", AnswerVerifier.normalize("Room 42-A, Code: 069!", List.of("digits_only")));

        // octal_to_decimal (e.g. 77 octal = 63 decimal)
        assertEquals("63", AnswerVerifier.normalize("077", List.of("octal_to_decimal")));

        // combined chain
        List<String> chain = List.of("lowercase", "trim", "collapse_ws", "strip_punct");
        assertEquals("palimpsest test 1", AnswerVerifier.normalize("  P-A-L-I-M-P-S-E-S-T   Test #1! ", chain));
    }

    @Test
    void testDirectSaltedHashVerification() {
        String salt = "ch0.lectern_order:pep98f";
        String rawAnswer = "  Epitaph!  ";
        List<String> rules = List.of("lowercase", "trim", "collapse_ws", "strip_punct");

        String normalized = AnswerVerifier.normalize(rawAnswer, rules);
        assertEquals("epitaph", normalized);

        String hash = AnswerVerifier.computeHash("SHA-256", salt, normalized);
        assertEquals("302fb2c7d62fffa604ec7018bbc4e2757ae0e15b3775ea84f4a8327c6350131c", hash);

        assertTrue(AnswerVerifier.verify(rawAnswer, rules, "SHA-256", salt, hash));
        assertFalse(AnswerVerifier.verify("wrong_answer", rules, "SHA-256", salt, hash));
    }

    @Test
    void testPuzzleEngineSubmitAndSolveFlow() {
        PuzzleDef puzzle = new PuzzleDef(
            "ch0.lectern_order",
            "ch0",
            "text_answer",
            "ch0.lectern_order.prompt",
            new PuzzleDef.VerifyDef("SHA-256", "ch0.lectern_order:pep98f", "302fb2c7d62fffa604ec7018bbc4e2757ae0e15b3775ea84f4a8327c6350131c"),
            List.of("lowercase", "trim", "collapse_ws", "strip_punct"),
            new PuzzleDef.OnSolveDef(List.of("ch0.terminal_unlocked", "ch0.logged_in_odette"), "ch0.lectern_order.solved"),
            List.of("hint1", "hint2"),
            false
        );
        engine.registerPuzzle(puzzle);

        // Incorrect attempt
        PuzzleEngine.PuzzleResult failResult = engine.submitAnswer(storyState, testPlayerId, "ch0.lectern_order", "wrong guess");
        assertFalse(failResult.solved());
        assertFalse(storyState.hasPlayerFlag(testPlayerId, "ch0.terminal_unlocked"));

        // Correct attempt
        PuzzleEngine.PuzzleResult winResult = engine.submitAnswer(storyState, testPlayerId, "ch0.lectern_order", "EPITAPH");
        assertTrue(winResult.solved());
        assertFalse(winResult.alreadySolved());
        assertEquals("ch0.lectern_order.solved", winResult.messageRef());
        assertTrue(storyState.hasWorldFlag("ch0.terminal_unlocked"));
        assertTrue(storyState.hasPlayerFlag(testPlayerId, "ch0.terminal_unlocked"));
        assertTrue(storyState.isPuzzleSolved("ch0.lectern_order"));

        // Idempotent second submission
        PuzzleEngine.PuzzleResult repeatResult = engine.submitAnswer(storyState, testPlayerId, "ch0.lectern_order", "EPITAPH");
        assertTrue(repeatResult.solved());
        assertTrue(repeatResult.alreadySolved());
    }

    @Test
    void testSolvabilityAgainstAnswersFile() throws IOException {
        Path answersPath = Paths.get("private/answers.md");
        if (!Files.exists(answersPath)) {
            // Optional if private folder not present in test environment
            return;
        }

        List<String> lines = Files.readAllLines(answersPath);
        Map<String, String> answers = new HashMap<>();
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("|") && !line.startsWith("| Puzzle ID") && !line.startsWith("|---")) {
                String[] parts = line.split("\\|");
                if (parts.length >= 4) {
                    String id = parts[1].trim();
                    String raw = parts[3].trim();
                    answers.put(id, raw);
                }
            }
        }

        assertFalse(answers.isEmpty(), "answers.md should contain puzzle entries");

        com.google.gson.Gson gson = new com.google.gson.Gson();
        Path puzzlesDir = Paths.get("src/main/resources/data/palimpsest/puzzles");
        if (Files.exists(puzzlesDir)) {
            try (var stream = Files.list(puzzlesDir)) {
                stream.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                    try {
                        String json = Files.readString(p);
                        PuzzleDef def = gson.fromJson(json, PuzzleDef.class);
                        engine.registerPuzzle(def);
                    } catch (Exception e) {
                        fail("Failed to parse " + p + ": " + e.getMessage());
                    }
                });
            }
        }

        for (Map.Entry<String, String> entry : answers.entrySet()) {
            String puzzleId = entry.getKey();
            String rawAnswer = entry.getValue();
            assertTrue(engine.getPuzzle(puzzleId).isPresent(), "Missing puzzle definition for: " + puzzleId);
            assertTrue(engine.checkAnswer(puzzleId, rawAnswer), "Puzzle answer failed verification for: " + puzzleId + " with raw answer: " + rawAnswer);
        }
    }
}
