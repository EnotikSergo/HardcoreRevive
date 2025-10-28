package com.enotiksergo.hardcorerevive.mixin;

import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelProperties.class)
public interface LevelPropertiesAccessor {
    @Accessor("levelInfo")
    LevelInfo hardcorerevive$getLevelInfo_();

    @Mutable
    @Accessor("levelInfo")
    void hardcorerevive$setLevelInfo_(LevelInfo info);
}