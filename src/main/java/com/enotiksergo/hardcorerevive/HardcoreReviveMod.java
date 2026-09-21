package com.enotiksergo.hardcorerevive;

import com.enotiksergo.hardcorerevive.config.HardcoreReviveConfig;
import com.enotiksergo.hardcorerevive.net.ReviveNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import java.util.*;

public class HardcoreReviveMod implements ModInitializer {

	private static final int TELEPORT_RADIUS = HardcoreReviveConfig.get().teleportRadius;

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			ClearContainersCommand.register(dispatcher);
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity instanceof Player player) {
				ServerLevel deathWorld = (ServerLevel) player.level();
				MinecraftServer server = deathWorld.getServer();

				if (server.isHardcore()) {
					// Очистка выпавших вещей рядом с местом смерти
					List<ItemEntity> droppedItems = deathWorld.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(20.0), item -> true);
					for (ItemEntity item : droppedItems) {
						item.discard();
					}

					// Очистка эндер-сундука
					player.getEnderChestInventory().clearContent();

					ServerLevel overworld = server.overworld();
					if (overworld == null) return;

					// Новые координаты спавна
					Random random = new Random();
					int x = random.nextInt(TELEPORT_RADIUS * 2) - TELEPORT_RADIUS;
					int z = random.nextInt(TELEPORT_RADIUS * 2) - TELEPORT_RADIUS;

					overworld.getChunkSource().getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
					int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
					BlockPos pos = new BlockPos(x, y, z);

					LevelData.RespawnData spawnpoint =
							new LevelData.RespawnData(new GlobalPos(overworld.dimension(), pos),
									player.getYRot(), player.getXRot());

					overworld.setRespawnData(spawnpoint);

					ServerPlayer newPlayer = server.getPlayerList().getPlayer(player.getUUID());
					if (newPlayer != null) {
						newPlayer.setRespawnPosition(new ServerPlayer.RespawnConfig(spawnpoint, true), false);
					}
				}
			}
		});

		ReviveNetworking.registerPayloads();
		ReviveNetworking.registerServerReceivers();
		HardcoreReviveConfig.load();
	}
}
