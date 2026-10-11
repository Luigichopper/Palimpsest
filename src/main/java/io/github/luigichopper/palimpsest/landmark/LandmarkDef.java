package io.github.luigichopper.palimpsest.landmark;

import java.util.Optional;

public record LandmarkDef(
    String id,
    int anchorX,
    int anchorZ,
    String builder,
    Optional<String> requiresFlag,
    String note
) {}
