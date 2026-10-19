/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.moodialep;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.wimods.moodialep.MoodialEpPresets.Preset;

/**
 * Saved as config/moodialep.json. The settings windows are built by hand, so
 * the fields here do not need annotations.
 */
@Config(name = "moodialep")
public final class MoodialEpConfig implements ConfigData
{
	public boolean enable = true;
	
	/** Small "ESP" badge next to the hotbar while the ESP is on. */
	public boolean show_indicator = true;
	public MoodialEpIndicatorPosition indicator_position =
		MoodialEpIndicatorPosition.LEFT;
	
	/** Lines from the crosshair to each group of highlighted blocks. */
	public boolean tracers = false;
	
	public MoodialEpShapeMode shape_mode = MoodialEpShapeMode.BOTH;
	
	/** 0-100, how strong the colored sides of the boxes are. */
	public int fill_opacity = 15;
	
	/**
	 * Block ID -> RGB color of every block that gets highlighted. This is what
	 * the block picker edits. null = not set yet, see {@link #blocks()}.
	 */
	public Map<String, Integer> highlighted_blocks = null;
	
	// The three settings below are only kept to carry over the settings of
	// older versions into highlighted_blocks.
	
	/** Preset key -> switched on? (missing = preset's default) */
	public Map<String, Boolean> enabled_presets = new LinkedHashMap<>();
	
	/** Preset key -> RGB color (missing = preset's default) */
	public Map<String, Integer> preset_colors = new LinkedHashMap<>();
	
	/** Old text list: lines like "minecraft:spawner #FF00FF". */
	public java.util.List<String> custom_blocks = new ArrayList<>();
	
	/** RGB color for custom blocks that don't have their own color. */
	public int custom_color = 0x00FFC8;
	
	/**
	 * The blocks to highlight with their colors. The first time this is called
	 * on a config from an older version, the old preset/custom settings are
	 * converted.
	 */
	public Map<String, Integer> blocks()
	{
		if(highlighted_blocks == null)
			highlighted_blocks = migrateOldSettings();
		
		return highlighted_blocks;
	}
	
	private Map<String, Integer> migrateOldSettings()
	{
		Map<String, Integer> map = new LinkedHashMap<>();
		
		for(Preset preset : MoodialEpPresets.ALL)
			if(isPresetEnabled(preset))
				for(String id : preset.blocks())
					map.put(id, getPresetColor(preset));
				
		if(custom_blocks != null)
			for(String line : custom_blocks)
			{
				if(line == null || line.isBlank())
					continue;
				
				String[] parts = line.trim().split("[\\s;]+");
				Identifier id = Identifier.tryParse(parts[0].toLowerCase());
				if(id == null)
					continue;
				
				Integer color =
					parts.length > 1 ? MoodialEpResolvedConfig.parseColor(parts[1])
						: null;
				map.put(id.toString(),
					color != null ? color : custom_color & 0xFFFFFF);
			}
		
		return map;
	}
	
	public boolean isPresetEnabled(Preset preset)
	{
		return enabled_presets.getOrDefault(preset.key(),
			preset.defaultEnabled());
	}
	
	public int getPresetColor(Preset preset)
	{
		return preset_colors.getOrDefault(preset.key(), preset.defaultColor())
			& 0xFFFFFF;
	}
	
	@Override
	public void validatePostLoad()
	{
		if(shape_mode == null)
			shape_mode = MoodialEpShapeMode.BOTH;
		if(indicator_position == null)
			indicator_position = MoodialEpIndicatorPosition.LEFT;
		if(enabled_presets == null)
			enabled_presets = new LinkedHashMap<>();
		if(preset_colors == null)
			preset_colors = new LinkedHashMap<>();
		if(custom_blocks == null)
			custom_blocks = new ArrayList<>();
		custom_blocks.removeIf(line -> line == null);
		fill_opacity = Mth.clamp(fill_opacity, 0, 100);
		blocks().values().removeIf(c -> c == null);
	}
}
