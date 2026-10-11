package io.github.luigichopper.palimpsest.terminal;

import io.github.luigichopper.palimpsest.core.StoryState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class VirtualFsTest {
    private VirtualFs fs;
    private StoryState storyState;
    private UUID testPlayerId;

    @BeforeEach
    void setUp() {
        fs = new VirtualFs();
        storyState = new StoryState();
        testPlayerId = UUID.randomUUID();
    }

    @Test
    void testPathNormalization() {
        assertEquals("/", VirtualFs.normalizePath("/", ""));
        assertEquals("/", VirtualFs.normalizePath("/home/guest", "/"));
        assertEquals("/home/odette", VirtualFs.normalizePath("/home", "odette"));
        assertEquals("/home/odette", VirtualFs.normalizePath("/home/guest", "../odette"));
        assertEquals("/home/odette/notes.txt", VirtualFs.normalizePath("/home/odette", "./notes.txt"));
        assertEquals("/notes.txt", VirtualFs.normalizePath("/home/guest/deep", "../../../notes.txt"));
        // Multiple slashes
        assertEquals("/home/odette/notes.txt", VirtualFs.normalizePath("/", "///home///odette///notes.txt"));
        // Attempting to break out of root
        assertEquals("/etc/passwd", VirtualFs.normalizePath("/", "../../../../../../etc/passwd"));
    }

    @Test
    void testPermissionAndFlagGating() {
        // Guest should not see odette's private notes initially
        Optional<VirtualFile> notesOpt = fs.getFile("/home/odette/notes.txt");
        assertTrue(notesOpt.isPresent());
        VirtualFile notes = notesOpt.get();

        assertFalse(fs.hasAccess(notes, storyState, testPlayerId));

        // Unlock flag
        storyState.setWorldFlag("ch0.terminal_unlocked");
        assertTrue(fs.hasAccess(notes, storyState, testPlayerId));
    }

    @Test
    void testDirectoryListingFiltersUnauthorized() {
        // Without flag, /home listing should only include guest
        List<VirtualFile> before = fs.listDirectory("/home", storyState, testPlayerId);
        assertTrue(before.stream().anyMatch(f -> f.getName().equals("guest")));
        assertFalse(before.stream().anyMatch(f -> f.getName().equals("odette")));

        // Grant flag
        storyState.setWorldFlag("ch0.terminal_unlocked");
        List<VirtualFile> after = fs.listDirectory("/home", storyState, testPlayerId);
        assertTrue(after.stream().anyMatch(f -> f.getName().equals("odette")));
    }
}
