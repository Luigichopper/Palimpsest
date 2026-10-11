package io.github.luigichopper.palimpsest.terminal;

import java.util.Collections;
import java.util.List;

public class VirtualFile {
    private final String path;
    private final boolean isDirectory;
    private final String content;
    private final List<String> requiresFlags;
    private final List<String> onReadFlags;
    private final String owner;
    private final String permissions;

    public VirtualFile(
        String path,
        boolean isDirectory,
        String content,
        List<String> requiresFlags,
        List<String> onReadFlags,
        String owner,
        String permissions
    ) {
        this.path = path.startsWith("/") ? path : "/" + path;
        this.isDirectory = isDirectory;
        this.content = content != null ? content : "";
        this.requiresFlags = requiresFlags != null ? Collections.unmodifiableList(requiresFlags) : Collections.emptyList();
        this.onReadFlags = onReadFlags != null ? Collections.unmodifiableList(onReadFlags) : Collections.emptyList();
        this.owner = owner != null ? owner : "root";
        this.permissions = permissions != null ? permissions : (isDirectory ? "drwxr-xr-x" : "-rw-r--r--");
    }

    public static VirtualFile directory(String path, List<String> requiresFlags, String owner) {
        return new VirtualFile(path, true, "", requiresFlags, Collections.emptyList(), owner, "drwxr-xr-x");
    }

    public static VirtualFile file(String path, String content, List<String> requiresFlags, List<String> onReadFlags, String owner) {
        return new VirtualFile(path, false, content, requiresFlags, onReadFlags, owner, "-rw-r--r--");
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        if ("/".equals(path)) {
            return "/";
        }
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public String getContent() {
        return content;
    }

    public List<String> getRequiresFlags() {
        return requiresFlags;
    }

    public List<String> getOnReadFlags() {
        return onReadFlags;
    }

    public String getOwner() {
        return owner;
    }

    public String getPermissions() {
        return permissions;
    }
}
