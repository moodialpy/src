/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.moodialep;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.wimods.moodialep.esp.ESPBlockData;
import net.wimods.moodialep.render.Color;

/**
 * The settings of {@link MoodialEpConfig} turned into real {@link Block}s and
 * the {@link ESPBlockData} (style and colors) the BlockESP code draws with.
 */
public final class MoodialEpResolvedConfig
{
	/** Same alpha as the tracer color of Meteor's default block config. */
	private static final int TRACER_ALPHA = 125;
	
	private final Map<Block, ESPBlockData> data;
	private final Set<Block> blocks;
	
	private MoodialEpResolvedConfig(Map<Block, ESPBlockData> data)
	{
		this.data = Map.copyOf(data);
		this.blocks = Set.copyOf(data.keySet());
	}
	
	public Set<Block> blocks()
	{
		return blocks;
	}
	
	/** Look of every highlighted block ("block-configs" in Meteor). */
	public Map<Block, ESPBlockData> data()
	{
		return data;
	}
	
	/**
	 * Short text that changes whenever something that affects
	 * {@link #from(MoodialEpConfig)} changes.
	 */
	public static String signature(MoodialEpConfig c)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(c.shape_mode).append('|').append(c.fill_opacity).append('|')
			.append(c.tracers).append('|');
		for(Map.Entry<String, Integer> e : c.blocks().entrySet())
			sb.append(e.getKey()).append('=').append(e.getValue()).append(',');
		return sb.toString();
	}
	
	public static MoodialEpResolvedConfig from(MoodialEpConfig c)
	{
		Map<Block, Integer> colors = new HashMap<>();
		
		for(Map.Entry<String, Integer> e : c.blocks().entrySet())
		{
			Block block = e.getKey() == null ? null : findBlock(e.getKey());
			if(block == null)
			{
				MoodialEpMod.LOGGER.warn("Unknown block in config: '{}'",
					e.getKey());
				continue;
			}
			
			colors.put(block, e.getValue() & 0xFFFFFF);
		}
		
		int sideAlpha = Math.round(c.fill_opacity * 255 / 100f);
		Map<Block, ESPBlockData> data = new HashMap<>();
		for(Map.Entry<Block, Integer> e : colors.entrySet())
		{
			int rgb = e.getValue() & 0xFFFFFF;
			data.put(e.getKey(),
				new ESPBlockData(c.shape_mode, Color.fromRgb(rgb, 255),
					Color.fromRgb(rgb, sideAlpha), true,
					Color.fromRgb(rgb, TRACER_ALPHA)));
		}
		
		return new MoodialEpResolvedConfig(data);
	}
	
	static Block findBlock(String text)
	{
		Identifier id = Identifier.tryParse(text.trim().toLowerCase());
		if(id == null)
			return null;
		
		Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
		if(block == null || block == Blocks.AIR)
			return null;
		
		return block;
	}
	
	/** "#RRGGBB" or "RRGGBB" -> RGB, or null if it isn't a color. */
	static Integer parseColor(String text)
	{
		String s = text.startsWith("#") ? text.substring(1) : text;
		if(s.length() != 6)
			return null;
		
		try
		{
			return (int)Long.parseLong(s, 16);
			
		}catch(NumberFormatException e)
		{
			return null;
		}
	}
}
