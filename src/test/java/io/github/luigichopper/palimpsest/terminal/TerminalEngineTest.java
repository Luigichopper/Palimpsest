package io.github.luigichopper.palimpsest.terminal;

import io.github.luigichopper.palimpsest.core.StoryState;
import io.github.luigichopper.palimpsest.puzzle.PuzzleDef;
import io.github.luigichopper.palimpsest.puzzle.PuzzleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TerminalEngineTest {
    private TerminalManager manager;
    private TerminalSession session;
    private StoryState storyState;
    private UUID testPlayerId;

    @BeforeEach
    void setUp() {
        manager = TerminalManager.get();
        testPlayerId = UUID.randomUUID();
        session = new TerminalSession(testPlayerId);
        storyState = new StoryState();

        // Register lectern puzzle for login testing
        PuzzleEngine.get().registerPuzzle(new PuzzleDef(
            "ch0.lectern_order",
            "ch0",
            "text_answer",
            "prompt",
            new PuzzleDef.VerifyDef("SHA-256", "ch0.lectern_order:pep98f", "302fb2c7d62fffa604ec7018bbc4e2757ae0e15b3775ea84f4a8327c6350131c"),
            List.of("lowercase", "trim", "collapse_ws", "strip_punct"),
            new PuzzleDef.OnSolveDef(List.of("ch0.terminal_unlocked"), "solved"),
            List.of(),
            false
        ));
    }

    @Test
    void testWhoAmIAndPwd() {
        TerminalManager.TerminalResponse who = manager.execute(session, storyState, testPlayerId, "whoami");
        assertEquals("guest\n", who.output());

        TerminalManager.TerminalResponse pwd = manager.execute(session, storyState, testPlayerId, "pwd");
        assertEquals("/home/guest\n", pwd.output());
    }

    @Test
    void testHelpCommand() {
        TerminalManager.TerminalResponse help = manager.execute(session, storyState, testPlayerId, "help");
        assertFalse(help.isError());
        assertTrue(help.output().contains("Available commands"));

        TerminalManager.TerminalResponse helpLs = manager.execute(session, storyState, testPlayerId, "help ls");
        assertTrue(helpLs.output().contains("ls [path]"));
    }

    @Test
    void testCdAndLsFlow() {
        TerminalManager.TerminalResponse cdEtc = manager.execute(session, storyState, testPlayerId, "cd /etc");
        assertFalse(cdEtc.isError());
        assertEquals("/etc", session.getCurrentDir());

        TerminalManager.TerminalResponse ls = manager.execute(session, storyState, testPlayerId, "ls");
        assertTrue(ls.output().contains("hostname"));
        assertTrue(ls.output().contains("motd"));

        // cd into non-existent
        TerminalManager.TerminalResponse cdFail = manager.execute(session, storyState, testPlayerId, "cd /ghost_folder");
        assertTrue(cdFail.isError());
        assertTrue(cdFail.output().contains("No such file or directory"));
    }

    @Test
    void testCatCommandAndOnReadFlags() {
        TerminalManager.TerminalResponse catMotd = manager.execute(session, storyState, testPlayerId, "cat /etc/motd");
        assertFalse(catMotd.isError());
        assertTrue(catMotd.output().contains("PALIMPSEST ARCHIVE RECOVERY SYSTEM"));

        // Trying to read locked notes as guest
        TerminalManager.TerminalResponse catNotesDenied = manager.execute(session, storyState, testPlayerId, "cat /home/odette/notes.txt");
        assertTrue(catNotesDenied.isError());
        assertTrue(catNotesDenied.output().contains("Permission denied"));

        // Unlock flag
        storyState.setWorldFlag("ch0.terminal_unlocked");
        TerminalManager.TerminalResponse catNotesAllowed = manager.execute(session, storyState, testPlayerId, "cat /home/odette/notes.txt");
        assertFalse(catNotesAllowed.isError());
        assertTrue(catNotesAllowed.output().contains("Wren's world save"));
        // Check onRead flag granted
        assertTrue(storyState.hasWorldFlag("ch0.read_notes"));
    }

    @Test
    void testLoginAndLogout() {
        // Failed login
        TerminalManager.TerminalResponse fail = manager.execute(session, storyState, testPlayerId, "login odette wrongpassword");
        assertTrue(fail.isError());
        assertTrue(fail.output().contains("authentication failure"));
        assertEquals("guest", session.getCurrentUser());

        // Successful login
        TerminalManager.TerminalResponse success = manager.execute(session, storyState, testPlayerId, "login odette EPITAPH");
        assertFalse(success.isError());
        assertTrue(success.output().contains("Session elevated to 'odette'"));
        assertEquals("odette", session.getCurrentUser());
        assertEquals("/home/odette", session.getCurrentDir());
        assertTrue(storyState.hasWorldFlag("ch0.terminal_unlocked"));

        // Logout
        TerminalManager.TerminalResponse logout = manager.execute(session, storyState, testPlayerId, "logout");
        assertFalse(logout.isError());
        assertEquals("guest", session.getCurrentUser());
        assertEquals("/home/guest", session.getCurrentDir());
    }

    @Test
    void testStatusAndChecksum() {
        TerminalManager.TerminalResponse status = manager.execute(session, storyState, testPlayerId, "status");
        assertFalse(status.isError());
        assertTrue(status.output().contains("ARCHIVE RECOVERY SUBSYSTEM STATUS"));

        TerminalManager.TerminalResponse checksum = manager.execute(session, storyState, testPlayerId, "checksum /etc/hostname");
        assertFalse(checksum.isError());
        assertTrue(checksum.output().contains("hostname"));
    }

    @Test
    void testMountAndUmount() {
        TerminalManager.TerminalResponse mount = manager.execute(session, storyState, testPlayerId, "mount /backups/anchor_snapshot_01.bak");
        assertFalse(mount.isError());
        assertTrue(mount.output().contains("Mounted '/backups/anchor_snapshot_01.bak'"));
        assertEquals("/backups/anchor_snapshot_01.bak", session.getMountedPath());

        TerminalManager.TerminalResponse umount = manager.execute(session, storyState, testPlayerId, "umount");
        assertFalse(umount.isError());
        assertNull(session.getMountedPath());
    }

    @Test
    void testDiffCommand() {
        TerminalManager.TerminalResponse diff = manager.execute(session, storyState, testPlayerId, "diff /etc/hostname /etc/motd");
        assertFalse(diff.isError());
        assertTrue(diff.output().contains("--- hostname"));
        assertTrue(diff.output().contains("+++ motd"));
    }

    @Test
    void testRestoreConfirmationWorkflow() {
        // Without clearance
        TerminalManager.TerminalResponse denied = manager.execute(session, storyState, testPlayerId, "restore /backups/01");
        assertTrue(denied.isError());
        assertTrue(denied.output().contains("restricted by system security policy"));

        // With clearance: first prompt warns and requests confirmation
        storyState.setWorldFlag("terminal.restore_authorized");
        TerminalManager.TerminalResponse prompt = manager.execute(session, storyState, testPlayerId, "restore /backups/01");
        assertFalse(prompt.isError());
        assertTrue(prompt.output().contains("confirm restore"));
        assertEquals("RESTORE", session.getPendingConfirmation());

        // Confirm
        TerminalManager.TerminalResponse confirmed = manager.execute(session, storyState, testPlayerId, "confirm restore");
        assertFalse(confirmed.isError());
        assertTrue(storyState.hasWorldFlag("ending:restore"));
        assertNull(session.getPendingConfirmation());
    }

    @Test
    void testHiddenFormatCommand() {
        // Without ARG completion, command is hidden
        TerminalManager.TerminalResponse notFound = manager.execute(session, storyState, testPlayerId, "format");
        assertTrue(notFound.isError());
        assertTrue(notFound.output().contains("command not found"));

        // With ARG completion flag
        storyState.setWorldFlag("ending:can_format");
        TerminalManager.TerminalResponse prompt = manager.execute(session, storyState, testPlayerId, "format");
        assertFalse(prompt.isError());
        assertTrue(prompt.output().contains("confirm format"));
        assertEquals("FORMAT", session.getPendingConfirmation());

        // Confirm
        TerminalManager.TerminalResponse confirmed = manager.execute(session, storyState, testPlayerId, "confirm format");
        assertFalse(confirmed.isError());
        assertTrue(storyState.hasWorldFlag("ending:format"));
    }

    @Test
    void testClearCommand() {
        TerminalManager.TerminalResponse clear = manager.execute(session, storyState, testPlayerId, "clear");
        assertTrue(clear.clearScreen());
    }
}
