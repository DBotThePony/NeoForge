/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;

/**
 * Adaptation of {@link LayeredDraw} that is used for {@link Gui} rendering specifically,
 * to give layers a name and fire appropriate events.
 *
 * <p>Overlays can be registered using the {@link RegisterGuiLayersEvent} event.
 */
@ApiStatus.Internal
public class GuiLayerManager {
    public static final float Z_SEPARATION = LayeredDraw.Z_SEPARATION;
    private final List<NamedLayer> layers = new ArrayList<>();
    private boolean initialized = false;

    public record NamedLayer(ResourceLocation name, LayeredDraw.Layer layer, BooleanSupplier shouldRender) {}

    public GuiLayerManager add(ResourceLocation name, LayeredDraw.Layer layer) {
        return this.add(name, layer, () -> true);
    }

    public GuiLayerManager add(ResourceLocation name, LayeredDraw.Layer layer, BooleanSupplier shouldRender) {
        this.layers.add(new NamedLayer(name, layer, shouldRender));
        return this;
    }

    public GuiLayerManager add(GuiLayerManager child, BooleanSupplier shouldRender) {
        // Flatten the layers to allow mods to insert layers between vanilla layers.
        for (var entry : child.layers) {
            var shouldRenderLayer = entry.shouldRender();
            add(entry.name(), entry.layer(), () -> shouldRender.getAsBoolean() && shouldRenderLayer.getAsBoolean());
        }

        return this;
    }

    public void render(GuiGraphics guiGraphics, DeltaTracker partialTick) {
        if (!NeoForge.EVENT_BUS.post(new RenderGuiEvent.Pre(guiGraphics, partialTick)).isCanceled()) {
            guiGraphics.pose().pushPose();

            for (var layer : this.layers) {
                var shouldRender = layer.shouldRender().getAsBoolean();

                // TODO: Reconsider this behavior in future version during breaking change window
                if (!NeoForge.EVENT_BUS.post(new RenderGuiLayerEvent.Pre(guiGraphics, partialTick, layer.name(), layer.layer(), shouldRender)).isCanceled()) {
                    if (shouldRender) layer.layer().render(guiGraphics, partialTick);
                    NeoForge.EVENT_BUS.post(new RenderGuiLayerEvent.Post(guiGraphics, partialTick, layer.name(), layer.layer(), shouldRender));
                }

                guiGraphics.pose().translate(0.0F, 0.0F, Z_SEPARATION);
            }

            guiGraphics.pose().popPose();

            NeoForge.EVENT_BUS.post(new RenderGuiEvent.Post(guiGraphics, partialTick));
        }
    }

    public void initModdedLayers() {
        if (initialized) {
            throw new IllegalStateException("Duplicate initialization of NamedLayeredDraw");
        }

        initialized = true;
        ModLoader.postEvent(new RegisterGuiLayersEvent(this.layers));
    }

    public int getLayerCount() {
        return this.layers.size();
    }
}
