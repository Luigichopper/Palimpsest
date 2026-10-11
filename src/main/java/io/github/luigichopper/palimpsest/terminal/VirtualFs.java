package io.github.luigichopper.palimpsest.terminal;

import io.github.luigichopper.palimpsest.core.StoryState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VirtualFs {
    private final Map<String, VirtualFile> files = new ConcurrentHashMap<>();

    public VirtualFs() {
        populateDefaults();
    }

    public static String normalizePath(String currentDir, String input) {
        if (input == null || input.trim().isEmpty()) {
            return currentDir != null && !currentDir.isEmpty() ? currentDir : "/";
        }

        String raw = input.trim();
        String combined;
        if (raw.startsWith("/")) {
            combined = raw;
        } else {
            String base = (currentDir == null || currentDir.isEmpty() || "/".equals(currentDir)) ? "" : currentDir;
            combined = base + "/" + raw;
        }

        String[] parts = combined.split("/+");
        Deque<String> stack = new ArrayDeque<>();
        for (String part : parts) {
            if (part.isEmpty() || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                if (!stack.isEmpty()) {
                    stack.removeLast();
                }
            } else {
                stack.addLast(part);
            }
        }

        if (stack.isEmpty()) {
            return "/";
        }

        StringBuilder sb = new StringBuilder();
        for (String segment : stack) {
            sb.append("/").append(segment);
        }
        return sb.toString();
    }

    public void addFile(VirtualFile file) {
        String normPath = normalizePath("/", file.getPath());
        files.put(normPath, file);
        ensureParentDirectories(normPath, file.getRequiresFlags(), file.getOwner());
    }

    public void removeFile(String path) {
        files.remove(normalizePath("/", path));
    }

    public Optional<VirtualFile> getFile(String path) {
        return Optional.ofNullable(files.get(normalizePath("/", path)));
    }

    public boolean hasAccess(VirtualFile file, StoryState storyState, UUID playerId) {
        if (file.getRequiresFlags().isEmpty()) {
            return true;
        }
        if (storyState == null) {
            return false;
        }
        for (String flag : file.getRequiresFlags()) {
            boolean has = storyState.hasWorldFlag(flag) || (playerId != null && storyState.hasPlayerFlag(playerId, flag));
            if (!has) {
                return false;
            }
        }
        return true;
    }

    public List<VirtualFile> listDirectory(String dirPath, StoryState storyState, UUID playerId) {
        String normDir = normalizePath("/", dirPath);
        if (!normDir.endsWith("/")) {
            normDir = normDir + "/";
        }

        List<VirtualFile> results = new ArrayList<>();
        Set<String> addedNames = new HashSet<>();

        for (Map.Entry<String, VirtualFile> entry : files.entrySet()) {
            String path = entry.getKey();
            if (path.equals(dirPath) || !path.startsWith(normDir)) {
                continue;
            }

            String relative = path.substring(normDir.length());
            int nextSlash = relative.indexOf('/');
            String childName = nextSlash >= 0 ? relative.substring(0, nextSlash) : relative;

            if (addedNames.add(childName)) {
                String childFullPath = normDir + childName;
                VirtualFile child = files.get(childFullPath);
                if (child != null && hasAccess(child, storyState, playerId)) {
                    results.add(child);
                } else if (child == null) {
                    // Implicit directory
                    results.add(VirtualFile.directory(childFullPath, Collections.emptyList(), "root"));
                }
            }
        }

        results.sort(Comparator.comparing(VirtualFile::getName));
        return results;
    }

    private void ensureParentDirectories(String path, List<String> requiresFlags, String owner) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash > 0) {
            String parent = path.substring(0, lastSlash);
            if (!files.containsKey(parent)) {
                files.put(parent, VirtualFile.directory(parent, requiresFlags, owner));
                ensureParentDirectories(parent, requiresFlags, owner);
            }
        }
    }

    public void populateDefaults() {
        addFile(VirtualFile.directory("/", Collections.emptyList(), "root"));
        addFile(VirtualFile.directory("/bin", Collections.emptyList(), "root"));
        addFile(VirtualFile.directory("/etc", Collections.emptyList(), "root"));
        addFile(VirtualFile.file("/etc/hostname", "archive-recovery-01\n", Collections.emptyList(), Collections.emptyList(), "root"));
        addFile(VirtualFile.file("/etc/motd",
            "====================================================\n" +
            " PALIMPSEST ARCHIVE RECOVERY SYSTEM (v2.6.3)\n" +
            " RESTRICTED ACCESS. SYSTEM AUDITING ENABLED.\n" +
            "====================================================\n",
            Collections.emptyList(), Collections.emptyList(), "root"));

        addFile(VirtualFile.directory("/home", Collections.emptyList(), "root"));
        addFile(VirtualFile.directory("/home/guest", Collections.emptyList(), "guest"));
        addFile(VirtualFile.file("/home/guest/readme.txt",
            "Welcome to the recovery console.\n" +
            "If you are looking for Odette's project files, authenticate with:\n" +
            "  login odette <password>\n" +
            "Type 'help' for shell command reference.\n",
            Collections.emptyList(), Collections.emptyList(), "guest"));

        // Odette's locked directory and files (requires ch0.logged_in_odette or ch0.terminal_unlocked)
        List<String> odetteFlags = List.of("ch0.terminal_unlocked");
        addFile(VirtualFile.directory("/home/odette", odetteFlags, "odette"));
        addFile(VirtualFile.file("/home/odette/notes.txt",
            "2026-10-02:\n" +
            "I managed to recover Wren's world save from the corrupted archive.\n" +
            "The terrain generated, but the structures are shifting. Something in the\n" +
            "mirror dimension is writing back into the anchor level.\n" +
            "I locked the Annex entrance at the coordinates recorded in archive.log.\n" +
            "The bunker door requires the 4-sherd pottery glyph sequence from my field journal.\n",
            odetteFlags, List.of("ch0.read_notes"), "odette"));

        addFile(VirtualFile.directory("/var", Collections.emptyList(), "root"));
        addFile(VirtualFile.directory("/var/log", Collections.emptyList(), "root"));
        addFile(VirtualFile.file("/var/log/archive.log",
            "[BOOT] Palimpsest kernel initialized.\n" +
            "[INFO] Anchor level mounted: palimpsest:anchor\n" +
            "[WARN] Sector 0x8849 checksum desync. Block mismatch in chunk [14, -8]\n" +
            "[INFO] Annex bunker entrance sealed at coordinates: X: 240, Y: 68, Z: -128\n" +
            "[WARN] Echo sequence divergence rate: 0.042%\n",
            Collections.emptyList(), Collections.emptyList(), "root"));

        addFile(VirtualFile.directory("/backups", Collections.emptyList(), "root"));
        addFile(VirtualFile.file("/backups/anchor_snapshot_01.bak",
            "ARCHIVE CONTAINER FORMAT v1\n" +
            "LEVEL: palimpsest:anchor\n" +
            "CHUNKS: 256\n" +
            "CHECKSUM: SHA256:d82e1fc08b479\n",
            Collections.emptyList(), Collections.emptyList(), "root"));
    }

    public void clear() {
        files.clear();
        populateDefaults();
    }
}
