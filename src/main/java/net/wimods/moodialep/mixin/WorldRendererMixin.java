/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.moodialep.mixin;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.wimods.moodialep.MoodialEpMod;
import net.wimods.moodialep.util.RenderUtils;

@Mixin(LevelRenderer.class)
public abstract class WorldRendererMixin
	implements ResourceManagerReloadListener, AutoCloseable
{
	/**
	 * Iniettiamo il render di MoodialEP a RETURN di renderLevel.
	 *
	 * A questo punto Minecraft ha già:
	 *   1. Disegnato il terreno solido
	 *   2. Disegnato le entità
	 *   3. Disegnato i trasparenti (foglie, acqua, vetro) e fatto il loro sorting
	 *   4. Chiuso tutti i suoi buffer
	 *
	 * Disegnando qui con un buffer dedicato (getDedicatedVCP) e chiamando
	 * endFrame() subito dopo, i nostri render non entrano mai nel sorting
	 * dei trasparenti di Minecraft, eliminando il calo di FPS vicino alle foglie.
	 */
	@Inject(at = @At("RETURN"),
		method = "renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V")
	private void onRender(GraphicsResourceAllocator allocator,
		DeltaTracker tickCounter, boolean renderBlockOutline, Camera camera,
		Matrix4f positionMatrix, Matrix4f projectionMatrix, Matrix4f matrix4f2,
		GpuBufferSlice gpuBufferSlice, Vector4f vector4f, boolean bl,
		CallbackInfo ci)
	{
		MoodialEpMod mod = MoodialEpMod.getInstance();
		if(mod == null || !mod.isEnabled())
			return;
		
		PoseStack matrixStack = new PoseStack();
		matrixStack.mulPose(positionMatrix);
		float tickProgress = tickCounter.getGameTimeDeltaPartialTick(false);
		
		// Disegna tutto nel buffer dedicato
		mod.onRender(matrixStack, tickProgress, positionMatrix, projectionMatrix);
		
		// Flush immediato del buffer dedicato, completamente separato
		// dal pipeline di rendering di Minecraft
		RenderUtils.endFrame();
	}
}
