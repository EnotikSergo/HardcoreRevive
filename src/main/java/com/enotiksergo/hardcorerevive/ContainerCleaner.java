package com.enotiksergo.hardcorerevive;

import com.enotiksergo.hardcorerevive.config.HardcoreReviveConfig;
import com.enotiksergo.hardcorerevive.duck.ChunkMapExt;
import com.enotiksergo.hardcorerevive.duck.ServerChunkCacheExt;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.entity.vehicle.boat.ChestRaft;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ContainerCleaner {
    public static void clearContainersInWorld(ServerLevel mcWorld, CommandSourceStack source) {
        MinecraftServer server = mcWorld.getServer();
        CompletableFuture.runAsync(() -> {
            int clearedCount = 0;

            ServerChunkCache chunkManager = mcWorld.getChunkSource();
            ChunkMap loadingManager = ((ServerChunkCacheExt) chunkManager).getChunkMap();
            Long2ObjectLinkedOpenHashMap<ChunkHolder> holders = ((ChunkMapExt) loadingManager).getUpdatingChunkMap();

            Set<UUID> processedEntities = new HashSet<>();
            for (ChunkHolder holder : holders.values()) {
                LevelChunk chunk = holder.getTickingChunk();
                if (chunk != null) {
                    clearedCount += clearContainersInChunk(mcWorld, chunk);
                    clearedCount += clearEntitiesInChunk(mcWorld, chunk, processedEntities);
                }
            }
            int finalCount = clearedCount;

            server.execute(() -> source.sendSuccess(() -> Component.translatable("hardcorerevive.chat.clear.end", finalCount), false));
        });
    }

    private static int clearContainersInChunk(ServerLevel world, LevelChunk chunk) {
        int cleared = 0;
        var cfg = HardcoreReviveConfig.get();
        final boolean CLEAN_SHELF = cfg.cleanShelf;
        final boolean CLEAN_CHISELED_BOOKSHELF = cfg.cleanChiseled_bookshelf;

        for (BlockEntity be : chunk.getBlockEntities().values()) {
            boolean hasLootTable = false;
            try {
                var nbt = be.saveWithFullMetadata(world.registryAccess());
                hasLootTable = nbt.contains("LootTable");
            } catch (Throwable ignored) { }

            // Сундуки/бочки/и т.п.
            if (be instanceof RandomizableContainerBlockEntity container) {
                if (!hasLootTable && !container.isEmpty()) {
                    container.clearContent();
                    container.setChanged();
                    cleared++;
                }
                continue;
            }

            // Печки
            if (be instanceof AbstractFurnaceBlockEntity furnace) {
                if (!furnace.isEmpty()) {
                    furnace.clearContent();
                    furnace.setChanged();
                    cleared++;
                }
                continue;
            }

            // Резные книжные полки
            if(CLEAN_CHISELED_BOOKSHELF) {
                if (be instanceof ChiseledBookShelfBlockEntity bookshelf) {
                    if (!bookshelf.isEmpty()) {
                        bookshelf.clearContent();
                        bookshelf.setChanged();
                        cleared++;
                    }
                    continue;
                }
            }

            // Декоративный горшок
            if (be instanceof DecoratedPotBlockEntity pot) {
                if (!hasLootTable && !pot.isEmpty()) {
                    pot.clearContent();
                    pot.setChanged();
                    cleared++;
                }
                continue;
            }

            // Полки
            if(CLEAN_SHELF) {
                if (be instanceof Container inv && be.getClass().getSimpleName().equals("ShelfBlockEntity")) {
                    if (!inv.isEmpty()) {
                        inv.clearContent();
                        be.setChanged();
                        cleared++;
                    }
                }
            }
        }

        return cleared;
    }

    private static int clearEntitiesInChunk(ServerLevel world, LevelChunk chunk, Set<UUID> processed) {
        int cleared = 0;
        var cfg = HardcoreReviveConfig.get();
        final boolean CLEAN_FRAMES = cfg.cleanItemFrames;
        final boolean CLEAN_ARMOR_STANDS = cfg.cleanArmorStands;

        ChunkPos pos = chunk.getPos();
        int minY = world.getMinY();
        int maxYExclusive = minY + world.getHeight();
        AABB box = new AABB(
                pos.getMinBlockX(), minY, pos.getMinBlockZ(),
                pos.getMaxBlockX() + 1, maxYExclusive, pos.getMaxBlockZ() + 1
        );

        // Рамки
        if(CLEAN_FRAMES) {
            for (ItemFrame frame : world.getEntitiesOfClass(ItemFrame.class, box, e -> true)) {
                if (processed.add(frame.getUUID()) && !frame.getItem().isEmpty()) {
                    frame.setItem(ItemStack.EMPTY, true);
                    frame.setRotation(0);
                    cleared++;
                }
            }
        }

        // Стенды для брони
        if(CLEAN_ARMOR_STANDS) {
            for (ArmorStand stand : world.getEntitiesOfClass(ArmorStand.class, box, e -> true)) {
                if (!processed.add(stand.getUUID())) continue;
                boolean changed = false;
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    if (!stand.getItemBySlot(slot).isEmpty()) {
                        stand.setItemSlot(slot, ItemStack.EMPTY);
                        changed = true;
                    }
                }
                if (changed) cleared++;
            }
        }

        // Вагонетки с сундуком
        for (MinecartChest cart : world.getEntitiesOfClass(MinecartChest.class, box, e -> true)) {
            if (!processed.add(cart.getUUID())) continue;
            if (cart.getLootTable() == null && !cart.isEmpty()) {
                cart.clearContent();
                cart.setChanged();
                cleared++;
            }
        }

        // Вагонетки с воронкой
        for (MinecartHopper cart : world.getEntitiesOfClass(MinecartHopper.class, box, e -> true)) {
            if (!processed.add(cart.getUUID())) continue;
            if (cart.getLootTable() == null && !cart.isEmpty()) {
                cart.clearContent();
                cart.setChanged();
                cleared++;
            }
        }

        // Грузовые лодки
        for (ChestBoat boat : world.getEntitiesOfClass(ChestBoat.class, box, e -> true)) {
            if (!processed.add(boat.getUUID())) continue;
            if (boat.getLootTable() == null && !boat.isEmpty()) {
                boat.clearContent();
                boat.setChanged();
                cleared++;
            }
        }
        for (ChestRaft raft : world.getEntitiesOfClass(ChestRaft.class, box, e -> true)) {
            if (!processed.add(raft.getUUID())) continue;
            if (raft.getLootTable() == null && !raft.isEmpty()) {
                raft.clearContent();
                raft.setChanged();
                cleared++;
            }
        }

        return cleared;
    }
}
