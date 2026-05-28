package com.enotiksergo.hardcorerevive.mixin;

import com.enotiksergo.hardcorerevive.client.ClientTerrainWaiter;
import com.enotiksergo.hardcorerevive.duck.ScreenExt;
import com.enotiksergo.hardcorerevive.net.ReviveNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public class DeathScreenMixin {

    @Unique private Button hardcorerevive$reviveButton;
    @Unique private Button hardcorerevive$titleButton;
    @Unique private int hardcorerevive$fallbackTicks;

    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void addRespawnButton(CallbackInfo ci) {
        DeathScreen screen = (DeathScreen) (Object) this;
        ScreenExt screenAccessor = (ScreenExt) screen;

        try {
            var client = Minecraft.getInstance();

            if (client != null && client.level != null && client.getSingleplayerServer() != null) {

                var server = client.getSingleplayerServer();
                var world = server.getLevel(client.level.dimension());

                if (world != null) {
                    var levelProperties = world.getLevelData();
                    boolean isHardcore = levelProperties.isHardcore();

                    if (isHardcore) {
                        int buttonY = screen.height / 4 + 144;
                        Button spectateButton = null;

                        for (var drawable : screenAccessor.getRenderables()) {
                            if (drawable instanceof Button button) {
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
                            screenAccessor.getRenderables().remove(spectateButton);
                            screenAccessor.getChildren().remove(spectateButton);
                        }

                        hardcorerevive$reviveButton = Button.builder(
                                Component.translatable("hardcorerevive.button.revive"),
                                button -> {
                                    if (client != null && client.player != null) {
                                        if (server == null) return;
                                        client.player.respawn();
                                        client.setScreen(null);
                                        ReviveNetworking.sendReviveRequest();
                                        ClientTerrainWaiter.startWaiting();
                                    }
                                }
                        ).bounds(screen.width / 2 - 100, buttonY, 200, 20).build();

                        hardcorerevive$reviveButton.active =
                                hardcorerevive$titleButton != null && hardcorerevive$titleButton.active;

                        screenAccessor.invokeAddRenderableWidget(hardcorerevive$reviveButton);
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
    private static boolean hardcorerevive$isSpectate(Component t) {
        return hardcorerevive$hasKey(t, "deathScreen.spectate");
    }

    @Unique
    private static boolean hardcorerevive$isTitle(Component t) {
        return hardcorerevive$hasKey(t, "deathScreen.titleScreen")
                || hardcorerevive$hasKey(t, "deathScreen.leaveServer")
                || hardcorerevive$hasKey(t, "gui.toTitle");
    }

    @Unique
    private static boolean hardcorerevive$hasKey(Component t, String key) {
        if (t.getContents() instanceof TranslatableContents tc) {
            return key.equals(tc.getKey());
        }
        return false;
    }
}