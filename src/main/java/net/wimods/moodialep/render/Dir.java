/*
 * Adapted from the Meteor Client distribution
 * (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development. GPL-3.0-or-later.
 */
package net.wimods.moodialep.render;

/**
 * Bit flags used to leave out sides of a box (same values as Meteor's Dir).
 */
public class Dir
{
	public static final byte UP = 1 << 1;
	public static final byte DOWN = 1 << 2;
	public static final byte NORTH = 1 << 3;
	public static final byte SOUTH = 1 << 4;
	public static final byte WEST = 1 << 5;
	public static final byte EAST = 1 << 6;
	
	private Dir()
	{}
	
	public static boolean is(int dir, byte flag)
	{
		return (dir & flag) == flag;
	}
	
	public static boolean isNot(int dir, byte flag)
	{
		return (dir & flag) != flag;
	}
}
