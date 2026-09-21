package com.enotiksergo.hardcorerevive.mixin;

import com.enotiksergo.hardcorerevive.duck.LevelDataReviveExt;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PrimaryLevelData.class)
public interface PrimaryLevelDataAccessor extends LevelDataReviveExt {

    @Override
    @Accessor("settings")
    LevelSettings hardcorerevive$getLevelInfo_();

    @Override
    @Mutable
    @Accessor("settings")
    void hardcorerevive$setLevelInfo_(LevelSettings info);
}