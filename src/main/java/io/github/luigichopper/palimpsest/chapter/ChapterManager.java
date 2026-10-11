package io.github.luigichopper.palimpsest.chapter;

import io.github.luigichopper.palimpsest.puzzle.PuzzleDef;
import io.github.luigichopper.palimpsest.terminal.VirtualFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChapterManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Palimpsest/ChapterManager");
    private static final ChapterManager INSTANCE = new ChapterManager();

    private final Map<String, ChapterDef> chapters = new ConcurrentHashMap<>();
    private final List<VirtualFile> chapterVirtualFiles = new ArrayList<>();
    private final List<PuzzleDef> chapterPuzzles = new ArrayList<>();

    public static ChapterManager get() {
        return INSTANCE;
    }

    public synchronized void setLoadedData(
        Map<String, ChapterDef> loadedChapters,
        List<PuzzleDef> loadedPuzzles,
        List<VirtualFile> loadedFiles
    ) {
        chapters.clear();
        chapters.putAll(loadedChapters);

        chapterPuzzles.clear();
        chapterPuzzles.addAll(loadedPuzzles);

        chapterVirtualFiles.clear();
        chapterVirtualFiles.addAll(loadedFiles);

        LOGGER.info("ChapterManager updated: {} chapters, {} puzzles, {} virtual files loaded.",
            chapters.size(), chapterPuzzles.size(), chapterVirtualFiles.size());
    }

    public Optional<ChapterDef> getChapter(String id) {
        return Optional.ofNullable(chapters.get(id));
    }

    public Collection<ChapterDef> getAllChapters() {
        return Collections.unmodifiableCollection(chapters.values());
    }

    public List<VirtualFile> getChapterVirtualFiles() {
        return Collections.unmodifiableList(chapterVirtualFiles);
    }

    public List<PuzzleDef> getChapterPuzzles() {
        return Collections.unmodifiableList(chapterPuzzles);
    }
}
