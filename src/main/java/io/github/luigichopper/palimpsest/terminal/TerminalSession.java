package io.github.luigichopper.palimpsest.terminal;

import java.util.UUID;

public class TerminalSession {
    private final UUID playerId;
    private String currentUser = "guest";
    private String currentDir = "/home/guest";
    private String mountedPath = null;
    private String pendingConfirmation = null;

    // Rate limiting: 5 commands per second
    private long windowSecond = 0;
    private int commandsInWindow = 0;

    public TerminalSession(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
    }

    public String getCurrentDir() {
        return currentDir;
    }

    public void setCurrentDir(String currentDir) {
        this.currentDir = currentDir;
    }

    public String getMountedPath() {
        return mountedPath;
    }

    public void setMountedPath(String mountedPath) {
        this.mountedPath = mountedPath;
    }

    public String getPendingConfirmation() {
        return pendingConfirmation;
    }

    public void setPendingConfirmation(String pendingConfirmation) {
        this.pendingConfirmation = pendingConfirmation;
    }

    public synchronized boolean checkRateLimit() {
        long currentSec = System.currentTimeMillis() / 1000L;
        if (currentSec != windowSecond) {
            windowSecond = currentSec;
            commandsInWindow = 1;
            return true;
        }
        if (commandsInWindow >= 5) {
            return false;
        }
        commandsInWindow++;
        return true;
    }
}
