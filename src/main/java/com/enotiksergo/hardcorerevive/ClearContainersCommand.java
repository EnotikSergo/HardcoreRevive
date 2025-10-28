package com.enotiksergo.hardcorerevive;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.function.IntSupplier;

public class ClearContainersCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("clearcontainers")
                .requires(source -> source.hasPermissionLevel(0))
                .executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    ServerWorld world = source.getWorld();

                    source.sendFeedback(() -> Text.translatable("hardcorerevive.chat.clear.start"), false);
                    ContainerCleaner.clearContainersInWorld(world, source);
                    return 1;
                }));
        dispatcher.register(CommandManager.literal("clearregion")
                .requires(source -> source.hasPermissionLevel(0))

                .executes(ctx -> guarded(ctx.getSource(),
                        () -> execClear(ctx.getSource(), true, Scope.CURRENT)))

                .then(CommandManager.literal("on")
                        .executes(ctx -> guarded(ctx.getSource(),
                                () -> execClear(ctx.getSource(), true, Scope.CURRENT)))
                        .then(CommandManager.literal("current")
                                .executes(ctx -> guarded(ctx.getSource(),
                                        () -> execClear(ctx.getSource(), true, Scope.CURRENT))))
                        .then(CommandManager.literal("all")
                                .executes(ctx -> guarded(ctx.getSource(),
                                        () -> execClear(ctx.getSource(), true, Scope.ALL))))
                )

                .then(CommandManager.literal("off")
                        .executes(ctx -> guarded(ctx.getSource(),
                                () -> execClear(ctx.getSource(), false, Scope.CURRENT)))
                        .then(CommandManager.literal("current")
                                .executes(ctx -> guarded(ctx.getSource(),
                                        () -> execClear(ctx.getSource(), false, Scope.CURRENT))))
                        .then(CommandManager.literal("all")
                                .executes(ctx -> guarded(ctx.getSource(),
                                        () -> execClear(ctx.getSource(), false, Scope.ALL))))
                )

                .then(CommandManager.literal("confirm")
                        .executes(ctx -> {
                            ServerCommandSource src = ctx.getSource();
                            var server = ctx.getSource().getServer();
                            for (ServerWorld w : server.getWorlds()) {
                                ClearRegionConfirm.confirm(w);
                            }
                            src.sendFeedback(() -> Text.translatable("hardcorerevive.chat.confirm"),true);
                            return 1;
                        })
                )
        );
    }
    private enum Scope { CURRENT, ALL }

    private static int execClear(ServerCommandSource source, boolean showBossbar, Scope scope) {
        source.sendFeedback(() -> Text.translatable("hardcorerevive.chat.clear.start"), false);

        if (scope == Scope.ALL) {
            RegionContainerCleaner.clearAllDimensions(source, showBossbar);
        } else {
            ServerWorld world = source.getWorld();
            RegionContainerCleaner.clearAllContainers(world, source, showBossbar);
        }
        return 1;
    }

    private static int guarded(ServerCommandSource src, IntSupplier action) {
        ServerWorld world = src.getWorld();
        if (!ClearRegionConfirm.isConfirmed(world)) {
            ClearRegionConfirm.warnBackup(src);
            return 0;
        }
        return action.getAsInt();
    }
}
