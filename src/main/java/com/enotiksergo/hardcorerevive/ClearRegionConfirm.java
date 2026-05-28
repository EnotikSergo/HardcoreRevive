package com.enotiksergo.hardcorerevive;

import net.minecraft.resources.ResourceKey;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClearRegionConfirm {
    private static final Set<ResourceKey<Level>> CONFIRMED_WORLDS =
            ConcurrentHashMap.newKeySet();

    private ClearRegionConfirm() {}

    public static boolean isConfirmed(ServerLevel world) {
        return CONFIRMED_WORLDS.contains(world.dimension());
    }

    public static void confirm(ServerLevel world) {
        CONFIRMED_WORLDS.add(world.dimension());
    }

    public static void warnBackup(CommandSourceStack src) {
        src.sendFailure(Component.translatable("hardcorerevive.chat.warning"));
    }
}
