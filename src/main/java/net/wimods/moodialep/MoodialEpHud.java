/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.moodialep;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The small "E: n" text next to the hotbar that shows the ESP is switched on.
 */
public enum MoodialEpHud
{
	;
	
	private static final int HEIGHT = 14;
	private static final int TEXT = 0xFFFFFFFF;
	
	public static void render(GuiGraphics graphics)
	{
		MoodialEpMod mod = MoodialEpMod.getInstance();
		if(mod == null || !mod.isEnabled())
			return;
		
		MoodialEpConfig config = mod.getConfigHolder().get();
		if(!config.show_indicator)
			return;
		
		Minecraft mc = Minecraft.getInstance();
		if(mc.player == null)
			return;
		
		Font font = mc.font;
		int count = mod.getFoundCount();
		String text = "E: " + count;
		int width = font.width(text);
		
		int centerX = graphics.guiWidth() / 2;
		int y = graphics.guiHeight() - 22 + (22 - HEIGHT) / 2;
		
		int x;
		if(config.indicator_position == MoodialEpIndicatorPosition.RIGHT)
			x = centerX + 91 + 6;
		else
		{
			x = centerX - 91 - 6 - width;
			
			// make room for the off-hand slot
			if(!mc.player.getOffhandItem().isEmpty())
				x -= 29;
		}
		
		graphics.drawString(font, text, x, y + 3, TEXT, true);
	}
}