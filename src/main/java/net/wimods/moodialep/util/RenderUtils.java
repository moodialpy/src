/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.moodialep.util;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

public enum RenderUtils
{
	;
	
	private static final Minecraft MC = Minecraft.getInstance();
	
	/**
	 * Buffer source dedicato per MoodialEP, separato da quello di Minecraft.
	 * Questo evita che i nostri render partecipino al sorting dei trasparenti
	 * (foglie, acqua, vetro) causando cali di FPS.
	 */
	private static MultiBufferSource.BufferSource dedicatedBufferSource;
	
	public static MultiBufferSource.BufferSource getDedicatedVCP()
	{
		if(dedicatedBufferSource == null)
			dedicatedBufferSource = MultiBufferSource.immediateWithBuffers(
				new java.util.LinkedHashMap<>(), new ByteBufferBuilder(1536));
		return dedicatedBufferSource;
	}
	
	public static void endFrame()
	{
		if(dedicatedBufferSource != null)
			dedicatedBufferSource.endLastBatch();
	}
	
	public static Vec3 getCameraPos()
	{
		Camera camera = MC.gameRenderer.getMainCamera();
		if(camera == null)
			return Vec3.ZERO;
		
		return camera.position();
	}
}
