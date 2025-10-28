package com.enotiksergo.hardcorerevive.mixin;

import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerChunkManager.class)
public interface ServerChunkManagerAccessor {
    @Accessor("chunkLoadingManager")
    ServerChunkLoadingManager getChunkLoadingManager();
}
