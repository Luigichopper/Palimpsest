package io.github.luigichopper.palimpsest.terminal;

import io.github.luigichopper.palimpsest.core.StoryState;
import io.github.luigichopper.palimpsest.net.TerminalOutputPayload;
import io.github.luigichopper.palimpsest.puzzle.AnswerVerifier;
import io.github.luigichopper.palimpsest.puzzle.PuzzleEngine;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TerminalManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Palimpsest/TerminalManager");
    private static final TerminalManager INSTANCE = new TerminalManager();

    private final VirtualFs virtualFs = new VirtualFs();
    private final Map<UUID, TerminalSession> sessions = new ConcurrentHashMap<>();

    public static TerminalManager get() {
        return INSTANCE;
    }

    public static VirtualFs getVirtualFs() {
        return INSTANCE.virtualFs;
    }

    public TerminalSession getSession(UUID playerId) {
        return sessions.computeIfAbsent(playerId, TerminalSession::new);
    }

    public void handleCommand(ServerPlayer player, BlockPos pos, String rawLine) {
        UUID playerId = player.getUUID();
        TerminalSession session = getSession(playerId);

        // 1. Length cap: 512 characters
        if (rawLine != null && rawLine.length() > 512) {
            ServerPlayNetworking.send(player, new TerminalOutputPayload(
                pos,
                "palimpsest-sh: input line exceeds maximum length of 512 characters\n",
                true,
                false
            ));
            return;
        }

        // 2. Rate limit: 5 commands per second
        if (!session.checkRateLimit()) {
            ServerPlayNetworking.send(player, new TerminalOutputPayload(
                pos,
                "palimpsest-sh: rate limit exceeded; please wait a moment\n",
                true,
                false
            ));
            return;
        }

        StoryState storyState = StoryState.get(player.level());
        TerminalResponse response = execute(session, storyState, playerId, rawLine);

        ServerPlayNetworking.send(player, new TerminalOutputPayload(
            pos,
            response.output(),
            response.isError(),
            response.clearScreen()
        ));
    }

    public record TerminalResponse(String output, boolean isError, boolean clearScreen) {}

    public TerminalResponse execute(TerminalSession session, StoryState storyState, UUID playerId, String rawLine) {
        if (rawLine == null || rawLine.trim().isEmpty()) {
            return new TerminalResponse("", false, false);
        }

        List<String> tokens = parseTokens(rawLine.trim());
        if (tokens.isEmpty()) {
            return new TerminalResponse("", false, false);
        }

        String command = tokens.get(0).toLowerCase();
        List<String> args = tokens.subList(1, tokens.size());

        switch (command) {
            case "help" -> {
                if (args.isEmpty()) {
                    return new TerminalResponse(
                        "Palimpsest Archive Shell (v2.6.3-recovery)\n" +
                        "Available commands:\n" +
                        "  help [topic]         - Display command information\n" +
                        "  whoami               - Print current user identity\n" +
                        "  pwd                  - Print current working directory\n" +
                        "  cd <path>            - Change directory\n" +
                        "  ls [path]            - List directory contents\n" +
                        "  cat <file>           - Display file content\n" +
                        "  login <user> <pass>  - Authenticate as user\n" +
                        "  logout               - Sign out back to guest\n" +
                        "  status               - Show system and integrity status\n" +
                        "  checksum <file>      - Compute SHA-256 integrity hash\n" +
                        "  mount <backup>       - Mount an archive backup\n" +
                        "  umount               - Unmount the active container\n" +
                        "  diff <a> <b>         - Compare two files\n" +
                        "  restore <backup>     - Restore archive image (restricted)\n" +
                        "  clear                - Clear terminal screen\n",
                        false, false
                    );
                } else {
                    return new TerminalResponse(getTopicHelp(args.get(0)), false, false);
                }
            }

            case "whoami" -> {
                return new TerminalResponse(session.getCurrentUser() + "\n", false, false);
            }

            case "pwd" -> {
                return new TerminalResponse(session.getCurrentDir() + "\n", false, false);
            }

            case "cd" -> {
                String target = args.isEmpty() ? "/home/" + session.getCurrentUser() : args.get(0);
                String norm = VirtualFs.normalizePath(session.getCurrentDir(), target);
                Optional<VirtualFile> fileOpt = virtualFs.getFile(norm);
                if (fileOpt.isEmpty()) {
                    return new TerminalResponse("cd: " + target + ": No such file or directory\n", true, false);
                }
                VirtualFile file = fileOpt.get();
                if (!file.isDirectory()) {
                    return new TerminalResponse("cd: " + target + ": Not a directory\n", true, false);
                }
                if (!virtualFs.hasAccess(file, storyState, playerId)) {
                    return new TerminalResponse("cd: " + target + ": Permission denied\n", true, false);
                }
                session.setCurrentDir(norm);
                return new TerminalResponse("", false, false);
            }

            case "ls" -> {
                String target = args.isEmpty() ? session.getCurrentDir() : args.get(0);
                String norm = VirtualFs.normalizePath(session.getCurrentDir(), target);
                Optional<VirtualFile> fileOpt = virtualFs.getFile(norm);
                if (fileOpt.isEmpty()) {
                    return new TerminalResponse("ls: cannot access '" + target + "': No such file or directory\n", true, false);
                }
                VirtualFile targetFile = fileOpt.get();
                if (!virtualFs.hasAccess(targetFile, storyState, playerId)) {
                    return new TerminalResponse("ls: cannot open directory '" + target + "': Permission denied\n", true, false);
                }
                if (!targetFile.isDirectory()) {
                    return new TerminalResponse(targetFile.getName() + "\n", false, false);
                }

                List<VirtualFile> children = virtualFs.listDirectory(norm, storyState, playerId);
                if (children.isEmpty()) {
                    return new TerminalResponse("", false, false);
                }

                StringBuilder sb = new StringBuilder();
                for (VirtualFile child : children) {
                    sb.append(String.format("%-10s  %-8s  %s%s\n",
                        child.getPermissions(),
                        child.getOwner(),
                        child.getName(),
                        child.isDirectory() ? "/" : ""
                    ));
                }
                return new TerminalResponse(sb.toString(), false, false);
            }

            case "cat" -> {
                if (args.isEmpty()) {
                    return new TerminalResponse("cat: missing file operand\n", true, false);
                }
                String target = args.get(0);
                String norm = VirtualFs.normalizePath(session.getCurrentDir(), target);
                Optional<VirtualFile> fileOpt = virtualFs.getFile(norm);
                if (fileOpt.isEmpty()) {
                    return new TerminalResponse("cat: " + target + ": No such file or directory\n", true, false);
                }
                VirtualFile file = fileOpt.get();
                if (file.isDirectory()) {
                    return new TerminalResponse("cat: " + target + ": Is a directory\n", true, false);
                }
                if (!virtualFs.hasAccess(file, storyState, playerId)) {
                    return new TerminalResponse("cat: " + target + ": Permission denied\n", true, false);
                }

                // Grant onRead flags
                if (storyState != null && !file.getOnReadFlags().isEmpty()) {
                    for (String flag : file.getOnReadFlags()) {
                        storyState.setWorldFlag(flag);
                        if (playerId != null) {
                            storyState.setPlayerFlag(playerId, flag);
                        }
                    }
                }

                return new TerminalResponse(file.getContent(), false, false);
            }

            case "login" -> {
                if (args.size() < 2) {
                    return new TerminalResponse("Usage: login <username> <password>\n", true, false);
                }
                String user = args.get(0);
                String pass = args.get(1);

                if ("odette".equalsIgnoreCase(user)) {
                    // Check against ch0.lectern_order puzzle via salted hash
                    boolean correct = PuzzleEngine.get().checkAnswer("ch0.lectern_order", pass);

                    if (correct) {
                        session.setCurrentUser("odette");
                        session.setCurrentDir("/home/odette");
                        if (storyState != null) {
                            storyState.setWorldFlag("ch0.terminal_unlocked");
                            storyState.setWorldFlag("ch0.logged_in_odette");
                            if (playerId != null) {
                                storyState.setPlayerFlag(playerId, "ch0.terminal_unlocked");
                                storyState.setPlayerFlag(playerId, "ch0.logged_in_odette");
                            }
                            storyState.markPuzzleSolved("ch0.lectern_order");
                        }
                        return new TerminalResponse(
                            "Authentication successful. Session elevated to 'odette'.\n" +
                            "Home directory: /home/odette\n",
                            false, false
                        );
                    } else {
                        return new TerminalResponse("login: authentication failure for user 'odette'\n", true, false);
                    }
                } else {
                    return new TerminalResponse("login: user '" + user + "' not found in shadow registry\n", true, false);
                }
            }

            case "logout" -> {
                session.setCurrentUser("guest");
                session.setCurrentDir("/home/guest");
                return new TerminalResponse("Logged out. Current session: guest\n", false, false);
            }

            case "status" -> {
                String mount = session.getMountedPath() != null ? session.getMountedPath() : "[none]";
                return new TerminalResponse(
                    "========================================\n" +
                    " ARCHIVE RECOVERY SUBSYSTEM STATUS\n" +
                    "========================================\n" +
                    " Node Identity : archive-recovery-01\n" +
                    " Status        : ACTIVE (DESYNC_DETECTED)\n" +
                    " Active User   : " + session.getCurrentUser() + "\n" +
                    " Working Dir   : " + session.getCurrentDir() + "\n" +
                    " Active Mount  : " + mount + "\n" +
                    " Mirror State  : OVERWRITE_ACTIVE (rate: 0.042%)\n" +
                    " Integrity     : 98.4% — Sector 0x8849 corrupted\n" +
                    " Clocks        : ServerClockInstance (DECOUPLED)\n" +
                    "========================================\n",
                    false, false
                );
            }

            case "checksum" -> {
                if (args.isEmpty()) {
                    return new TerminalResponse("checksum: missing file operand\n", true, false);
                }
                String target = args.get(0);
                String norm = VirtualFs.normalizePath(session.getCurrentDir(), target);
                Optional<VirtualFile> fileOpt = virtualFs.getFile(norm);
                if (fileOpt.isEmpty()) {
                    return new TerminalResponse("checksum: " + target + ": No such file or directory\n", true, false);
                }
                VirtualFile file = fileOpt.get();
                if (file.isDirectory()) {
                    return new TerminalResponse("checksum: " + target + ": Is a directory\n", true, false);
                }
                if (!virtualFs.hasAccess(file, storyState, playerId)) {
                    return new TerminalResponse("checksum: " + target + ": Permission denied\n", true, false);
                }

                String hash = AnswerVerifier.computeHash("SHA-256", "", file.getContent());
                return new TerminalResponse(hash + "  " + file.getName() + "\n", false, false);
            }

            case "mount" -> {
                if (args.isEmpty()) {
                    return new TerminalResponse("mount: missing backup path operand\n", true, false);
                }
                String target = args.get(0);
                String norm = VirtualFs.normalizePath(session.getCurrentDir(), target);
                Optional<VirtualFile> fileOpt = virtualFs.getFile(norm);
                if (fileOpt.isEmpty()) {
                    return new TerminalResponse("mount: " + target + ": No such file or directory\n", true, false);
                }
                session.setMountedPath(norm);
                return new TerminalResponse("Mounted '" + norm + "' to /mnt/recovery (read-only)\n", false, false);
            }

            case "umount" -> {
                if (session.getMountedPath() == null) {
                    return new TerminalResponse("umount: no container currently mounted\n", true, false);
                }
                String old = session.getMountedPath();
                session.setMountedPath(null);
                return new TerminalResponse("Unmounted container '" + old + "'.\n", false, false);
            }

            case "diff" -> {
                if (args.size() < 2) {
                    return new TerminalResponse("Usage: diff <file_a> <file_b>\n", true, false);
                }
                String pathA = VirtualFs.normalizePath(session.getCurrentDir(), args.get(0));
                String pathB = VirtualFs.normalizePath(session.getCurrentDir(), args.get(1));

                Optional<VirtualFile> optA = virtualFs.getFile(pathA);
                Optional<VirtualFile> optB = virtualFs.getFile(pathB);

                if (optA.isEmpty()) return new TerminalResponse("diff: " + args.get(0) + ": No such file\n", true, false);
                if (optB.isEmpty()) return new TerminalResponse("diff: " + args.get(1) + ": No such file\n", true, false);

                if (!virtualFs.hasAccess(optA.get(), storyState, playerId)) return new TerminalResponse("diff: " + args.get(0) + ": Permission denied\n", true, false);
                if (!virtualFs.hasAccess(optB.get(), storyState, playerId)) return new TerminalResponse("diff: " + args.get(1) + ": Permission denied\n", true, false);

                return new TerminalResponse(computeDiff(optA.get().getName(), optA.get().getContent(), optB.get().getName(), optB.get().getContent()), false, false);
            }

            case "restore" -> {
                if (storyState == null || (!storyState.hasWorldFlag("terminal.restore_authorized") && !storyState.hasWorldFlag("ending:can_restore"))) {
                    return new TerminalResponse("restore: operation restricted by system security policy (flag 'terminal.restore_authorized' missing)\n", true, false);
                }

                if ("RESTORE".equals(session.getPendingConfirmation())) {
                    storyState.setWorldFlag("ending:restore");
                    session.setPendingConfirmation(null);
                    return new TerminalResponse("Executing restore operation... Mirror levels evacuated and rollback completed.\n", false, false);
                } else {
                    session.setPendingConfirmation("RESTORE");
                    return new TerminalResponse("WARNING: This will roll back the Overwrite front and delete mirror levels.\nTo confirm, type: confirm restore\n", false, false);
                }
            }

            case "format" -> {
                // Hidden command
                if (storyState == null || (!storyState.hasWorldFlag("ending:can_format") && !storyState.hasWorldFlag("ch4.complete"))) {
                    return new TerminalResponse("palimpsest-sh: format: command not found\n", true, false);
                }

                if ("FORMAT".equals(session.getPendingConfirmation())) {
                    storyState.setWorldFlag("ending:format");
                    session.setPendingConfirmation(null);
                    return new TerminalResponse("Executing archive purge... All mod levels and data formatted.\nFinal site code: 0xDEADBEEF\n", false, false);
                } else {
                    session.setPendingConfirmation("FORMAT");
                    return new TerminalResponse("CRITICAL ACTION: This will purge all recovered levels.\nTo confirm, type: confirm format\n", false, false);
                }
            }

            case "confirm" -> {
                if (args.isEmpty()) {
                    return new TerminalResponse("confirm: missing action operand (e.g. 'confirm restore')\n", true, false);
                }
                String action = args.get(0).toUpperCase();
                if (action.equals(session.getPendingConfirmation())) {
                    if ("RESTORE".equals(action)) {
                        storyState.setWorldFlag("ending:restore");
                        session.setPendingConfirmation(null);
                        return new TerminalResponse("Restore confirmed and executed.\n", false, false);
                    } else if ("FORMAT".equals(action)) {
                        storyState.setWorldFlag("ending:format");
                        session.setPendingConfirmation(null);
                        return new TerminalResponse("Format confirmed and executed.\n", false, false);
                    }
                }
                return new TerminalResponse("confirm: no pending confirmation matching '" + action + "'\n", true, false);
            }

            case "clear" -> {
                return new TerminalResponse("", false, true);
            }

            default -> {
                return new TerminalResponse("palimpsest-sh: " + command + ": command not found\n", true, false);
            }
        }
    }

    private String getTopicHelp(String topic) {
        return switch (topic.toLowerCase()) {
            case "login" -> "login <user> <pass>: Authenticates as a user to access privileged files.\n";
            case "cat" -> "cat <file>: Prints the content of the specified file.\n";
            case "ls" -> "ls [path]: Lists entries in the specified directory.\n";
            case "cd" -> "cd <path>: Changes current working directory.\n";
            case "diff" -> "diff <a> <b>: Line-by-line comparison between two virtual files.\n";
            case "mount" -> "mount <backup>: Attaches a backup archive for examination.\n";
            case "restore" -> "restore <backup>: Initiates system rollback (requires security clearance).\n";
            case "checksum" -> "checksum <file>: Computes the SHA-256 integrity checksum.\n";
            default -> "help: no help entry available for topic '" + topic + "'\n";
        };
    }

    public static String computeDiff(String nameA, String contentA, String nameB, String contentB) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- ").append(nameA).append("\n");
        sb.append("+++ ").append(nameB).append("\n");

        String[] linesA = contentA.split("\r?\n");
        String[] linesB = contentB.split("\r?\n");

        int max = Math.max(linesA.length, linesB.length);
        boolean hasDiff = false;
        for (int i = 0; i < max; i++) {
            String a = i < linesA.length ? linesA[i] : null;
            String b = i < linesB.length ? linesB[i] : null;
            if (!Objects.equals(a, b)) {
                hasDiff = true;
                if (a != null) sb.append("- ").append(a).append("\n");
                if (b != null) sb.append("+ ").append(b).append("\n");
            }
        }

        if (!hasDiff) {
            return "Files " + nameA + " and " + nameB + " are identical.\n";
        }
        return sb.toString();
    }

    public static List<String> parseTokens(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens;
    }
}
