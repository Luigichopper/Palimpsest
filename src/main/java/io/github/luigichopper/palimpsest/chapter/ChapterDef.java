package io.github.luigichopper.palimpsest.chapter;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;

public record ChapterDef(
    String id,
    int order,
    String name,
    List<String> requires
) {
    public List<String> getSafeRequires() {
        return requires != null ? requires : Collections.emptyList();
    }
}
