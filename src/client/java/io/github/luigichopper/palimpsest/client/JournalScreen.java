package io.github.luigichopper.palimpsest.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.luigichopper.palimpsest.journal.JournalEntry;
import io.github.luigichopper.palimpsest.journal.JournalManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class JournalScreen extends Screen {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private int selectedIndex = 0;
    private int scrollOffset = 0;

    public JournalScreen() {
        super(Component.literal("Palimpsest Journal"));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        List<JournalEntry> entries = JournalManager.CLIENT_ENTRIES;
        int key = event.key();

        if (key == InputConstants.KEY_ESCAPE) {
            this.onClose();
            return true;
        }

        if (key == InputConstants.KEY_UP && !entries.isEmpty()) {
            selectedIndex = Math.max(0, selectedIndex - 1);
            if (selectedIndex < scrollOffset) {
                scrollOffset = selectedIndex;
            }
            return true;
        }

        if (key == InputConstants.KEY_DOWN && !entries.isEmpty()) {
            selectedIndex = Math.min(entries.size() - 1, selectedIndex + 1);
            if (selectedIndex >= scrollOffset + 12) {
                scrollOffset = selectedIndex - 11;
            }
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<JournalEntry> entries = JournalManager.CLIENT_ENTRIES;
        if (scrollY > 0) {
            selectedIndex = Math.max(0, selectedIndex - 1);
            if (selectedIndex < scrollOffset) {
                scrollOffset = selectedIndex;
            }
        } else if (scrollY < 0 && !entries.isEmpty()) {
            selectedIndex = Math.min(entries.size() - 1, selectedIndex + 1);
            if (selectedIndex >= scrollOffset + 12) {
                scrollOffset = selectedIndex - 11;
            }
        }
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int marginX = Math.max(16, this.width / 12);
        int marginY = Math.max(16, this.height / 12);
        int boxW = this.width - (marginX * 2);
        int boxH = this.height - (marginY * 2);

        // 1. Dark phosphor CRT background
        extractor.fill(0, 0, this.width, this.height, 0x88000000);
        extractor.fill(marginX, marginY, marginX + boxW, marginY + boxH, 0xFA070F14);

        // Frame border
        extractor.fill(marginX - 1, marginY - 1, marginX + boxW + 1, marginY, 0xFF1B424D);
        extractor.fill(marginX - 1, marginY + boxH, marginX + boxW + 1, marginY + boxH + 1, 0xFF1B424D);
        extractor.fill(marginX - 1, marginY, marginX, marginY + boxH, 0xFF1B424D);
        extractor.fill(marginX + boxW, marginY, marginX + boxW + 1, marginY + boxH, 0xFF1B424D);

        // Header
        extractor.fill(marginX + 2, marginY + 2, marginX + boxW - 2, marginY + 20, 0xFF0E272E);
        extractor.text(this.font, Component.literal("PALIMPSEST FIELD JOURNAL // RECOVERED OBSERVATIONS"), marginX + 8, marginY + 6, 0xFF58D2E8);

        List<JournalEntry> entries = JournalManager.CLIENT_ENTRIES;
        int listWidth = Math.min(200, boxW / 3);

        // Divider
        extractor.fill(marginX + listWidth, marginY + 22, marginX + listWidth + 1, marginY + boxH - 2, 0xFF1B424D);

        // Left Pane: Entry List
        int startY = marginY + 28;
        if (entries.isEmpty()) {
            extractor.text(this.font, Component.literal("No entries discovered yet."), marginX + 8, startY, 0xFF7A8B99);
            extractor.text(this.font, Component.literal("Read books, terminals, and"), marginX + 8, startY + 14, 0xFF5A6B79);
            extractor.text(this.font, Component.literal("signs to collect notes."), marginX + 8, startY + 26, 0xFF5A6B79);
        } else {
            for (int i = 0; i < 12; i++) {
                int entryIdx = scrollOffset + i;
                if (entryIdx >= entries.size()) break;

                JournalEntry entry = entries.get(entryIdx);
                int itemY = startY + (i * 18);
                boolean isSelected = (entryIdx == selectedIndex);

                if (isSelected) {
                    extractor.fill(marginX + 4, itemY - 2, marginX + listWidth - 2, itemY + 14, 0xFF1A3D47);
                }

                String displayTitle = entry.title();
                if (displayTitle.length() > 18) {
                    displayTitle = displayTitle.substring(0, 16) + "..";
                }
                extractor.text(this.font, Component.literal(displayTitle), marginX + 8, itemY, isSelected ? 0xFFFFFFFF : 0xFF79B8C4);
            }
        }

        // Right Pane: Selected Entry Detail
        if (!entries.isEmpty() && selectedIndex >= 0 && selectedIndex < entries.size()) {
            JournalEntry selected = entries.get(selectedIndex);
            int detailX = marginX + listWidth + 12;
            int detailY = marginY + 28;

            extractor.text(this.font, Component.literal(selected.title()), detailX, detailY, 0xFF58D2E8);
            String dateStr = DATE_FORMAT.format(new Date(selected.timestamp()));
            extractor.text(this.font, Component.literal("Tag: [" + selected.tag() + "] | Logged: " + dateStr), detailX, detailY + 14, 0xFF5A7E8C);

            extractor.fill(detailX, detailY + 26, marginX + boxW - 12, detailY + 27, 0xFF1B424D);

            // Wrap content
            String[] lines = selected.content().split("\r?\n");
            int lineY = detailY + 34;
            for (String line : lines) {
                if (lineY > marginY + boxH - 20) break;
                extractor.text(this.font, Component.literal(line), detailX, lineY, 0xFFD2E6EB);
                lineY += 12;
            }
        }

        // Footer instructions
        extractor.text(this.font, Component.literal("[↑/↓] Navigate   [Click] Select   [Esc] Close"), marginX + 8, marginY + boxH - 14, 0xFF4A6875);
    }
}
