/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.moodialep;

import net.fabricmc.api.ModInitializer;

public final class MoodialEpModInitializer implements ModInitializer
{
	private static MoodialEpMod instance;
	
	@Override
	public void onInitialize()
	{
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		
		if(instance != null)
			throw new RuntimeException("MoodialEpMod.onInitialize() ran twice!");
		
		instance = new MoodialEpMod();
	}
	
	public static MoodialEpMod getInstance()
	{
		return instance;
	}
}
