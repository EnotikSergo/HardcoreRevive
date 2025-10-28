package com.enotiksergo.hardcorerevive;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import java.util.BitSet;

public final class HardcoreHeartsFx {
    private HardcoreHeartsFx() {}

    private static final long FADE_MS = 360L;
    private static final long STAGGER_MS = 160L;
    private static final boolean REVERSE = false;
    private static final int PER_ROW = 10;

    private static final float EARLY_WINDOW = 0.45f;
    private static final float EARLY_AMP    = 0.28f;

    private static final float LATE_WINDOW  = 0.30f;
    private static final float LATE_AMP     = 0.12f;

    private static final Identifier HUD_ID = Identifier.of("hardcorerevive", "hearts_wave_xfade");

    private static final Identifier TEX_FULL = Identifier.of("minecraft", "textures/gui/sprites/hud/heart/full.png");
    private static final Identifier TEX_HALF = Identifier.of("minecraft", "textures/gui/sprites/hud/heart/half.png");
    private static final Identifier TEX_HC_FULL = Identifier.of("minecraft", "textures/gui/sprites/hud/heart/hardcore_full.png");
    private static final Identifier TEX_HC_HALF = Identifier.of("minecraft", "textures/gui/sprites/hud/heart/hardcore_half.png");

    private static volatile long startMs = -1L;
    private static volatile boolean active = false;
    private static volatile int plannedHearts = 0;
    private static volatile long finishAtMs = 0L;
    private static final BitSet popPlayed = new BitSet();

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, HUD_ID, HardcoreHeartsFx::render);
    }

    public static void begin() {
        var mc = MinecraftClient.getInstance();
        if (mc == null) return;

        mc.execute(() -> {
            ensureAttached();

            startMs = Util.getMeasuringTimeMs();
            active = true;

            popPlayed.clear();

            int heartsNow = 10;
            var p = mc.player;
            if (p != null) heartsNow = Math.max(1, MathHelper.ceil(p.getHealth() / 2f));
            plannedHearts = heartsNow;

            long tail = (plannedHearts > 0 ? (plannedHearts - 1L) * STAGGER_MS : 0L);
            finishAtMs = startMs + tail + FADE_MS;
        });
    }

    private static volatile boolean hudAttached = false;

    public static void ensureAttached() {
        if (hudAttached) return;
        hudAttached = true;
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, HUD_ID, HardcoreHeartsFx::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!active) return;

        var mc = MinecraftClient.getInstance();
        var p = mc != null ? mc.player : null;
        if (p == null) return;

        final int sw = mc.getWindow().getScaledWidth();
        final int sh = mc.getWindow().getScaledHeight();
        final int baseX = sw / 2 - 91;
        final int baseY = sh - 39;

        float hp = p.getHealth();
        int heartsToDraw = Math.max(1, MathHelper.ceil(hp / 2f));
        int fullHearts   = MathHelper.floor(hp / 2f);
        boolean half     = (hp % 2f) >= 1f;

        long now = Util.getMeasuringTimeMs();

        for (int i = 0; i < heartsToDraw; i++) {
            int order = REVERSE ? (heartsToDraw - 1 - i) : i;

            int row = i / PER_ROW;
            int col = i % PER_ROW;
            int x = baseX + col * 8;
            int y = baseY - row * 10;

            boolean isHalf = (i == fullHearts) && half;

            long heartStart = startMs + (long) order * STAGGER_MS;
            float localT = MathHelper.clamp((now - heartStart) / (float) FADE_MS, 0f, 1f);
            float smooth = easeInOutCubic(localT);

            float alphaHC = 1f - smooth;
            float alphaNR = smooth;

            float scaleHC = 1f, scaleNR = 1f;
            if (alphaHC >= alphaNR) {
                scaleHC = punchEarly(localT, EARLY_WINDOW, EARLY_AMP);
            } else {
                scaleNR = punchLate(localT, LATE_WINDOW, LATE_AMP);
                if (!popPlayed.get(order) && localT >= 1f - LATE_WINDOW) {
                    p.playSound(SoundEvents.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 0.7f, 1.10f);
                    popPlayed.set(order);
                }
            }

            if (alphaHC > 0f) {
                drawHeart(ctx, x, y, isHalf, true,  alphaHC, scaleHC);
            }
            if (alphaNR > 0f) {
                drawHeart(ctx, x, y, isHalf, false, alphaNR, scaleNR);
            }
        }

        if (now >= finishAtMs) {
            active = false;
            startMs = -1L;

            var m = MinecraftClient.getInstance();
            if (m != null && m.world != null) {
                var props = m.world.getLevelProperties();
                ((com.enotiksergo.hardcorerevive.duck.ClientWorldHardcoreDuck) props)
                        .hardcorerevive$setHardcore(false);
            }
        }
    }

    private static void drawHeart(DrawContext ctx, int x, int y, boolean half, boolean hardcore, float alpha, float scale) {
        final Identifier tex = hardcore
                ? (half ? TEX_HC_HALF : TEX_HC_FULL)
                : (half ? TEX_HALF    : TEX_FULL);

        final int a = MathHelper.clamp(Math.round(alpha * 255f), 0, 255);
        final int argb = ColorHelper.withAlpha(a, 0xFFFFFF);

        final float cx = x + 4.5f;
        final float cy = y + 4.5f;

        var m = ctx.getMatrices();
        boolean doScale = Math.abs(scale - 1f) > 1e-4f;
        if (doScale) {
            m.pushMatrix();
            m.translate(cx, cy);
            m.scale(scale, scale);
            m.translate(-cx, -cy);
        }

        ctx.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                tex,
                x, y,
                0, 0,
                9, 9,
                9, 9,
                argb
        );

        if (doScale) {
            m.popMatrix();
        }
    }

    private static float easeInOutCubic(float x) {
        return (x < 0.5f) ? 4f * x * x * x : 1f - (float) Math.pow(-2f * x + 2f, 3) / 2f;
    }

    private static float punchEarly(float t, float window, float amp) {
        if (t <= 0f || t >= window) return 1f;
        float k = t / window;
        float bell = (float) Math.sin(Math.PI * k) * (1f - 0.35f * k);
        return 1f + amp * bell;
    }

    private static float punchLate(float t, float window, float amp) {
        if (t <= 1f - window || t >= 1f) return 1f;
        float k = (t - (1f - window)) / window;
        float bell = (float) Math.sin(Math.PI * k) * (1f - 0.35f * k);
        return 1f + amp * bell;
    }
}