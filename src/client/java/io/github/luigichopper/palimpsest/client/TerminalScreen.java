package io.github.luigichopper.palimpsest.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.luigichopper.palimpsest.net.TerminalCommandPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

public class TerminalScreen extends Screen {
    private static final List<String> COMMAND_AUTOCOMPLETES = List.of(
        "help", "whoami", "pwd", "cd", "ls", "cat", "login", "logout",
        "status", "checksum", "mount", "umount", "diff", "restore", "clear"
    );

    private final BlockPos terminalPos;
    private final List<String> outputLines = new ArrayList<>();
    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;

    private String currentInput = "";
    private int cursorPosition = 0;
    private int scrollOffset = 0;
    private int blinkTicks = 0;

    public TerminalScreen(BlockPos terminalPos) {
        super(Component.translatable("palimpsest.terminal.title"));
        this.terminalPos = terminalPos;

        // Initial welcome lines
        outputLines.add("=================================================");
        outputLines.add("  PALIMPSEST ARCHIVE RECOVERY TERMINAL (v2.6.3)");
        outputLines.add("  Connection established to local recovery node.");
        outputLines.add("  Type 'help' for a list of available commands.");
        outputLines.add("=================================================");
        outputLines.add("");
    }

    public BlockPos getTerminalPos() {
        return terminalPos;
    }

    public void appendOutput(String output, boolean isError, boolean clearScreen) {
        if (clearScreen) {
            outputLines.clear();
        }
        if (output != null && !output.isEmpty()) {
            String[] lines = output.split("\r?\n");
            for (String line : lines) {
                outputLines.add(line);
            }
            while (outputLines.size() > 500) {
                outputLines.remove(0);
            }
            // Auto scroll to bottom
            scrollOffset = 0;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        blinkTicks++;
    }

    private void playTypingSound() {
        if (this.minecraft != null) {
            float pitch = 1.2f + (float) (Math.random() * 0.4f);
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        if (key == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (key == InputConstants.KEY_RETURN) {
            playTypingSound();
            String commandToSubmit = currentInput.trim();
            outputLines.add("> " + currentInput);
            if (!commandToSubmit.isEmpty()) {
                history.add(currentInput);
                historyIndex = -1;
                ClientPlayNetworking.send(new TerminalCommandPayload(terminalPos, currentInput));
            }
            currentInput = "";
            cursorPosition = 0;
            scrollOffset = 0;
            return true;
        }

        if (key == InputConstants.KEY_BACKSPACE) {
            if (cursorPosition > 0 && !currentInput.isEmpty()) {
                playTypingSound();
                currentInput = currentInput.substring(0, cursorPosition - 1) + currentInput.substring(cursorPosition);
                cursorPosition--;
            }
            return true;
        }

        if (key == InputConstants.KEY_UP) {
            if (!history.isEmpty()) {
                if (historyIndex == -1) {
                    historyIndex = history.size() - 1;
                } else if (historyIndex > 0) {
                    historyIndex--;
                }
                currentInput = history.get(historyIndex);
                cursorPosition = currentInput.length();
            }
            return true;
        }

        if (key == InputConstants.KEY_DOWN) {
            if (historyIndex != -1) {
                if (historyIndex < history.size() - 1) {
                    historyIndex++;
                    currentInput = history.get(historyIndex);
                } else {
                    historyIndex = -1;
                    currentInput = "";
                }
                cursorPosition = currentInput.length();
            }
            return true;
        }

        if (key == InputConstants.KEY_LEFT) {
            if (cursorPosition > 0) {
                cursorPosition--;
            }
            return true;
        }

        if (key == InputConstants.KEY_RIGHT) {
            if (cursorPosition < currentInput.length()) {
                cursorPosition++;
            }
            return true;
        }

        if (key == InputConstants.KEY_TAB) {
            triggerTabCompletion();
            return true;
        }

        return super.keyPressed(event);
    }

    private void triggerTabCompletion() {
        if (currentInput.isEmpty()) {
            return;
        }
        int lastSpace = currentInput.lastIndexOf(' ');
        String prefix = lastSpace >= 0 ? currentInput.substring(lastSpace + 1) : currentInput;
        if (prefix.isEmpty()) {
            return;
        }

        for (String candidate : COMMAND_AUTOCOMPLETES) {
            if (candidate.startsWith(prefix.toLowerCase()) && !candidate.equalsIgnoreCase(prefix)) {
                String completed = candidate + " ";
                if (lastSpace >= 0) {
                    currentInput = currentInput.substring(0, lastSpace + 1) + completed;
                } else {
                    currentInput = completed;
                }
                cursorPosition = currentInput.length();
                playTypingSound();
                break;
            }
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (event.isAllowedChatCharacter()) {
            String s = event.codepointAsString();
            currentInput = currentInput.substring(0, cursorPosition) + s + currentInput.substring(cursorPosition);
            cursorPosition += s.length();
            playTypingSound();
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) {
            scrollOffset = Math.min(scrollOffset + 2, Math.max(0, outputLines.size() - 5));
        } else if (scrollY < 0) {
            scrollOffset = Math.max(0, scrollOffset - 2);
        }
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int marginX = Math.max(16, this.width / 12);
        int marginY = Math.max(16, this.height / 12);
        int boxW = this.width - (marginX * 2);
        int boxH = this.height - (marginY * 2);

        // 1. CRT Outer Background and Bezels
        extractor.fill(0, 0, this.width, this.height, 0x88000000);
        extractor.fill(marginX, marginY, marginX + boxW, marginY + boxH, 0xFA050E06);

        // CRT border phosphor frame
        extractor.fill(marginX - 1, marginY - 1, marginX + boxW + 1, marginY, 0xFF145214);
        extractor.fill(marginX - 1, marginY + boxH, marginX + boxW + 1, marginY + boxH + 1, 0xFF145214);
        extractor.fill(marginX - 1, marginY, marginX, marginY + boxH, 0xFF145214);
        extractor.fill(marginX + boxW, marginY, marginX + boxW + 1, marginY + boxH, 0xFF145214);

        // Header bar
        extractor.fill(marginX + 2, marginY + 2, marginX + boxW - 2, marginY + 16, 0xFF0A240A);
        extractor.text(this.font, Component.literal("PALIMPSEST RECOVERY NODE // RECOVERY SHELL"), marginX + 6, marginY + 5, 0xFF44FF66);
        extractor.fill(marginX + 2, marginY + 16, marginX + boxW - 2, marginY + 17, 0xFF1B6B22);

        // Content Area
        int contentTop = marginY + 22;
        int inputBottom = marginY + boxH - 16;
        int lineHeight = this.font.lineHeight + 2;
        int maxVisibleLines = Math.max(1, (inputBottom - contentTop) / lineHeight);

        int totalLines = outputLines.size();
        int endIndex = Math.max(0, totalLines - scrollOffset);
        int startIndex = Math.max(0, endIndex - maxVisibleLines);

        int currentY = contentTop;
        for (int i = startIndex; i < endIndex; i++) {
            String line = outputLines.get(i);
            int color = 0xFF33EE44;
            if (line.startsWith("[WARN]") || line.startsWith("WARNING:")) {
                color = 0xFFFFCC22;
            } else if (line.startsWith("Error") || line.contains("failed") || line.contains("command not found") || line.contains("Permission denied")) {
                color = 0xFFFF4444;
            } else if (line.startsWith("> ")) {
                color = 0xFF88FF99;
            }
            extractor.text(this.font, Component.literal(line), marginX + 6, currentY, color);
            currentY += lineHeight;
        }

        // Input Line Separator & Prompt
        extractor.fill(marginX + 2, inputBottom - 3, marginX + boxW - 2, inputBottom - 2, 0xFF145214);

        boolean showCursor = (blinkTicks / 10) % 2 == 0;
        String cursorChar = showCursor ? "_" : " ";
        String promptText = "> " + currentInput.substring(0, cursorPosition) + cursorChar + currentInput.substring(cursorPosition);
        extractor.text(this.font, Component.literal(promptText), marginX + 6, inputBottom + 2, 0xFF55FF77);
    }
}
