package com.enotiksergo.hardcorerevive.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HardcoreReviveConfig {
    public int teleportRadius = 10000000;
    public boolean cleanItemFrames = false;
    public boolean cleanArmorStands = false;
    public boolean cleanShelf = false;
    public boolean cleanChiseled_bookshelf = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static HardcoreReviveConfig INSTANCE;

    private HardcoreReviveConfig() {}

    public static synchronized HardcoreReviveConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static synchronized void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("hardcorerevive.json");
        try (Reader r = Files.newBufferedReader(path)) {
            INSTANCE = GSON.fromJson(r, HardcoreReviveConfig.class);
            if (INSTANCE == null) INSTANCE = new HardcoreReviveConfig();
        } catch (IOException e) {
            INSTANCE = new HardcoreReviveConfig();
            save();
        }
    }

    public static synchronized void save() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("hardcorerevive.json");
        try {
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path)) {
                GSON.toJson(get(), w);
            }
        } catch (IOException e) {
            System.err.println("[HardcoreRevive] Error config save: " + e.getMessage());
        }
    }
}
