package com.enotiksergo.hardcorerevive.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            var cfg = HardcoreReviveConfig.get();

            ConfigBuilder b = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.translatable("hardcorerevive.config.title"));

            ConfigEntryBuilder e = b.entryBuilder();
            ConfigCategory cat = b.getOrCreateCategory(Text.translatable("hardcorerevive.config.category.cleaning"));

            cat.addEntry(e.startIntField(
                            Text.translatable("hardcorerevive.config.teleport_radius"),
                            cfg.teleportRadius)
                    .setDefaultValue(10000000)
                            .setMax(30000000)
                            .setMin(1000000)
                    .setTooltip(Text.translatable("hardcorerevive.config.teleport_radius.tooltip"))
                    .setSaveConsumer(v -> cfg.teleportRadius = v)
                    .build());

            cat.addEntry(e.startBooleanToggle(
                            Text.translatable("hardcorerevive.config.clean_shelf"),
                            cfg.cleanShelf)
                    .setDefaultValue(false)
                    .setTooltip(Text.translatable("hardcorerevive.config.clean_shelf.tooltip"))
                    .setSaveConsumer(v -> cfg.cleanShelf = v)
                    .build());

            cat.addEntry(e.startBooleanToggle(
                            Text.translatable("hardcorerevive.config.clean_chiseled_bookshelf"),
                            cfg.cleanChiseled_bookshelf)
                    .setDefaultValue(false)
                    .setTooltip(Text.translatable("hardcorerevive.config.clean_chiseled_bookshelf.tooltip"))
                    .setSaveConsumer(v -> cfg.cleanChiseled_bookshelf = v)
                    .build());

            cat.addEntry(e.startBooleanToggle(
                            Text.translatable("hardcorerevive.config.clean_item_frames"),
                            cfg.cleanItemFrames)
                    .setDefaultValue(false)
                    .setTooltip(Text.translatable("hardcorerevive.config.clean_item_frames.tooltip"))
                    .setSaveConsumer(v -> cfg.cleanItemFrames = v)
                    .build());

            cat.addEntry(e.startBooleanToggle(
                            Text.translatable("hardcorerevive.config.clean_armor_stands"),
                            cfg.cleanArmorStands)
                    .setDefaultValue(false)
                    .setTooltip(Text.translatable("hardcorerevive.config.clean_armor_stands.tooltip"))
                    .setSaveConsumer(v -> cfg.cleanArmorStands = v)
                    .build());

            b.setSavingRunnable(HardcoreReviveConfig::save);
            return b.build();
        };
    }
}
