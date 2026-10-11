package io.github.luigichopper.palimpsest.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.luigichopper.palimpsest.config.PalimpsestConfig;
import io.github.luigichopper.palimpsest.core.StoryState;
import io.github.luigichopper.palimpsest.level.Evacuator;
import io.github.luigichopper.palimpsest.level.LevelKind;
import io.github.luigichopper.palimpsest.level.LevelManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.Level;
import xyz.nucleoid.fantasy.RuntimeLevelHandle;

import java.util.Collections;

public class DebugCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!PalimpsestConfig.get().devMode) {
            return;
        }

        dispatcher.register(
            Commands.literal("palimpsest")
                .then(Commands.literal("dev")
                    .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.literal("level")
                        .then(Commands.literal("list")
                            .executes(ctx -> {
                                LevelManager lm = LevelManager.get();
                                if (lm == null) {
                                    ctx.getSource().sendFailure(Component.literal("LevelManager not initialized"));
                                    return 0;
                                }
                                ctx.getSource().sendSuccess(() -> Component.literal("=== Active Runtime Levels ==="), false);
                                for (Identifier id : lm.getActiveHandles().keySet()) {
                                    ctx.getSource().sendSuccess(() -> Component.literal(" - " + id.toString()), false);
                                }
                                ctx.getSource().sendSuccess(() -> Component.literal("=== Persisted Levels in StoryState ==="), false);
                                StoryState state = StoryState.get(ctx.getSource().getServer());
                                for (Identifier id : state.getRegisteredLevels().keySet()) {
                                    ctx.getSource().sendSuccess(() -> Component.literal(" * " + id.toString()), false);
                                }
                                return 1;
                            })
                        )
                        .then(Commands.literal("open")
                            .then(Commands.argument("kind", StringArgumentType.word())
                                .executes(ctx -> {
                                    String kind = StringArgumentType.getString(ctx, "kind").toLowerCase();
                                    LevelManager lm = LevelManager.get();
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    RuntimeLevelHandle handle;

                                    switch (kind) {
                                        case "annex" -> handle = lm.openAnnex(player);
                                        case "echo" -> handle = lm.openEcho(player);
                                        case "mirror" -> handle = lm.ensureMirror();
                                        case "backup" -> handle = lm.openBackup("test");
                                        default -> {
                                            ctx.getSource().sendFailure(Component.literal("Unknown kind: " + kind + ". Use annex, echo, mirror, backup"));
                                            return 0;
                                        }
                                    }

                                    ctx.getSource().sendSuccess(
                                        () -> Component.literal("Opened " + kind + " level: " + handle.getRegistryKey().identifier()),
                                        true
                                    );
                                    return 1;
                                })
                            )
                        )
                        .then(Commands.literal("close")
                            .then(Commands.argument("id", StringArgumentType.string())
                                .executes(ctx -> {
                                    String idStr = StringArgumentType.getString(ctx, "id");
                                    Identifier id = Identifier.tryParse(idStr);
                                    if (id == null) {
                                        ctx.getSource().sendFailure(Component.literal("Invalid identifier: " + idStr));
                                        return 0;
                                    }
                                    LevelManager lm = LevelManager.get();
                                    RuntimeLevelHandle handle = lm.getHandle(id);
                                    if (handle != null) {
                                        lm.closeAndDelete(handle);
                                        ctx.getSource().sendSuccess(() -> Component.literal("Closed and deleted level " + id), true);
                                        return 1;
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("No active handle for " + id));
                                        return 0;
                                    }
                                })
                            )
                        )
                        .then(Commands.literal("tp")
                            .then(Commands.argument("id", StringArgumentType.string())
                                .executes(ctx -> {
                                    String idStr = StringArgumentType.getString(ctx, "id");
                                    Identifier id = Identifier.tryParse(idStr);
                                    if (id == null) {
                                        ctx.getSource().sendFailure(Component.literal("Invalid identifier: " + idStr));
                                        return 0;
                                    }
                                    ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
                                    ServerLevel level = ctx.getSource().getServer().getLevel(key);
                                    if (level == null) {
                                        ctx.getSource().sendFailure(Component.literal("Level not found: " + id));
                                        return 0;
                                    }
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    Evacuator.recordAnchorPos(player);
                                    player.teleportTo(level, 0.5, 65, 0.5, Collections.emptySet(), 0, 0, true);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Teleported to " + id), false);
                                    return 1;
                                })
                            )
                        )
                    )
                    .then(Commands.literal("director")
                        .then(Commands.literal("fire")
                            .then(Commands.argument("anomaly", StringArgumentType.word())
                                .executes(ctx -> {
                                    String anomalyId = StringArgumentType.getString(ctx, "anomaly");
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    boolean fired = io.github.luigichopper.palimpsest.director.AnomalyDirector.get().fireAnomalyDirectly(player, anomalyId);
                                    if (fired) {
                                        ctx.getSource().sendSuccess(() -> Component.literal("Fired anomaly: " + anomalyId), true);
                                        return 1;
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("Failed to fire anomaly (not found or precondition failed): " + anomalyId));
                                        return 0;
                                    }
                                })
                            )
                        )
                        .then(Commands.literal("tension")
                            .then(Commands.argument("amount", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0, 100))
                                .executes(ctx -> {
                                    float amt = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "amount");
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    io.github.luigichopper.palimpsest.director.AnomalyDirector.get().getTracker(player.getUUID()).setTension(amt);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Set player tension to " + amt), false);
                                    return 1;
                                })
                            )
                        )
                    )
                )
        );
    }
}
