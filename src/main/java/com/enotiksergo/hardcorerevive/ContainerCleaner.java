package com.enotiksergo.hardcorerevive;

import com.enotiksergo.hardcorerevive.config.HardcoreReviveConfig;
import com.enotiksergo.hardcorerevive.mixin.ServerChunkLoadingManagerAccessor;
import com.enotiksergo.hardcorerevive.mixin.ServerChunkManagerAccessor;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.ChiseledBookshelfBlockEntity;
import net.minecraft.block.entity.DecoratedPotBlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.entity.vehicle.ChestRaftEntity;
import net.minecraft.entity.vehicle.HopperMinecartEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ContainerCleaner {
    public static void clearContainersInWorld(ServerWorld mcWorld, ServerCommandSource source) {
        MinecraftServer server = mcWorld.getServer();
        CompletableFuture.runAsync(() -> {
            int clearedCount = 0;

            ServerChunkManager chunkManager = mcWorld.getChunkManager();
            ServerChunkLoadingManager loadingManager = ((ServerChunkManagerAccessor) chunkManager).getChunkLoadingManager();
            Long2ObjectLinkedOpenHashMap<ChunkHolder> holders = ((ServerChunkLoadingManagerAccessor) loadingManager).getCurrentChunkHolders();

            Set<UUID> processedEntities = new HashSet<>();
            for (ChunkHolder holder : holders.values()) {
                WorldChunk chunk = holder.getWorldChunk();
                if (chunk != null) {
                    clearedCount += clearContainersInChunk(mcWorld, chunk);
                    clearedCount += clearEntitiesInChunk(mcWorld, chunk, processedEntities);
                }
            }
            int finalCount = clearedCount;

            server.execute(() -> source.sendFeedback(() -> Text.translatable("hardcorerevive.chat.clear.end", finalCount), false));
        });
    }

    private static int clearContainersInChunk(ServerWorld world, WorldChunk chunk) {
        int cleared = 0;
        var cfg = HardcoreReviveConfig.get();
        final boolean CLEAN_SHELF = cfg.cleanShelf;
        final boolean CLEAN_CHISELED_BOOKSHELF = cfg.cleanChiseled_bookshelf;

        for (BlockEntity be : chunk.getBlockEntities().values()) {
            boolean hasLootTable = false;
            try {
                var nbt = be.createNbtWithIdentifyingData(world.getRegistryManager());
                hasLootTable = nbt.contains("LootTable");
            } catch (Throwable ignored) { }

            // Сундуки/бочки/и т.п.
            if (be instanceof LootableContainerBlockEntity container) {
                if (!hasLootTable && !container.isEmpty()) {
                    container.clear();
                    container.markDirty();
                    cleared++;
                }
                continue;
            }

            // Печки
            if (be instanceof AbstractFurnaceBlockEntity furnace) {
                if (!furnace.isEmpty()) {
                    furnace.clear();
                    furnace.markDirty();
                    cleared++;
                }
                continue;
            }

            // Резные книжные полки
            if(CLEAN_CHISELED_BOOKSHELF) {
                if (be instanceof ChiseledBookshelfBlockEntity bookshelf) {
                    if (!bookshelf.isEmpty()) {
                        bookshelf.clear();
                        bookshelf.markDirty();
                        cleared++;
                    }
                    continue;
                }
            }

            // Декоративный горшок
            if (be instanceof DecoratedPotBlockEntity pot) {
                if (!hasLootTable && !pot.isEmpty()) {
                    pot.clear();
                    pot.markDirty();
                    cleared++;
                }
                continue;
            }

            // Полки
            if(CLEAN_SHELF) {
                if (be instanceof Inventory inv && be.getClass().getSimpleName().equals("ShelfBlockEntity")) {
                    if (!inv.isEmpty()) {
                        inv.clear();
                        be.markDirty();
                        cleared++;
                    }
                }
            }
        }

        return cleared;
    }

    private static int clearEntitiesInChunk(ServerWorld world, WorldChunk chunk, Set<UUID> processed) {
        int cleared = 0;
        var cfg = HardcoreReviveConfig.get();
        final boolean CLEAN_FRAMES = cfg.cleanItemFrames;
        final boolean CLEAN_ARMOR_STANDS = cfg.cleanArmorStands;

        ChunkPos pos = chunk.getPos();
        int minY = world.getBottomY();
        int maxYExclusive = minY + world.getHeight();
        Box box = new Box(
                pos.getStartX(), minY, pos.getStartZ(),
                pos.getEndX() + 1, maxYExclusive, pos.getEndZ() + 1
        );

        // Рамки
        if(CLEAN_FRAMES) {
            for (ItemFrameEntity frame : world.getEntitiesByClass(ItemFrameEntity.class, box, e -> true)) {
                if (processed.add(frame.getUuid()) && !frame.getHeldItemStack().isEmpty()) {
                    frame.setHeldItemStack(ItemStack.EMPTY, true);
                    frame.setRotation(0);
                    cleared++;
                }
            }
        }

        // Стенды для брони
        if(CLEAN_ARMOR_STANDS) {
            for (ArmorStandEntity stand : world.getEntitiesByClass(ArmorStandEntity.class, box, e -> true)) {
                if (!processed.add(stand.getUuid())) continue;
                boolean changed = false;
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    if (!stand.getEquippedStack(slot).isEmpty()) {
                        stand.equipStack(slot, ItemStack.EMPTY);
                        changed = true;
                    }
                }
                if (changed) cleared++;
            }
        }

        // Вагонетки с сундуком
        for (ChestMinecartEntity cart : world.getEntitiesByClass(ChestMinecartEntity.class, box, e -> true)) {
            if (!processed.add(cart.getUuid())) continue;
            if (cart.getLootTable() == null && !cart.isEmpty()) {
                cart.clear();
                cart.markDirty();
                cleared++;
            }
        }

        // Вагонетки с воронкой
        for (HopperMinecartEntity cart : world.getEntitiesByClass(HopperMinecartEntity.class, box, e -> true)) {
            if (!processed.add(cart.getUuid())) continue;
            if (cart.getLootTable() == null && !cart.isEmpty()) {
                cart.clear();
                cart.markDirty();
                cleared++;
            }
        }

        // Грузовые лодки
        for (ChestBoatEntity boat : world.getEntitiesByClass(ChestBoatEntity.class, box, e -> true)) {
            if (!processed.add(boat.getUuid())) continue;
            if (boat.getLootTable() == null && !boat.isEmpty()) {
                boat.clear();
                boat.markDirty();
                cleared++;
            }
        }
        for (ChestRaftEntity raft : world.getEntitiesByClass(ChestRaftEntity.class, box, e -> true)) {
            if (!processed.add(raft.getUuid())) continue;
            if (raft.getLootTable() == null && !raft.isEmpty()) {
                raft.clear();
                raft.markDirty();
                cleared++;
            }
        }

        return cleared;
    }
}
