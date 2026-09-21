package com.enotiksergo.hardcorerevive.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class BlockOnPlacedMixin {

    @Inject(method = "setPlacedBy", at = @At("TAIL"))
    private void campfireSpawn$onPlaced(Level world, BlockPos pos, BlockState state,
                                        LivingEntity placer, ItemStack itemStack, CallbackInfo ci) {
        if (world.isClientSide()) return;
        if (!state.is(Blocks.CAMPFIRE)) return;
        if (pos.getX() != 0 || pos.getZ() != 0) return;

        ServerLevel serverWorld = (ServerLevel) world;
        assert placer != null;

        float yaw = placer.getYRot();
        float pitch = placer.getXRot();

        if (serverWorld.dimension() != Level.OVERWORLD) return;
        LevelData.RespawnData spawnpoint = new LevelData.RespawnData(new GlobalPos(world.dimension(), pos), yaw, pitch);
        serverWorld.setRespawnData(spawnpoint);
        serverWorld.getServer().saveEverything(true, true, true);
        if (placer instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.translatable("hardcorerevive.chat.campfire"), false);
            player.setRespawnPosition(new ServerPlayer.RespawnConfig(spawnpoint, true), false);
            serverWorld.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0f, 1.0f);
        }
    }
}