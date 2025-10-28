package com.enotiksergo.hardcorerevive.util;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

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

    public static void addPreload(ServerWorld world, ChunkPos pos, UUID id) {
        var prev = BY_PLAYER.put(id, new TicketInfo(world.getRegistryKey(), pos));
        if (prev != null) {
            removePreload(world.getServer(), id, prev);
        }

        var cm = world.getChunkManager();
        var key = new PosKey(world.getRegistryKey(), pos);
        int count = COUNTS.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
        if (count == 1) {
            cm.addTicket(ChunkTicketType.FORCED, pos, REVIVE_RADIUS);
        }
    }

    public static void removePreload(MinecraftServer server, UUID id) {
        var info = BY_PLAYER.remove(id);
        if (info != null) removePreload(server, id, info);
    }

    private static void removePreload(MinecraftServer server, UUID id, TicketInfo info) {
        ServerWorld world = server.getWorld(info.worldKey());
        if (world == null) return;

        var key = new PosKey(info.worldKey(), info.pos());
        var counter = COUNTS.get(key);
        if (counter == null) return;

        int left = counter.decrementAndGet();
        if (left <= 0) {
            COUNTS.remove(key);
            world.getChunkManager().removeTicket(ChunkTicketType.FORCED, info.pos(), REVIVE_RADIUS);
        }
    }

    private record TicketInfo(RegistryKey<World> worldKey, ChunkPos pos) {}

    private record PosKey(RegistryKey<World> worldKey, ChunkPos pos) {
    }

    private ReviveCoordinator() {}
}