package com.enotiksergo.hardcorerevive.mixin;

import com.enotiksergo.hardcorerevive.duck.ClientWorldHardcoreDuck;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientWorld.Properties.class)
public abstract class ClientWorldPropertiesMixin implements ClientWorldHardcoreDuck {
    @Shadow @Final @Mutable private boolean hardcore;

    @Override
    public void hardcorerevive$setHardcore(boolean value) {
        this.hardcore = value;
    }
}
