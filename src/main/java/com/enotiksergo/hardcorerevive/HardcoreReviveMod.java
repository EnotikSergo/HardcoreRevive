package com.enotiksergo.hardcorerevive;

import com.enotiksergo.hardcorerevive.config.HardcoreReviveConfig;
import com.enotiksergo.hardcorerevive.net.ReviveNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.chunk.ChunkStatus;
import java.util.*;

public class HardcoreReviveMod implements ModInitializer {

	private static final int TELEPORT_RADIUS = HardcoreReviveConfig.get().teleportRadius;

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			ClearContainersCommand.register(dispatcher);
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity instanceof PlayerEntity player) {
				ServerWorld deathWorld = (ServerWorld) player.getEntityWorld();
				MinecraftServer server = deathWorld.getServer();

				if (server.isHardcore()) {
					// Очистка выпавших вещей рядом с местом смерти
					List<ItemEntity> droppedItems = deathWorld.getEntitiesByClass(ItemEntity.class, player.getBoundingBox().expand(20.0), item -> true);
					for (ItemEntity item : droppedItems) {
						item.discard();
					}

					// Очистка эндер-сундука
					player.getEnderChestInventory().clear();

					ServerWorld overworld = server.getOverworld();
					if (overworld == null) return;

					// Новые координаты спавна
					Random random = new Random();
					int x = random.nextInt(TELEPORT_RADIUS * 2) - TELEPORT_RADIUS;
					int z = random.nextInt(TELEPORT_RADIUS * 2) - TELEPORT_RADIUS;

					overworld.getChunkManager().getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
					int y = overworld.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
					BlockPos pos = new BlockPos(x, y, z);

					WorldProperties.SpawnPoint spawnpoint =
							new WorldProperties.SpawnPoint(new GlobalPos(overworld.getRegistryKey(), pos),
									player.getYaw(), player.getPitch());

					overworld.setSpawnPoint(spawnpoint);

					ServerPlayerEntity newPlayer = server.getPlayerManager().getPlayer(player.getUuid());
					if (newPlayer != null) {
						newPlayer.setSpawnPoint(new ServerPlayerEntity.Respawn(spawnpoint, true), false);
					}
				}
			}
		});

		ReviveNetworking.registerPayloads();
		ReviveNetworking.registerServerReceivers();
		HardcoreReviveConfig.load();
	}
}
