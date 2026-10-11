package io.github.luigichopper.palimpsest.terminal;

import io.github.luigichopper.palimpsest.core.StoryState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TerminalFuzzTest {
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
    }

    @Test
    void testRateLimitingEnforcement() {
        // First 5 commands in a single second should pass
        for (int i = 0; i < 5; i++) {
            assertTrue(session.checkRateLimit(), "Command " + (i + 1) + " should pass rate limit");
        }
        // 6th command in the same second must be rejected
        assertFalse(session.checkRateLimit(), "6th command in 1-second window must be rejected");
    }

    @Test
    void testExcessiveLengthLine() {
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            huge.append("A");
        }
        String input = huge.toString();
        assertTrue(input.length() > 512);

        // TerminalSession / execute should safely handle without OutOfMemory or Exception
        assertDoesNotThrow(() -> {
            TerminalManager.TerminalResponse response = manager.execute(session, storyState, testPlayerId, input);
            assertNotNull(response);
            assertTrue(response.isError());
        });
    }

    @Test
    void testPathTraversalAttacksStaySandboxed() {
        String[] traversals = {
            "cat ../../../../../../../../../../windows/win.ini",
            "cat ../../../../../../../../../../etc/shadow",
            "cat /../../../../../../../../../../root",
            "cd /../../../../../../../../../../",
            "ls ../../../../../../../../../../../"
        };

        for (String attack : traversals) {
            assertDoesNotThrow(() -> {
                TerminalManager.TerminalResponse response = manager.execute(session, storyState, testPlayerId, attack);
                assertNotNull(response);
                // Working directory should never escape virtual root
                assertTrue(session.getCurrentDir().startsWith("/"), "Current dir must stay rooted");
            });
        }
    }

    @Test
    void testMalformedAndSpecialCharacterInputs() {
        String[] malformedInputs = {
            "",
            "   ",
            "\n\r\t",
            "\"unclosed quote test",
            "cat \"\"\"\"\"\"\"\"\"\"",
            "login 'single quotes' \"mixed\"",
            "help %s%s%s%s%x%x%n",
            "cat \u0000\u0001\u0002\u0003",
            "login odette \uD83D\uDD25\uD83D\uDD25\uD83D\uDC80",
            "; rm -rf / ; reboot ;",
            "cat /etc/passwd | grep root && echo pwned > /etc/pwned",
            "cd ////////////",
            "cd .",
            "cd ..",
            "diff \"\" \"\"",
            "checksum /dev/null",
            "mount ../../../secret.bak",
            "umount umount umount",
            "restore null",
            "format ; DROP TABLE users;"
        };

        for (String input : malformedInputs) {
            assertDoesNotThrow(() -> {
                TerminalManager.TerminalResponse response = manager.execute(session, storyState, testPlayerId, input);
                assertNotNull(response, "Response should never be null for input: " + input);
            }, "Fuzz test failed on input: " + input);
        }
    }

    @Test
    void testRandomStringFuzzing() {
        Random rng = new Random(42);
        char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 \t\n\"'\\/.-_!@#$%^&*()".toCharArray();

        for (int i = 0; i < 500; i++) {
            int len = rng.nextInt(100);
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < len; j++) {
                sb.append(chars[rng.nextInt(chars.length)]);
            }
            String fuzzInput = sb.toString();

            assertDoesNotThrow(() -> {
                TerminalManager.TerminalResponse resp = manager.execute(session, storyState, testPlayerId, fuzzInput);
                assertNotNull(resp);
            });
        }
    }
}
