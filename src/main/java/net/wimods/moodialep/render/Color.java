/*
 * Adapted from the Meteor Client distribution
 * (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development. GPL-3.0-or-later.
 */
package net.wimods.moodialep.render;

/**
 * Minimal version of Meteor's Color class: 4 channels, 0-255 each.
 */
public class Color
{
	public int r, g, b, a;
	
	public Color(int r, int g, int b)
	{
		this(r, g, b, 255);
	}
	
	public Color(int r, int g, int b, int a)
	{
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
		validate();
	}
	
	public Color(Color color)
	{
		this(color.r, color.g, color.b, color.a);
	}
	
	/** From a 0xRRGGBB value plus a separate alpha (0-255). */
	public static Color fromRgb(int rgb, int alpha)
	{
		return new Color(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, alpha);
	}
	
	/** ARGB, the format the vertex consumers want. */
	public int getPacked()
	{
		return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
	}
	
	public void validate()
	{
		r = Math.max(0, Math.min(255, r));
		g = Math.max(0, Math.min(255, g));
		b = Math.max(0, Math.min(255, b));
		a = Math.max(0, Math.min(255, a));
	}
}
