/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.moodialep;

/**
 * How a highlighted block is drawn. Same as ShapeMode (Lines, Sides, Both) in
 * Meteor Client, including the lines() / sides() helpers the BlockESP code
 * uses.
 */
public enum MoodialEpShapeMode
{
	LINES(true, false),
	SIDES(false, true),
	BOTH(true, true);
	
	private final boolean lines;
	private final boolean sides;
	
	private MoodialEpShapeMode(boolean lines, boolean sides)
	{
		this.lines = lines;
		this.sides = sides;
	}
	
	public boolean lines()
	{
		return lines;
	}
	
	public boolean sides()
	{
		return sides;
	}
	
	@Override
	public String toString()
	{
		return "text.autoconfig.moodialep.option.shape_mode."
			+ name().toLowerCase();
	}
}
