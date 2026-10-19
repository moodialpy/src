/*
 * Adapted from the Meteor Client distribution
 * (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development. GPL-3.0-or-later.
 */
package net.wimods.moodialep.esp;

import net.wimods.moodialep.MoodialEpShapeMode;
import net.wimods.moodialep.render.Color;

/**
 * How one kind of block is drawn: same fields as Meteor's ESPBlockData. The
 * Meteor settings/GUI code that was part of this class is gone, the config
 * window of MoodialEP fills these in (see MoodialEpResolvedConfig).
 */
public class ESPBlockData
{
	public MoodialEpShapeMode shapeMode;
	public Color lineColor;
	public Color sideColor;
	
	public boolean tracer;
	public Color tracerColor;
	
	public ESPBlockData(MoodialEpShapeMode shapeMode, Color lineColor,
		Color sideColor, boolean tracer, Color tracerColor)
	{
		this.shapeMode = shapeMode;
		this.lineColor = lineColor;
		this.sideColor = sideColor;
		
		this.tracer = tracer;
		this.tracerColor = tracerColor;
	}
	
	public ESPBlockData copy()
	{
		return new ESPBlockData(shapeMode, new Color(lineColor),
			new Color(sideColor), tracer, new Color(tracerColor));
	}
}
