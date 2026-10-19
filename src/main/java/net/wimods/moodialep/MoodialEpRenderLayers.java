/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.moodialep;

import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

public enum MoodialEpRenderLayers
{
	;
	
	/**
	 * Similar to {@link RenderType#getLines()}, but with line width 2.
	 */
	public static final RenderType LINES = RenderType.create("moodialep:lines",
		RenderSetup.builder(MoodialEpPipelines.DEPTH_TEST_LINES)
			.setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
			.setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
			.createRenderSetup());
	
	/**
	 * Similar to {@link RenderType#getLines()}, but with line width 2 and no
	 * depth test.
	 */
	public static final RenderType ESP_LINES =
		RenderType.create("moodialep:esp_lines",
			RenderSetup.builder(MoodialEpPipelines.ESP_LINES)
				.setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
				.setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
				.createRenderSetup());
	
	/**
	 * Similar to {@link RenderType#getDebugQuads()}, but with culling enabled.
	 * sortOnUpload() rimosso intenzionalmente: usiamo un buffer dedicato
	 * separato dal sorting dei trasparenti di Minecraft (foglie, acqua, vetro).
	 */
	public static final RenderType QUADS = RenderType.create("moodialep:quads",
		RenderSetup.builder(MoodialEpPipelines.QUADS)
			.createRenderSetup());
	
	/**
	 * Similar to {@link RenderType#getDebugQuads()}, but with culling enabled
	 * and no depth test.
	 * sortOnUpload() rimosso intenzionalmente: vedi sopra.
	 */
	public static final RenderType ESP_QUADS = RenderType.create(
		"moodialep:esp_quads",
		RenderSetup.builder(MoodialEpPipelines.ESP_QUADS)
			.createRenderSetup());
	
	/**
	 * Returns either {@link #QUADS} or {@link #ESP_QUADS} depending on the
	 * value of {@code depthTest}.
	 */
	public static RenderType getQuads(boolean depthTest)
	{
		return depthTest ? QUADS : ESP_QUADS;
	}
	
	/**
	 * Returns either {@link #LINES} or {@link #ESP_LINES} depending on the
	 * value of {@code depthTest}.
	 */
	public static RenderType getLines(boolean depthTest)
	{
		return depthTest ? LINES : ESP_LINES;
	}
}
