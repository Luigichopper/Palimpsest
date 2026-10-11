package io.github.luigichopper.palimpsest.puzzle;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;

public record PuzzleDef(
    String id,
    String chapter,
    String kind,
    @SerializedName("prompt_ref") String promptRef,
    VerifyDef verify,
    List<String> normalize,
    @SerializedName("on_solve") OnSolveDef onSolve,
    List<String> hints,
    boolean external
) {
    public record VerifyDef(
        String algo,
        String salt,
        String hash
    ) {}

    public record OnSolveDef(
        @SerializedName("grant_flags") List<String> grantFlags,
        @SerializedName("message_ref") String messageRef
    ) {
        public List<String> getSafeGrantFlags() {
            return grantFlags != null ? grantFlags : Collections.emptyList();
        }
    }

    public List<String> getSafeNormalize() {
        return normalize != null ? normalize : Collections.emptyList();
    }

    public List<String> getSafeHints() {
        return hints != null ? hints : Collections.emptyList();
    }
}
