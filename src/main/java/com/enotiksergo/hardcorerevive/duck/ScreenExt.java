package com.enotiksergo.hardcorerevive.duck;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

import java.util.List;

public interface ScreenExt {
    <T extends GuiEventListener & Renderable & NarratableEntry> T invokeAddRenderableWidget(T widget);
    List<Renderable> getRenderables();
    List<GuiEventListener> getChildren();
}
