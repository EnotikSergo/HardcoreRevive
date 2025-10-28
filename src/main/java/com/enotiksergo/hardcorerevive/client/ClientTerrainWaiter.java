package com.enotiksergo.hardcorerevive.client;

import com.enotiksergo.hardcorerevive.net.ReviveNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.util.math.ChunkPos;

public final class ClientTerrainWaiter {
    private static boolean waiting = false;
    private static int graceTicks;
    private static int stableOkTicks;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!waiting) return;
            if (client.player == null || client.world == null) return;

            if (graceTicks > 0) { graceTicks--; return; }

            boolean screenReady = (client.currentScreen == null);

            ChunkPos pos = new ChunkPos(client.player.getBlockPos());
            boolean chunkReady;
            try {
                chunkReady = client.world.getChunkManager().isChunkLoaded(pos.x, pos.z);
            } catch (Throwable t) {
                chunkReady = false;
            }

            if (screenReady && chunkReady) {
                if (++stableOkTicks >= 3) {
                    ReviveNetworking.sendReady();
                    waiting = false;
                }
            } else {
                stableOkTicks = 0;
            }
        });
    }

    public static void startWaiting() {
        waiting = true;
        graceTicks = 10;
        stableOkTicks = 0;
    }

    private ClientTerrainWaiter() {}
}