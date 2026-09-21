package com.enotiksergo.hardcorerevive.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ReviveCoordinator {

    private static final Set<UUID> WAITING = ConcurrentHashMap.newKeySet();
    private static final int REVIVE_RADIUS = 1;
    private static final Map<UUID, TicketInfo> BY_PLAYER = new ConcurrentHashMap<>();
    private static final Map<PosKey, AtomicInteger> COUNTS = new ConcurrentHashMap<>();
    public static void markWaiting(UUID id) { WAITING.add(id); }
    public static boolean consumeWaiting(UUID id) { return WAITING.remove(id); }

    public static void addPreload(ServerLevel world, ChunkPos pos, UUID id) {
        var prev = BY_PLAYER.put(id, new TicketInfo(world.dimension(), pos));
        if (prev != null) {
            removePreload(world.getServer(), id, prev);
        }

        var cm = world.getChunkSource();
        var key = new PosKey(world.dimension(), pos);
        int count = COUNTS.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
        if (count == 1) {
            cm.addTicketWithRadius(TicketType.FORCED, pos, REVIVE_RADIUS);
        }
    }

    public static void removePreload(MinecraftServer server, UUID id) {
        var info = BY_PLAYER.remove(id);
        if (info != null) removePreload(server, id, info);
    }

    private static void removePreload(MinecraftServer server, UUID id, TicketInfo info) {
        ServerLevel world = server.getLevel(info.worldKey());
        if (world == null) return;

        var key = new PosKey(info.worldKey(), info.pos());
        var counter = COUNTS.get(key);
        if (counter == null) return;

        int left = counter.decrementAndGet();
        if (left <= 0) {
            COUNTS.remove(key);
            world.getChunkSource().removeTicketWithRadius(TicketType.FORCED, info.pos(), REVIVE_RADIUS);
        }
    }

    private record TicketInfo(ResourceKey<Level> worldKey, ChunkPos pos) {}

    private record PosKey(ResourceKey<Level> worldKey, ChunkPos pos) {
    }

    private ReviveCoordinator() {}
}