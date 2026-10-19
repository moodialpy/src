/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.moodialep;

/**
 * Where the "ESP is on" badge is drawn, relative to the hotbar.
 */
public enum MoodialEpIndicatorPosition
{
	LEFT,
	RIGHT;
	
	@Override
	public String toString()
	{
		return "text.moodialep.indicator_position." + name().toLowerCase();
	}
}
