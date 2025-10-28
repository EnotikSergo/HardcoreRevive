package com.enotiksergo.hardcorerevive;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClearRegionConfirm {
    private static final Set<RegistryKey<World>> CONFIRMED_WORLDS =
            ConcurrentHashMap.newKeySet();

    private ClearRegionConfirm() {}

    public static boolean isConfirmed(ServerWorld world) {
        return CONFIRMED_WORLDS.contains(world.getRegistryKey());
    }

    public static void confirm(ServerWorld world) {
        CONFIRMED_WORLDS.add(world.getRegistryKey());
    }

    public static void warnBackup(ServerCommandSource src) {
        src.sendError(Text.translatable("hardcorerevive.chat.warning"));
    }
}
