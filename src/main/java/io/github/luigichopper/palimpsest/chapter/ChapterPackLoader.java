package io.github.luigichopper.palimpsest.chapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.luigichopper.palimpsest.Palimpsest;
import io.github.luigichopper.palimpsest.puzzle.PuzzleDef;
import io.github.luigichopper.palimpsest.puzzle.PuzzleEngine;
import io.github.luigichopper.palimpsest.terminal.TerminalManager;
import io.github.luigichopper.palimpsest.terminal.VirtualFile;
import io.github.luigichopper.palimpsest.terminal.VirtualFs;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.*;

public class ChapterPackLoader extends SimplePreparableReloadListener<ChapterPackLoader.ParsedPackData> implements IdentifiableResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("Palimpsest/ChapterPackLoader");
    private static final Identifier FABRIC_ID = Identifier.fromNamespaceAndPath(Palimpsest.MOD_ID, "chapter_pack_loader");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public record ParsedPackData(
        Map<String, ChapterDef> chapters,
        List<PuzzleDef> puzzles,
        List<VirtualFile> virtualFiles
    ) {}

    @Override
    public Identifier getFabricId() {
        return FABRIC_ID;
    }

    @Override
    protected ParsedPackData prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Map<String, ChapterDef> chapters = new HashMap<>();
        List<PuzzleDef> puzzles = new ArrayList<>();
        List<VirtualFile> virtualFiles = new ArrayList<>();

        // 1. Load chapters: chapters/<id>/chapter.json
        Map<Identifier, Resource> chapterResources = resourceManager.listResources(
            "chapters",
            id -> id.getNamespace().equals(Palimpsest.MOD_ID) && id.getPath().endsWith("chapter.json")
        );

        for (Map.Entry<Identifier, Resource> entry : chapterResources.entrySet()) {
            Identifier id = entry.getKey();
            try (Reader reader = entry.getValue().openAsReader()) {
                ChapterDef def = GSON.fromJson(reader, ChapterDef.class);
                if (def != null && def.id() != null) {
                    chapters.put(def.id(), def);
                    LOGGER.info("Loaded chapter definition '{}' from {}", def.id(), id);
                } else {
                    LOGGER.warn("Malformed chapter file (missing id): {}", id);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load chapter definition from {}: {}", id, e.getMessage());
            }
        }

        // 2. Load puzzles: puzzles/*.json and chapters/*/puzzles/*.json
        Map<Identifier, Resource> puzzleResources = resourceManager.listResources(
            "puzzles",
            id -> id.getNamespace().equals(Palimpsest.MOD_ID) && id.getPath().endsWith(".json")
        );

        for (Map.Entry<Identifier, Resource> entry : puzzleResources.entrySet()) {
            Identifier id = entry.getKey();
            try (Reader reader = entry.getValue().openAsReader()) {
                PuzzleDef def = GSON.fromJson(reader, PuzzleDef.class);
                if (def != null && def.id() != null) {
                    puzzles.add(def);
                    LOGGER.debug("Loaded puzzle '{}' from {}", def.id(), id);
                } else {
                    LOGGER.warn("Malformed puzzle file (missing id): {}", id);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load puzzle from {}: {}", id, e.getMessage());
            }
        }

        // 3. Load VirtualFs files: vfs/*.json and chapters/*/vfs/*.json
        Map<Identifier, Resource> vfsResources = resourceManager.listResources(
            "vfs",
            id -> id.getNamespace().equals(Palimpsest.MOD_ID) && id.getPath().endsWith(".json")
        );

        for (Map.Entry<Identifier, Resource> entry : vfsResources.entrySet()) {
            Identifier id = entry.getKey();
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                String path = json.has("path") ? json.get("path").getAsString() : null;
                if (path != null) {
                    boolean isDir = json.has("is_directory") && json.get("is_directory").getAsBoolean();
                    String content = json.has("content") ? json.get("content").getAsString() : "";
                    List<String> reqFlags = new ArrayList<>();
                    if (json.has("requires_flags")) {
                        json.getAsJsonArray("requires_flags").forEach(el -> reqFlags.add(el.getAsString()));
                    }
                    List<String> onReadFlags = new ArrayList<>();
                    if (json.has("on_read_flags")) {
                        json.getAsJsonArray("on_read_flags").forEach(el -> onReadFlags.add(el.getAsString()));
                    }
                    String owner = json.has("owner") ? json.get("owner").getAsString() : "root";
                    String perms = json.has("permissions") ? json.get("permissions").getAsString() : (isDir ? "drwxr-xr-x" : "-rw-r--r--");

                    virtualFiles.add(new VirtualFile(path, isDir, content, reqFlags, onReadFlags, owner, perms));
                    LOGGER.debug("Loaded virtual file '{}' from {}", path, id);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load virtual file definition from {}: {}", id, e.getMessage());
            }
        }

        return new ParsedPackData(chapters, puzzles, virtualFiles);
    }

    @Override
    protected void apply(ParsedPackData data, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        ChapterManager.get().setLoadedData(data.chapters(), data.puzzles(), data.virtualFiles());

        // Register puzzles into PuzzleEngine
        PuzzleEngine engine = PuzzleEngine.get();
        for (PuzzleDef puzzle : data.puzzles()) {
            engine.registerPuzzle(puzzle);
        }

        // Register virtual files into VirtualFs
        VirtualFs fs = TerminalManager.getVirtualFs();
        for (VirtualFile file : data.virtualFiles()) {
            fs.addFile(file);
        }

        LOGGER.info("ChapterPackLoader applied successfully: {} chapters, {} puzzles registered.",
            data.chapters().size(), data.puzzles().size());
    }
}
