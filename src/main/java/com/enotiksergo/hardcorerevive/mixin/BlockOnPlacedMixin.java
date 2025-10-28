package com.enotiksergo.hardcorerevive.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class BlockOnPlacedMixin {

    @Inject(method = "onPlaced", at = @At("TAIL"))
    private void campfireSpawn$onPlaced(World world, BlockPos pos, BlockState state,
                                        LivingEntity placer, ItemStack itemStack, CallbackInfo ci) {
        if (world.isClient()) return;
        if (!state.isOf(Blocks.CAMPFIRE)) return;
        if (pos.getX() != 0 || pos.getZ() != 0) return;

        ServerWorld serverWorld = (ServerWorld) world;
        assert placer != null;

        float yaw = placer.getYaw();
        float pitch = placer.getPitch();

        if (serverWorld.getRegistryKey() != World.OVERWORLD) return;
        WorldProperties.SpawnPoint spawnpoint = new WorldProperties.SpawnPoint(new GlobalPos(world.getRegistryKey(), pos), yaw, pitch);
        serverWorld.setSpawnPoint(spawnpoint);
        serverWorld.getServer().saveAll(true, true, true);
        if (placer instanceof ServerPlayerEntity player) {
            player.sendMessage(Text.translatable("hardcorerevive.chat.campfire"), false);
            player.setSpawnPoint(new ServerPlayerEntity.Respawn(spawnpoint, true), false);
            serverWorld.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 2.0f, 1.0f);
        }
    }
}