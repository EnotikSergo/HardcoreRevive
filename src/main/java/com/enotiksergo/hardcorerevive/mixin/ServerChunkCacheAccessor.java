package com.enotiksergo.hardcorerevive.mixin;

import com.enotiksergo.hardcorerevive.duck.ServerChunkCacheExt;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerChunkCache.class)
public interface ServerChunkCacheAccessor extends ServerChunkCacheExt {
    @Accessor("chunkMap")
    ChunkMap getChunkMap();
}
