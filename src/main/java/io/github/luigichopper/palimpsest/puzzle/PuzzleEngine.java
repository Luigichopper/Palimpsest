package io.github.luigichopper.palimpsest.puzzle;

import io.github.luigichopper.palimpsest.core.StoryState;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PuzzleEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger("Palimpsest/PuzzleEngine");
    private static final PuzzleEngine INSTANCE = new PuzzleEngine();

    private final Map<String, PuzzleDef> puzzles = new ConcurrentHashMap<>();

    public static PuzzleEngine get() {
        return INSTANCE;
    }

    public void registerPuzzle(PuzzleDef puzzle) {
        puzzles.put(puzzle.id(), puzzle);
        LOGGER.debug("Registered puzzle: {} (chapter: {}, kind: {})", puzzle.id(), puzzle.chapter(), puzzle.kind());
    }

    public Optional<PuzzleDef> getPuzzle(String id) {
        return Optional.ofNullable(puzzles.get(id));
    }

    public Collection<PuzzleDef> getAllPuzzles() {
        return Collections.unmodifiableCollection(puzzles.values());
    }

    public void clear() {
        puzzles.clear();
    }

    public boolean checkAnswer(String puzzleId, String rawInput) {
        PuzzleDef puzzle = puzzles.get(puzzleId);
        if (puzzle == null || puzzle.verify() == null) {
            return false;
        }

        PuzzleDef.VerifyDef verify = puzzle.verify();
        return AnswerVerifier.verify(
            rawInput,
            puzzle.getSafeNormalize(),
            verify.algo(),
            verify.salt(),
            verify.hash()
        );
    }

    public record PuzzleResult(boolean solved, boolean alreadySolved, String messageRef, List<String> grantedFlags) {}

    public PuzzleResult submitAnswer(ServerPlayer player, String puzzleId, String rawInput) {
        StoryState storyState = StoryState.get(player.level());
        return submitAnswer(storyState, player.getUUID(), puzzleId, rawInput);
    }

    public PuzzleResult submitAnswer(StoryState storyState, UUID playerId, String puzzleId, String rawInput) {
        PuzzleDef puzzle = puzzles.get(puzzleId);
        if (puzzle == null) {
            return new PuzzleResult(false, false, "puzzle.not_found", Collections.emptyList());
        }

        if (storyState.isPuzzleSolved(puzzleId)) {
            return new PuzzleResult(true, true, puzzle.onSolve() != null ? puzzle.onSolve().messageRef() : "puzzle.already_solved", Collections.emptyList());
        }

        boolean correct = checkAnswer(puzzleId, rawInput);
        if (!correct) {
            return new PuzzleResult(false, false, "puzzle.incorrect", Collections.emptyList());
        }

        // Correct answer!
        storyState.markPuzzleSolved(puzzleId);

        List<String> granted = new ArrayList<>();
        if (puzzle.onSolve() != null) {
            for (String flag : puzzle.onSolve().getSafeGrantFlags()) {
                storyState.setWorldFlag(flag);
                if (playerId != null) {
                    storyState.setPlayerFlag(playerId, flag);
                }
                granted.add(flag);
            }
        }

        LOGGER.info("Puzzle '{}' solved by player {}! Granted flags: {}", puzzleId, playerId, granted);
        String msg = puzzle.onSolve() != null ? puzzle.onSolve().messageRef() : "puzzle.solved";
        return new PuzzleResult(true, false, msg, granted);
    }
}
