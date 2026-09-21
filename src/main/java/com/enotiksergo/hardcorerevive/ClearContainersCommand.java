package com.enotiksergo.hardcorerevive;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;

import java.util.function.IntSupplier;

public class ClearContainersCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("clearcontainers")
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    ServerLevel world = source.getLevel();

                    source.sendSuccess(() -> Component.translatable("hardcorerevive.chat.clear.start"), false);
                    ContainerCleaner.clearContainersInWorld(world, source);
                    return 1;
                }));
        dispatcher.register(Commands.literal("clearregion")
                .executes(ctx -> guarded(ctx.getSource(),
                        () -> execClear(ctx.getSource(), true, Scope.CURRENT)))

                .then(Commands.literal("on")
                        .executes(ctx -> guarded(ctx.getSource(),
                                () -> execClear(ctx.getSource(), true, Scope.CURRENT)))
                        .then(Commands.literal("current")
                                .executes(ctx -> guarded(ctx.getSource(),
                                        () -> execClear(ctx.getSource(), true, Scope.CURRENT))))
                        .then(Commands.literal("all")
                                .executes(ctx -> guarded(ctx.getSource(),
                                        () -> execClear(ctx.getSource(), true, Scope.ALL))))
                )

                .then(Commands.literal("off")
                        .executes(ctx -> guarded(ctx.getSource(),
                                () -> execClear(ctx.getSource(), false, Scope.CURRENT)))
                        .then(Commands.literal("current")
                                .executes(ctx -> guarded(ctx.getSource(),
                                        () -> execClear(ctx.getSource(), false, Scope.CURRENT))))
                        .then(Commands.literal("all")
                                .executes(ctx -> guarded(ctx.getSource(),
                                        () -> execClear(ctx.getSource(), false, Scope.ALL))))
                )

                .then(Commands.literal("confirm")
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            var server = ctx.getSource().getServer();
                            for (ServerLevel w : server.getAllLevels()) {
                                ClearRegionConfirm.confirm(w);
                            }
                            src.sendSuccess(() -> Component.translatable("hardcorerevive.chat.confirm"),true);
                            return 1;
                        })
                )
        );
    }
    private enum Scope { CURRENT, ALL }

    private static int execClear(CommandSourceStack source, boolean showBossbar, Scope scope) {
        source.sendSuccess(() -> Component.translatable("hardcorerevive.chat.clear.start"), false);

        if (scope == Scope.ALL) {
            RegionContainerCleaner.clearAllDimensions(source, showBossbar);
        } else {
            ServerLevel world = source.getLevel();
            RegionContainerCleaner.clearAllContainers(world, source, showBossbar);
        }
        return 1;
    }

    private static int guarded(CommandSourceStack src, IntSupplier action) {
        ServerLevel world = src.getLevel();
        if (!ClearRegionConfirm.isConfirmed(world)) {
            ClearRegionConfirm.warnBackup(src);
            return 0;
        }
        return action.getAsInt();
    }
}
