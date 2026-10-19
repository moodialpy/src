/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.moodialep;

import java.util.ArrayList;
import java.util.List;

/**
 * Ready-made block groups that can be switched on in the config, so nobody
 * has to type block IDs for the common stuff.
 */
public enum MoodialEpPresets
{
	;
	
	/**
	 * @param key
	 *            used for the language file ("text.moodialep.preset.[key]") and
	 *            for saving the settings
	 * @param defaultColor
	 *            RGB
	 * @param blocks
	 *            block IDs that belong to this preset
	 */
	public record Preset(String key, int defaultColor, boolean defaultEnabled,
		List<String> blocks)
	{}
	
	public static final List<Preset> OVERWORLD = List.of(
		new Preset("coal", 0x8C8C8C, false,
			List.of("minecraft:coal_ore", "minecraft:deepslate_coal_ore")),
		new Preset("iron", 0xD8AF93, false,
			List.of("minecraft:iron_ore", "minecraft:deepslate_iron_ore")),
		new Preset("copper", 0xE77C56, false,
			List.of("minecraft:copper_ore", "minecraft:deepslate_copper_ore")),
		new Preset("gold", 0xFCEE4B, false,
			List.of("minecraft:gold_ore", "minecraft:deepslate_gold_ore")),
		new Preset("redstone", 0xFF2D2D, false, List.of(
			"minecraft:redstone_ore", "minecraft:deepslate_redstone_ore")),
		new Preset("lapis", 0x3B6CFF, false,
			List.of("minecraft:lapis_ore", "minecraft:deepslate_lapis_ore")),
		new Preset("diamond", 0x33EBCB, true,
			List.of("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore")),
		new Preset("emerald", 0x17DD62, false,
			List.of("minecraft:emerald_ore", "minecraft:deepslate_emerald_ore")));
	
	public static final List<Preset> NETHER = List.of(
		new Preset("nether_quartz", 0xF0E6D2, false,
			List.of("minecraft:nether_quartz_ore")),
		new Preset("nether_gold", 0xFCEE4B, false,
			List.of("minecraft:nether_gold_ore")),
		new Preset("ancient_debris", 0xFF8A3D, false,
			List.of("minecraft:ancient_debris")));
	
	public static final List<Preset> OTHER =
		List.of(new Preset("spawner", 0xB04BFF, false, List.of("minecraft:spawner")),
			new Preset("trial_spawner", 0x4BC6FF, false,
				List.of("minecraft:trial_spawner")),
			new Preset("budding_amethyst", 0xC77DFF, false,
				List.of("minecraft:budding_amethyst")));
	
	public static final List<Preset> ALL = all();
	
	private static List<Preset> all()
	{
		ArrayList<Preset> list = new ArrayList<>();
		list.addAll(OVERWORLD);
		list.addAll(NETHER);
		list.addAll(OTHER);
		return List.copyOf(list);
	}
}
