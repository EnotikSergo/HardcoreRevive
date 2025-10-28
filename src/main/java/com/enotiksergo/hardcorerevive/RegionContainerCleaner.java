package com.enotiksergo.hardcorerevive;

import com.enotiksergo.hardcorerevive.config.HardcoreReviveConfig;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class RegionContainerCleaner {
    public static void clearAllContainers(ServerWorld world, ServerCommandSource source, boolean showBossbar) {
        MinecraftServer server = world.getServer();
        Path worldDir = server.getSavePath(WorldSavePath.ROOT);

        Path regionDir;
        if (world.getRegistryKey().equals(World.OVERWORLD)) {
            regionDir = worldDir.resolve("region");
        } else {
            regionDir = worldDir.resolve(world.getRegistryKey().getValue().getPath()).resolve("region");
        }
        regionDir = regionDir.normalize();

        if (!Files.isDirectory(regionDir)) {
            System.err.println("[HardcoreRevive Cleaner] Region dirrectory not found : " + regionDir);
            return;
        }

        System.out.println("[HardcoreRevive Cleaner] Clean Start: " + regionDir);
        ServerBossBar bossBar = new ServerBossBar(Text.translatable("hardcorerevive.bossbar.clear"), BossBar.Color.YELLOW, BossBar.Style.PROGRESS);

        if (showBossbar) {
            server.execute(() -> {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    bossBar.addPlayer(player);
                }
            });
        }

        Path finalRegionDir = regionDir;
        Path logsDir = server.getPath("HardcoreReviveCleanLogs");
        try {
            Files.createDirectories(logsDir);
        } catch (IOException e) {
            System.err.println("[HardcoreRevive Cleaner] Couldn't create a logs folder: " + e.getMessage());
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path logFile = logsDir.resolve("clean_log_" + timestamp + ".txt");
        long startTime = System.currentTimeMillis();
        try {
            String logContent = "[HardcoreRevive Cleaner] " + Text.translatable("hardcorerevive.logs.start").getString()
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + System.lineSeparator();
            Files.writeString(logFile, logContent, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        } catch (IOException e) {
            System.err.println("[HardcoreRevive Cleaner] Failed to create a log file: " + e.getMessage());
        }
        server.saveAll(true, true, true);
        CompletableFuture.runAsync(() -> {
            AtomicInteger clearedCount = new AtomicInteger();

            try (Stream<Path> stream = Files.list(finalRegionDir)) {
                List<Path> files = stream.filter(f -> f.toString().endsWith(".mca")).toList();
                int totalRegions = files.size();

                ExecutorService pool = Executors.newFixedThreadPool(Math.min(4, totalRegions));
                List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (int i = 0; i < files.size(); i++) {
                    final int regionIndex = i;
                    Path regionPath = files.get(i);

                    AtomicInteger finalI = new AtomicInteger(i);
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        StringBuilder localLog = new StringBuilder();
                        try {
                            bossBar.setName(Text.translatable("hardcorerevive.bossbar.region").append(Text.literal(regionPath.getFileName().toString())).append(" §a[§f" + finalI.getAndIncrement() + "§a/§f" + totalRegions + "§a]§f"));
                            bossBar.setPercent((float) regionIndex / totalRegions);
                            cleanRegion(regionPath, world, clearedCount, localLog);
                            synchronized (RegionContainerCleaner.class) {
                                Files.writeString(logFile, localLog.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                            }
                        } catch (Exception e) {
                            System.err.println("[HardcoreRevive Cleaner] Failed Region " + regionPath + ": " + e.getMessage());
                        }
                    }, pool);
                    futures.add(future);
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                pool.shutdown();
            } catch (IOException e) {
                System.err.println("[HardcoreRevive Cleaner] Failed region read: " + e.getMessage());
            }

            Path entitiesDir = finalRegionDir.resolveSibling("entities");
            if (Files.isDirectory(entitiesDir)) {
                try (Stream<Path> stream = Files.list(entitiesDir)) {
                    List<Path> files = stream.filter(f -> f.toString().endsWith(".mca")).toList();
                    int totalRegions = files.size();

                    ExecutorService pool = Executors.newFixedThreadPool(Math.min(4, totalRegions));
                    List<CompletableFuture<Void>> futures = new ArrayList<>();

                    for (int i = 0; i < files.size(); i++) {
                        final int regionIndex = i;
                        Path regionPath = files.get(i);
                        AtomicInteger finalI = new AtomicInteger(i);
                        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                            StringBuilder localLog = new StringBuilder();
                            try {
                                bossBar.setName(Text.translatable("hardcorerevive.bossbar.region2").append(Text.literal(regionPath.getFileName().toString())).append(" §a[§f" + finalI.getAndIncrement() + "§a/§f" + totalRegions + "§a]§f"));
                                bossBar.setPercent((float) regionIndex / totalRegions);
                                cleanEntitiesRegion(regionPath, world, clearedCount, localLog);
                                synchronized (RegionContainerCleaner.class) {
                                    Files.writeString(logFile, localLog.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                                }
                            } catch (Exception e) {
                                System.err.println("[HardcoreRevive Cleaner] Failed Entities Region " + regionPath + ": " + e.getMessage());
                            }
                        }, pool);
                        futures.add(future);
                    }

                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                    pool.shutdown();
                } catch (IOException e) {
                    System.err.println("[HardcoreRevive Cleaner] Failed entities read: " + e.getMessage());
                }
            } else {
                System.out.println("[HardcoreRevive Cleaner] Entities directory not found: " + entitiesDir);
            }

            server.execute(() -> {
                if (showBossbar) {
                    bossBar.clearPlayers();
                }
                world.getChunkManager().save(true);

                long elapsed = System.currentTimeMillis() - startTime;
                String timeFormatted = formatElapsedTime(elapsed);

                source.sendFeedback(() ->
                        Text.translatable("hardcorerevive.chat.clear.end", clearedCount.get()), false);

                try {
                    String logContent = System.lineSeparator() +
                            "[HardcoreRevive Cleaner] " + Text.translatable("hardcorerevive.logs.end2").getString() + clearedCount.get() + System.lineSeparator() +
                            "[HardcoreRevive Cleaner] " + Text.translatable("hardcorerevive.logs.end").getString() + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + System.lineSeparator() +
                            "[HardcoreRevive Cleaner] " + Text.translatable("hardcorerevive.logs.time").getString() + timeFormatted + System.lineSeparator();

                    Files.writeString(logFile, logContent, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    System.err.println("[HardcoreRevive Cleaner] Error log writing: " + e.getMessage());
                }
            });
        });
    }
    public static void clearAllDimensions(ServerCommandSource source, boolean showBossbar) {
        MinecraftServer server = source.getServer();

        ServerBossBar bossBar = new ServerBossBar(
                Text.translatable("hardcorerevive.bossbar.clear"),
                BossBar.Color.YELLOW,
                BossBar.Style.PROGRESS
        );
        if (showBossbar) {
            server.execute(() -> {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    bossBar.addPlayer(player);
                }
            });
        }

        Path logsDir = server.getPath("HardcoreReviveCleanLogs");
        try { Files.createDirectories(logsDir); } catch (IOException ignored) {}
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path logFile = logsDir.resolve("clean_log_" + timestamp + ".txt");
        long startTime = System.currentTimeMillis();
        try {
            String logContent = "[HardcoreRevive Cleaner] " + Text.translatable("hardcorerevive.logs.start").getString()
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + System.lineSeparator();
            Files.writeString(logFile, logContent, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        } catch (IOException e) {
            System.err.println("[HardcoreRevive Cleaner] Failed to create a log file: " + e.getMessage());
        }

        CompletableFuture.runAsync(() -> {
            AtomicInteger clearedCount = new AtomicInteger();

            List<ServerWorld> targets = new ArrayList<>();
            ServerWorld overworld = server.getWorld(World.OVERWORLD);
            ServerWorld nether    = server.getWorld(World.NETHER);
            ServerWorld end       = server.getWorld(World.END);
            if (overworld != null) targets.add(overworld);
            if (nether != null)    targets.add(nether);
            if (end != null)       targets.add(end);

            for (ServerWorld w : targets) {
                Path regionDir = resolveRegionDir(server, w).normalize();
                System.out.println("[HardcoreRevive Cleaner] Clean Start: " + regionDir);

                if (!Files.isDirectory(regionDir)) {
                    System.err.println("[HardcoreRevive Cleaner] Region directory not found: " + regionDir);
                    continue;
                }

                try {
                    String head = System.lineSeparator() +
                            "[HardcoreRevive Cleaner] === Dimension: " + w.getRegistryKey().getValue() + " ===" +
                            System.lineSeparator();
                    Files.writeString(logFile, head, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                } catch (IOException ignored) {}

                try (Stream<Path> stream = Files.list(regionDir)) {
                    List<Path> files = stream.filter(f -> f.toString().endsWith(".mca")).toList();
                    int totalRegions = files.size();

                    ExecutorService pool = Executors.newFixedThreadPool(Math.min(4, Math.max(1, totalRegions)));
                    List<CompletableFuture<Void>> futures = new ArrayList<>();

                    for (int i = 0; i < files.size(); i++) {
                        final int regionIndex = i;
                        Path regionPath = files.get(i);
                        AtomicInteger finalI = new AtomicInteger(i);
                        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                            StringBuilder localLog = new StringBuilder();
                            try {
                                bossBar.setName(Text.translatable("hardcorerevive.bossbar.region").append(Text.literal(w.getRegistryKey().getValue().toShortTranslationKey() + " ")).append(Text.literal(regionPath.getFileName().toString())).append(" §a[§f" + finalI.getAndIncrement() + "§a/§f" + totalRegions + "§a]§f"));
                                bossBar.setPercent((float) regionIndex / totalRegions);

                                cleanRegion(regionPath, w, clearedCount, localLog);

                                synchronized (RegionContainerCleaner.class) {
                                    Files.writeString(logFile, localLog.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                                }
                            } catch (Exception e) {
                                System.err.println("[HardcoreRevive Cleaner] Failed Region " + regionPath + ": " + e.getMessage());
                            }
                        }, pool);

                        futures.add(future);
                    }

                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                    pool.shutdown();
                } catch (IOException e) {
                    System.err.println("[HardcoreRevive Cleaner] Failed region read: " + e.getMessage());
                }

                Path entitiesDir = regionDir.resolveSibling("entities");
                if (Files.isDirectory(entitiesDir)) {
                    try (Stream<Path> stream = Files.list(entitiesDir)) {
                        List<Path> files = stream.filter(f -> f.toString().endsWith(".mca")).toList();
                        int totalRegions = files.size();

                        ExecutorService pool = Executors.newFixedThreadPool(Math.min(4, Math.max(1, totalRegions)));
                        List<CompletableFuture<Void>> futures = new ArrayList<>();

                        for (int i = 0; i < files.size(); i++) {
                            final int regionIndex = i;
                            Path regionPath = files.get(i);

                            AtomicInteger finalI = new AtomicInteger(i);
                            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                                StringBuilder localLog = new StringBuilder();
                                try {
                                    bossBar.setName(Text.translatable("hardcorerevive.bossbar.region2").append(Text.literal(w.getRegistryKey().getValue().toShortTranslationKey() + " ")).append(Text.literal(regionPath.getFileName().toString())).append(" §a[§f" + finalI.getAndIncrement() + "§a/§f" + totalRegions + "§a]§f"));
                                    bossBar.setPercent((float) regionIndex / totalRegions);

                                    cleanEntitiesRegion(regionPath, w, clearedCount, localLog);

                                    synchronized (RegionContainerCleaner.class) {
                                        Files.writeString(logFile, localLog.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                                    }
                                } catch (Exception e) {
                                    System.err.println("[HardcoreRevive Cleaner] Failed Entities Region " + regionPath + ": " + e.getMessage());
                                }
                            }, pool);

                            futures.add(future);
                        }

                        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                        pool.shutdown();
                    } catch (IOException e) {
                        System.err.println("[HardcoreRevive Cleaner] Failed entities read: " + e.getMessage());
                    }
                } else {
                    System.out.println("[HardcoreRevive Cleaner] Entities directory not found: " + entitiesDir);
                }

                server.execute(() -> w.getChunkManager().save(true));
            }

            server.execute(() -> {
                if (showBossbar) {
                    bossBar.clearPlayers();
                }

                long elapsed = System.currentTimeMillis() - startTime;
                String timeFormatted = formatElapsedTime(elapsed);

                source.sendFeedback(() ->
                        Text.translatable("hardcorerevive.chat.clear.end", clearedCount.get()), false);

                try {
                    String logContent = System.lineSeparator() +
                            "[HardcoreRevive Cleaner] " + Text.translatable("hardcorerevive.logs.end2").getString() + clearedCount.get() + System.lineSeparator() +
                            "[HardcoreRevive Cleaner] " + Text.translatable("hardcorerevive.logs.end").getString() + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + System.lineSeparator() +
                            "[HardcoreRevive Cleaner] " + Text.translatable("hardcorerevive.logs.time").getString() + timeFormatted + System.lineSeparator();

                    Files.writeString(logFile, logContent, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    System.err.println("[HardcoreRevive Cleaner] Error log writing: " + e.getMessage());
                }
            });
        });
    }

    private static void cleanRegion(Path regionPath, ServerWorld world, AtomicInteger clearedCount, StringBuilder logBuffer) throws IOException {
        String fileName = regionPath.getFileName().toString();
        String[] parts = fileName.replace("r.", "").replace(".mca", "").split("\\.");
        int regionX = Integer.parseInt(parts[0]);
        int regionZ = Integer.parseInt(parts[1]);

        if (regionHasLoadedChunks(world, regionX, regionZ)) {
            logBuffer.append("[Skip] Region ")
                    .append(regionPath.getFileName())
                    .append(" has loaded chunks.")
                    .append(System.lineSeparator());
            return;
        }

        var cfg = HardcoreReviveConfig.get();
        final boolean CLEAN_SHELF = cfg.cleanShelf;
        final boolean CLEAN_CHISELED_BOOKSHELF = cfg.cleanChiseled_bookshelf;

        StorageKey storageKey = new StorageKey("region", world.getRegistryKey(), "region");
        Path regionDir = regionPath.getParent();

        try (RegionFile regionFile = new RegionFile(storageKey, regionPath, regionDir, ChunkCompressionFormat.DEFLATE, true)) {

            for (int localX = 0; localX < 32; localX++) {
                for (int localZ = 0; localZ < 32; localZ++) {
                    int chunkX = (regionX << 5) + localX;
                    int chunkZ = (regionZ << 5) + localZ;

                    ChunkPos pos = new ChunkPos(chunkX, chunkZ);

                    if (world.isChunkLoaded(pos.x, pos.z)) continue;
                    if (!regionFile.hasChunk(pos)) continue;

                    try (DataInputStream input = regionFile.getChunkInputStream(pos)) {
                        if (input == null) continue;

                        Path tempFile = Files.createTempFile("chunk", ".nbt");
                        try {
                            try (OutputStream out = Files.newOutputStream(tempFile)) {
                                input.transferTo(out);
                            }

                            NbtCompound nbt = NbtIo.read(tempFile);
                            if (nbt == null) continue;
                            AtomicBoolean modified = new AtomicBoolean(false);

                            nbt.getList("block_entities").ifPresent(blockEntities -> {

                                for (int i = 0; i < blockEntities.size(); i++) {
                                    blockEntities.getCompound(i).ifPresent(be -> {
                                        String id = String.valueOf(be.getString("id"));
                                        if (isContainerId(id) && be.contains("Items")) {
                                            if ((id.contains("chiseled_bookshelf") && !CLEAN_CHISELED_BOOKSHELF) || (id.contains("shelf") && !CLEAN_SHELF)) return;
                                            be.getList("Items").ifPresent(items -> {
                                                if (!items.isEmpty()) {
                                                    logBuffer.append("Region: ").append(regionPath.getFileName())
                                                            .append(" | Chunk: [").append(chunkX).append(", ").append(chunkZ).append("]")
                                                            .append(" | Block: ").append(be.getString("id").orElse("unknown"));

                                                    int bx = be.getInt("x").orElse(0);
                                                    int by = be.getInt("y").orElse(0);
                                                    int bz = be.getInt("z").orElse(0);
                                                    logBuffer.append(" | Coord: (")
                                                            .append(bx).append(", ")
                                                            .append(by).append(", ")
                                                            .append(bz).append(")");

                                                    Map<String, Integer> combined = new HashMap<>();
                                                    for (int j = 0; j < items.size(); j++) {
                                                        items.getCompound(j).ifPresent(item -> {
                                                            String itemId = item.getString("id").orElse("unknown");
                                                            int count = item.getInt("count").orElse(0);
                                                            combined.merge(itemId, count, Integer::sum);
                                                        });
                                                    }
                                                    if (!combined.isEmpty()) {
                                                        logBuffer.append(" | Items: [");
                                                        int idx = 0;
                                                        for (var entry : combined.entrySet()) {
                                                            logBuffer.append(entry.getKey()).append(" x").append(entry.getValue());
                                                            if (++idx < combined.size()) logBuffer.append(", ");
                                                        }
                                                        logBuffer.append("]");
                                                    }

                                                    logBuffer.append(System.lineSeparator());

                                                    be.remove("Items");
                                                    clearedCount.getAndIncrement();
                                                    modified.set(true);
                                                }
                                            });
                                        }
                                    });
                                }
                            });
                            if (modified.get()) {
                                try (DataOutputStream out = regionFile.getChunkOutputStream(pos)) {
                                    NbtIo.write(nbt, out);
                                }
                            }
                        } finally {
                            Files.deleteIfExists(tempFile);
                        }
                    } catch (Exception e) {
                        System.err.println("[HardcoreRevive Cleaner] Failed chunk " + pos + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    private static void cleanEntitiesRegion(Path regionPath, ServerWorld world, AtomicInteger clearedCount, StringBuilder logBuffer) throws IOException {
        String fileName = regionPath.getFileName().toString();
        String[] parts = fileName.replace("r.", "").replace(".mca", "").split("\\.");
        int regionX = Integer.parseInt(parts[0]);
        int regionZ = Integer.parseInt(parts[1]);
        if (regionHasLoadedChunks(world, regionX, regionZ)) {
            logBuffer.append("[Skip] Region(Entities) ")
                    .append(regionPath.getFileName())
                    .append(" has loaded chunks.")
                    .append(System.lineSeparator());
            return;
        }
        var cfg = HardcoreReviveConfig.get();
        final boolean CLEAN_FRAMES = cfg.cleanItemFrames;
        final boolean CLEAN_ARMOR_STANDS = cfg.cleanArmorStands;

        StorageKey storageKey = new StorageKey("entities", world.getRegistryKey(), "entities");
        Path entitiesDir = regionPath.getParent();

        try (RegionFile regionFile = new RegionFile(storageKey, regionPath, entitiesDir, ChunkCompressionFormat.DEFLATE, true)) {

            for (int localX = 0; localX < 32; localX++) {
                for (int localZ = 0; localZ < 32; localZ++) {
                    int chunkX = (regionX << 5) + localX;
                    int chunkZ = (regionZ << 5) + localZ;

                    ChunkPos pos = new ChunkPos(chunkX, chunkZ);

                    if (world.isChunkLoaded(pos.x, pos.z)) continue;
                    if (!regionFile.hasChunk(pos)) continue;

                    try (DataInputStream input = regionFile.getChunkInputStream(pos)) {
                        if (input == null) continue;

                        Path tempFile = Files.createTempFile("entities_chunk", ".nbt");
                        try {
                            try (OutputStream out = Files.newOutputStream(tempFile)) {
                                input.transferTo(out);
                            }

                            NbtCompound nbt = NbtIo.read(tempFile);
                            if (nbt == null) continue;

                            AtomicBoolean modified = new AtomicBoolean(false);

                            java.util.function.Consumer<NbtList> cleaner = (entities) -> {
                                for (int i = 0; i < entities.size(); i++) {
                                    entities.getCompound(i).ifPresent(e -> {
                                        String id = e.getString("id").orElse("unknown");

                                        if (CLEAN_FRAMES && id.contains("item_frame")) {
                                            if (e.contains("Item") || e.contains("item")) {
                                                e.getCompound("Item").ifPresent(item -> {
                                                    if (!item.isEmpty()) {
                                                        logBuffer.append("Region(entities): ").append(regionPath.getFileName())
                                                                .append(" | Chunk: [").append(chunkX).append(", ").append(chunkZ).append("]")
                                                                .append(" | Entity: ").append(id)
                                                                .append(" | Item: ").append(item.getString("id").orElse("unknown"))
                                                                .append(" x").append(item.getInt("count").orElse(item.getInt("Count").orElse(0)))
                                                                .append(System.lineSeparator());
                                                        e.remove("Item");
                                                        clearedCount.getAndIncrement();
                                                        modified.set(true);
                                                    }
                                                });
                                            }
                                        }

                                        else if (CLEAN_ARMOR_STANDS && id.contains("armor_stand")) {
                                            java.util.concurrent.atomic.AtomicBoolean removedSomething = new java.util.concurrent.atomic.AtomicBoolean(false);
                                            java.util.Map<String, Integer> combined = new java.util.HashMap<>();

                                            if (e.contains("equipment")) {
                                                e.getCompound("equipment").ifPresent(eq -> {
                                                    String[][] slots = new String[][]{
                                                            {"head"}, {"chest"}, {"legs"}, {"feet"},
                                                            {"mainhand","main_hand"}, {"offhand","off_hand"}
                                                    };
                                                    for (String[] variants : slots) {
                                                        for (String slot : variants) {
                                                            if (eq.contains(slot)) {
                                                                eq.getCompound(slot).ifPresent(it -> {
                                                                    if (!it.isEmpty()) {
                                                                        String itemId = it.getString("id").orElse("unknown");
                                                                        int count = it.getInt("count").orElse(it.getInt("Count").orElse(0));
                                                                        if (count <= 0) count = 1;
                                                                        combined.merge(itemId, count, Integer::sum);
                                                                        removedSomething.set(true);
                                                                    }
                                                                });
                                                                eq.remove(slot);
                                                                break;
                                                            }
                                                        }
                                                    }
                                                });
                                            }
                                            if (removedSomething.get()) {
                                                logBuffer.append("Region(entities): ").append(regionPath.getFileName())
                                                        .append(" | Chunk: [").append(chunkX).append(", ").append(chunkZ).append("]")
                                                        .append(" | Entity: ").append(id);
                                                if (!combined.isEmpty()) {
                                                    logBuffer.append(" | Items: [");
                                                    int idx = 0;
                                                    for (var entry : combined.entrySet()) {
                                                        logBuffer.append(entry.getKey()).append(" x").append(entry.getValue());
                                                        if (++idx < combined.size()) logBuffer.append(", ");
                                                    }
                                                    logBuffer.append("]");
                                                }
                                                logBuffer.append(System.lineSeparator());
                                                clearedCount.getAndIncrement();
                                                modified.set(true);
                                            }
                                        }

                                        else {
                                            java.util.concurrent.atomic.AtomicBoolean removed = new java.util.concurrent.atomic.AtomicBoolean(false);
                                            java.util.Map<String, Integer> combined = new java.util.HashMap<>();

                                            if (e.contains("Items") || e.contains("items")) {
                                                e.getList("Items").ifPresent(items -> {
                                                    if (!items.isEmpty()) {
                                                        for (int j = 0; j < items.size(); j++) {
                                                            items.getCompound(j).ifPresent(it -> {
                                                                String itemId = it.getString("id").orElse("unknown");
                                                                int count = it.getInt("count").orElse(it.getInt("Count").orElse(0));
                                                                if (count > 0) combined.merge(itemId, count, Integer::sum);
                                                            });
                                                        }
                                                        e.remove("Items");
                                                        removed.set(true);
                                                    }
                                                });
                                            }

                                            if (removed.get()) {
                                                logBuffer.append("Region(entities): ").append(regionPath.getFileName())
                                                        .append(" | Chunk: [").append(chunkX).append(", ").append(chunkZ).append("]")
                                                        .append(" | Entity: ").append(id);
                                                if (!combined.isEmpty()) {
                                                    logBuffer.append(" | Items: [");
                                                    int idx = 0;
                                                    for (var entry : combined.entrySet()) {
                                                        logBuffer.append(entry.getKey()).append(" x").append(entry.getValue());
                                                        if (++idx < combined.size()) logBuffer.append(", ");
                                                    }
                                                    logBuffer.append("]");
                                                }
                                                logBuffer.append(System.lineSeparator());
                                                clearedCount.getAndIncrement();
                                                modified.set(true);
                                            }
                                        }
                                    });
                                }
                            };

                            nbt.getList("entities").ifPresent(cleaner);
                            nbt.getList("Entities").ifPresent(cleaner);

                            if (modified.get()) {
                                try (DataOutputStream out = regionFile.getChunkOutputStream(pos)) {
                                    NbtIo.write(nbt, out);
                                }
                            }

                        } finally {
                            Files.deleteIfExists(tempFile);
                        }
                    } catch (Exception ex) {
                        System.err.println("[HardcoreRevive Cleaner] Failed entities chunk " + pos + ": " + ex.getMessage());
                    }
                }
            }
        }
    }

    private static boolean isContainerId(String id) {
        return id.contains("chest")
        || id.contains("barrel")
        || id.contains("hopper")
        || id.contains("dispenser")
        || id.contains("dropper")
        || id.contains("shulker_box")
        || id.contains("trapped_chest")
        || id.contains("furnace")
        || id.contains("blast_furnace")
        || id.contains("smoker")
        || id.contains("chiseled_bookshelf")
        || id.contains("crafter")
        || id.contains("decorated_pot")
        || id.contains("shelf");
    }

    private static String formatElapsedTime(long elapsedMs) {
        long totalSeconds = elapsedMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d h %d m %d s", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d m %d s", minutes, seconds);
        } else {
            return String.format("%d s", seconds);
        }
    }
    private static Path resolveRegionDir(MinecraftServer server, ServerWorld world) {
        Path root = server.getSavePath(WorldSavePath.ROOT);

        if (world.getRegistryKey().equals(World.OVERWORLD)) {
            Path p = root.resolve("region");
            if (Files.isDirectory(p)) return p;
        }

        var id = world.getRegistryKey().getValue();
        Path modern = root.resolve("dimensions").resolve(id.getNamespace()).resolve(id.getPath()).resolve("region");
        if (Files.isDirectory(modern)) return modern;

        if (world.getRegistryKey().equals(World.NETHER)) {
            Path p = root.resolve("DIM-1").resolve("region");
            if (Files.isDirectory(p)) return p;
        } else if (world.getRegistryKey().equals(World.END)) {
            Path p = root.resolve("DIM1").resolve("region");
            if (Files.isDirectory(p)) return p;
        }

        return root.resolve(id.getPath()).resolve("region");
    }

    private static boolean regionHasLoadedChunks(ServerWorld world, int regionX, int regionZ) {
        for (int lx = 0; lx < 32; lx++) {
            for (int lz = 0; lz < 32; lz++) {
                int cx = (regionX << 5) + lx;
                int cz = (regionZ << 5) + lz;
                if (world.isChunkLoaded(cx, cz)) return true;
            }
        }
        return false;
    }
}
