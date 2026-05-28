package com.enotiksergo.hardcorerevive.duck;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.level.ChunkHolder;

public interface ChunkMapExt {
    Long2ObjectLinkedOpenHashMap<ChunkHolder> getUpdatingChunkMap();
}
