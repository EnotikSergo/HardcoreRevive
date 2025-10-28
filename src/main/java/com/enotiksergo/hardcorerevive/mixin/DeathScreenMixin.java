package com.enotiksergo.hardcorerevive.mixin;

import com.enotiksergo.hardcorerevive.client.ClientTerrainWaiter;
import com.enotiksergo.hardcorerevive.net.ReviveNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public class DeathScreenMixin {

    @Unique private ButtonWidget hardcorerevive$reviveButton;
    @Unique private ButtonWidget hardcorerevive$titleButton;
    @Unique private int          hardcorerevive$fallbackTicks;

    @Inject(method = "init", at = @At("TAIL"))
    private void addRespawnButton(CallbackInfo ci) {
        DeathScreen screen = (DeathScreen) (Object) this;
        ScreenAccessor screenAccessor = (ScreenAccessor) screen;

        try {
            var client = MinecraftClient.getInstance();

            if (client != null && client.world != null && client.getServer() != null) {

                var server = client.getServer();
                var world = server.getWorld(client.world.getRegistryKey());

                if (world != null) {
                    var levelProperties = world.getLevelProperties();
                    boolean isHardcore = levelProperties.isHardcore();

                    if (isHardcore) {
                        int buttonY = screen.height / 4 + 144;
                        ButtonWidget spectateButton = null;

                        for (var drawable : screenAccessor.getDrawables()) {
                            if (drawable instanceof ButtonWidget button) {
                                var msg = button.getMessage();
                                if (hardcorerevive$isSpectate(msg)) {
                                    spectateButton = button;
                                } else if (hardcorerevive$isTitle(msg)) {
                                    hardcorerevive$titleButton = button;
                                }
                            }
                        }

                        if (spectateButton != null) {
                            buttonY = spectateButton.getY();
                            screenAccessor.getDrawables().remove(spectateButton);
                            screenAccessor.getChildren().remove(spectateButton);
                        }

                        hardcorerevive$reviveButton = ButtonWidget.builder(
                                Text.translatable("hardcorerevive.button.revive"),
                                button -> {
                                    if (client != null && client.player != null) {
                                        if (server == null) return;
                                        client.player.requestRespawn();
                                        client.setScreen(null);
                                        ReviveNetworking.sendReviveRequest();
                                        ClientTerrainWaiter.startWaiting();
                                    }
                                }
                        ).dimensions(screen.width / 2 - 100, buttonY, 200, 20).build();

                        hardcorerevive$reviveButton.active =
                                hardcorerevive$titleButton != null && hardcorerevive$titleButton.active;

                        screenAccessor.invokeAddDrawableChild(hardcorerevive$reviveButton);
                        hardcorerevive$fallbackTicks = 0;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[HardcoreRevive] " + e.getMessage());
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void hardcorerevive$syncReviveButtonActivity(CallbackInfo ci) {
        if (hardcorerevive$reviveButton == null) return;

        if (hardcorerevive$titleButton != null) {
            hardcorerevive$reviveButton.active = hardcorerevive$titleButton.active;
        } else {
            if (!hardcorerevive$reviveButton.active && ++hardcorerevive$fallbackTicks >= 20) {
                hardcorerevive$reviveButton.active = true;
            }
        }
    }

    @Unique
    private static boolean hardcorerevive$isSpectate(Text t) {
        return hardcorerevive$hasKey(t, "deathScreen.spectate");
    }

    @Unique
    private static boolean hardcorerevive$isTitle(Text t) {
        return hardcorerevive$hasKey(t, "deathScreen.titleScreen")
                || hardcorerevive$hasKey(t, "deathScreen.leaveServer")
                || hardcorerevive$hasKey(t, "gui.toTitle");
    }

    @Unique
    private static boolean hardcorerevive$hasKey(Text t, String key) {
        if (t.getContent() instanceof TranslatableTextContent tc) {
            return key.equals(tc.getKey());
        }
        return false;
    }
}