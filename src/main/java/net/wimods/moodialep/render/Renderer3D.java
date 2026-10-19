/*
 * Adapted from the Meteor Client distribution
 * (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development. GPL-3.0-or-later.
 */
package net.wimods.moodialep.render;

import java.util.Arrays;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;
import net.wimods.moodialep.MoodialEpRenderLayers;
import net.wimods.moodialep.MoodialEpShapeMode;
import net.wimods.moodialep.util.RenderUtils;

/**
 * Same drawing API as Meteor's Renderer3D (line, quad, quadVertical,
 * quadHorizontal, box, ...). Like Meteor, shapes are first collected in two
 * meshes (lines and triangles/quads) and drawn together once per frame, after
 * the world. The only difference is the last step: Meteor's GPU code
 * is written for a newer Minecraft, so here the meshes are written to the
 * Minecraft 1.21.11 buffers that MoodialEP already uses.
 *
 * <p>
 * All coordinates are world coordinates. They are made relative to the camera
 * when they are added, the same thing Meteor's MeshBuilder does.
 */
public final class Renderer3D
{
	private static final float LINE_WIDTH = 2;
	
	/**
	 * Point in the middle of the screen (world coordinates), where the
	 * tracers start. Same as RenderUtils.center in Meteor.
	 */
	public static Vec3 center = Vec3.ZERO;
	
	// lines: 2 points, 3 floats each
	private float[] lineCoords = new float[6 * 256];
	private int[] lineColors = new int[2 * 256];
	private int lineCount;
	
	// quads: 4 points, 3 floats each
	private float[] quadCoords = new float[12 * 256];
	private int[] quadColors = new int[4 * 256];
	private int quadCount;
	
	private double camX, camY, camZ;
	
	/**
	 * Same math as RenderUtils.updateScreenCenter() in Meteor: the point in
	 * the middle of the screen, taken back through the projection and view
	 * matrices.
	 */
	public static void updateScreenCenter(Matrix4f projection, Matrix4f view,
		Vec3 camera)
	{
		Matrix4f invProjection = new Matrix4f(projection).invert();
		Matrix4f invView = new Matrix4f(view).invert();
		
		Vector4f center4 =
			new Vector4f(0, 0, 0, 1).mul(invProjection).mul(invView);
		center4.div(center4.w);
		
		center = new Vec3(camera.x + center4.x, camera.y + center4.y,
			camera.z + center4.z);
	}
	
	public int getLineCount()
	{
		return lineCount;
	}
	
	public int getQuadCount()
	{
		return quadCount;
	}
	
	public void begin()
	{
		lineCount = 0;
		quadCount = 0;
		
		Vec3 camera = RenderUtils.getCameraPos();
		camX = camera.x;
		camY = camera.y;
		camZ = camera.z;
	}
	
	/**
	 * Draws everything added since {@link #begin()}. Lines first, then the
	 * quads, like Meteor.
	 */
	public void render(PoseStack matrices)
	{
		PoseStack.Pose entry = matrices.last();
		MultiBufferSource.BufferSource vcp = RenderUtils.getDedicatedVCP();
		
		if(lineCount > 0)
		{
			RenderType layer = MoodialEpRenderLayers.getLines(false);
			VertexConsumer buffer = vcp.getBuffer(layer);
			
			for(int i = 0; i < lineCount; i++)
			{
				int c = i * 6;
				writeLine(entry, buffer, lineCoords[c], lineCoords[c + 1],
					lineCoords[c + 2], lineCoords[c + 3], lineCoords[c + 4],
					lineCoords[c + 5], lineColors[i * 2], lineColors[i * 2 + 1]);
			}
			
			vcp.endBatch(layer);
		}
		
		if(quadCount > 0)
		{
			RenderType layer = MoodialEpRenderLayers.getQuads(false);
			VertexConsumer buffer = vcp.getBuffer(layer);
			
			for(int i = 0; i < quadCount; i++)
				for(int v = 0; v < 4; v++)
				{
					int c = i * 12 + v * 3;
					buffer
						.addVertex(entry, quadCoords[c], quadCoords[c + 1],
							quadCoords[c + 2])
						.setColor(quadColors[i * 4 + v]);
				}
			
			vcp.endBatch(layer);
		}
	}
	
	/**
	 * Lines in Minecraft's line shader are drawn from a start point in the
	 * direction of a normal, so every line needs its direction. Lines that
	 * go through the camera get one more point there, because the shader
	 * breaks otherwise (same workaround Wurst uses).
	 */
	private static void writeLine(PoseStack.Pose entry, VertexConsumer buffer,
		float x1, float y1, float z1, float x2, float y2, float z2, int color1,
		int color2)
	{
		Vector3f dir = new Vector3f(x2, y2, z2).sub(x1, y1, z1);
		float length = dir.length();
		if(length < 1e-6f)
			return;
		
		Vector3f normal = dir.div(length);
		
		buffer.addVertex(entry, x1, y1, z1).setColor(color1)
			.setNormal(entry, normal).setLineWidth(LINE_WIDTH);
		
		float t = new Vector3f(x1, y1, z1).negate().dot(normal);
		if(t > 0 && t < length)
		{
			Vector3f closeToCam = new Vector3f(normal).mul(t).add(x1, y1, z1);
			int color = lerpColor(color1, color2, t / length);
			buffer.addVertex(entry, closeToCam).setColor(color)
				.setNormal(entry, normal).setLineWidth(LINE_WIDTH);
			buffer.addVertex(entry, closeToCam).setColor(color)
				.setNormal(entry, normal).setLineWidth(LINE_WIDTH);
		}
		
		buffer.addVertex(entry, x2, y2, z2).setColor(color2)
			.setNormal(entry, normal).setLineWidth(LINE_WIDTH);
	}
	
	private static int lerpColor(int c1, int c2, float t)
	{
		if(c1 == c2)
			return c1;
		
		int a = (int)(((c1 >>> 24) & 255) * (1 - t) + ((c2 >>> 24) & 255) * t);
		int r = (int)(((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
		int g = (int)(((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
		int b = (int)((c1 & 255) * (1 - t) + (c2 & 255) * t);
		return a << 24 | r << 16 | g << 8 | b;
	}
	
	// Lines
	
	public void line(double x1, double y1, double z1, double x2, double y2,
		double z2, Color color1, Color color2)
	{
		if(lineCount * 6 + 6 > lineCoords.length)
		{
			lineCoords = Arrays.copyOf(lineCoords, lineCoords.length * 2);
			lineColors = Arrays.copyOf(lineColors, lineColors.length * 2);
		}
		
		int c = lineCount * 6;
		lineCoords[c] = (float)(x1 - camX);
		lineCoords[c + 1] = (float)(y1 - camY);
		lineCoords[c + 2] = (float)(z1 - camZ);
		lineCoords[c + 3] = (float)(x2 - camX);
		lineCoords[c + 4] = (float)(y2 - camY);
		lineCoords[c + 5] = (float)(z2 - camZ);
		lineColors[lineCount * 2] = color1.getPacked();
		lineColors[lineCount * 2 + 1] = color2.getPacked();
		lineCount++;
	}
	
	public void line(double x1, double y1, double z1, double x2, double y2,
		double z2, Color color)
	{
		line(x1, y1, z1, x2, y2, z2, color, color);
	}
	
	public void boxLines(double x1, double y1, double z1, double x2, double y2,
		double z2, Color color, int excludeDir)
	{
		if(excludeDir == 0)
		{
			// Bottom to top
			line(x1, y1, z1, x1, y2, z1, color);
			line(x1, y1, z2, x1, y2, z2, color);
			line(x2, y1, z1, x2, y2, z1, color);
			line(x2, y1, z2, x2, y2, z2, color);
			
			// Bottom loop
			line(x1, y1, z1, x1, y1, z2, color);
			line(x2, y1, z1, x2, y1, z2, color);
			line(x1, y1, z1, x2, y1, z1, color);
			line(x1, y1, z2, x2, y1, z2, color);
			
			// Top loop
			line(x1, y2, z1, x1, y2, z2, color);
			line(x2, y2, z1, x2, y2, z2, color);
			line(x1, y2, z1, x2, y2, z1, color);
			line(x1, y2, z2, x2, y2, z2, color);
		}else
		{
			// Bottom to top
			if(Dir.isNot(excludeDir, Dir.WEST)
				&& Dir.isNot(excludeDir, Dir.NORTH))
				line(x1, y1, z1, x1, y2, z1, color);
			if(Dir.isNot(excludeDir, Dir.WEST)
				&& Dir.isNot(excludeDir, Dir.SOUTH))
				line(x1, y1, z2, x1, y2, z2, color);
			if(Dir.isNot(excludeDir, Dir.EAST)
				&& Dir.isNot(excludeDir, Dir.NORTH))
				line(x2, y1, z1, x2, y2, z1, color);
			if(Dir.isNot(excludeDir, Dir.EAST)
				&& Dir.isNot(excludeDir, Dir.SOUTH))
				line(x2, y1, z2, x2, y2, z2, color);
			
			// Bottom loop
			if(Dir.isNot(excludeDir, Dir.WEST)
				&& Dir.isNot(excludeDir, Dir.DOWN))
				line(x1, y1, z1, x1, y1, z2, color);
			if(Dir.isNot(excludeDir, Dir.EAST)
				&& Dir.isNot(excludeDir, Dir.DOWN))
				line(x2, y1, z1, x2, y1, z2, color);
			if(Dir.isNot(excludeDir, Dir.NORTH)
				&& Dir.isNot(excludeDir, Dir.DOWN))
				line(x1, y1, z1, x2, y1, z1, color);
			if(Dir.isNot(excludeDir, Dir.SOUTH)
				&& Dir.isNot(excludeDir, Dir.DOWN))
				line(x1, y1, z2, x2, y1, z2, color);
			
			// Top loop
			if(Dir.isNot(excludeDir, Dir.WEST) && Dir.isNot(excludeDir, Dir.UP))
				line(x1, y2, z1, x1, y2, z2, color);
			if(Dir.isNot(excludeDir, Dir.EAST) && Dir.isNot(excludeDir, Dir.UP))
				line(x2, y2, z1, x2, y2, z2, color);
			if(Dir.isNot(excludeDir, Dir.NORTH)
				&& Dir.isNot(excludeDir, Dir.UP))
				line(x1, y2, z1, x2, y2, z1, color);
			if(Dir.isNot(excludeDir, Dir.SOUTH)
				&& Dir.isNot(excludeDir, Dir.UP))
				line(x1, y2, z2, x2, y2, z2, color);
		}
	}
	
	// Quads
	
	public void quad(double x1, double y1, double z1, double x2, double y2,
		double z2, double x3, double y3, double z3, double x4, double y4,
		double z4, Color topLeft, Color topRight, Color bottomRight,
		Color bottomLeft)
	{
		if(quadCount * 12 + 12 > quadCoords.length)
		{
			quadCoords = Arrays.copyOf(quadCoords, quadCoords.length * 2);
			quadColors = Arrays.copyOf(quadColors, quadColors.length * 2);
		}
		
		// same vertex order as Meteor: bottomLeft, topLeft, topRight,
		// bottomRight
		int c = quadCount * 12;
		putPoint(quadCoords, c, x1, y1, z1);
		putPoint(quadCoords, c + 3, x2, y2, z2);
		putPoint(quadCoords, c + 6, x3, y3, z3);
		putPoint(quadCoords, c + 9, x4, y4, z4);
		quadColors[quadCount * 4] = bottomLeft.getPacked();
		quadColors[quadCount * 4 + 1] = topLeft.getPacked();
		quadColors[quadCount * 4 + 2] = topRight.getPacked();
		quadColors[quadCount * 4 + 3] = bottomRight.getPacked();
		quadCount++;
	}
	
	private void putPoint(float[] array, int index, double x, double y,
		double z)
	{
		array[index] = (float)(x - camX);
		array[index + 1] = (float)(y - camY);
		array[index + 2] = (float)(z - camZ);
	}
	
	public void quad(double x1, double y1, double z1, double x2, double y2,
		double z2, double x3, double y3, double z3, double x4, double y4,
		double z4, Color color)
	{
		quad(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, color, color,
			color, color);
	}
	
	public void quadVertical(double x1, double y1, double z1, double x2,
		double y2, double z2, Color color)
	{
		quad(x1, y1, z1, x1, y2, z1, x2, y2, z2, x2, y1, z2, color);
	}
	
	public void quadHorizontal(double x1, double y, double z1, double x2,
		double z2, Color color)
	{
		quad(x1, y, z1, x1, y, z2, x2, y, z2, x2, y, z1, color);
	}
	
	// Boxes
	
	public void boxSides(double x1, double y1, double z1, double x2, double y2,
		double z2, Color color, int excludeDir)
	{
		// West
		if(excludeDir == 0 || Dir.isNot(excludeDir, Dir.WEST))
			quad(x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, color);
		// East
		if(excludeDir == 0 || Dir.isNot(excludeDir, Dir.EAST))
			quad(x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, color);
		// North
		if(excludeDir == 0 || Dir.isNot(excludeDir, Dir.NORTH))
			quad(x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, color);
		// South
		if(excludeDir == 0 || Dir.isNot(excludeDir, Dir.SOUTH))
			quad(x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, color);
		// Bottom
		if(excludeDir == 0 || Dir.isNot(excludeDir, Dir.DOWN))
			quad(x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, color);
		// Top
		if(excludeDir == 0 || Dir.isNot(excludeDir, Dir.UP))
			quad(x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, color);
	}
	
	public void box(double x1, double y1, double z1, double x2, double y2,
		double z2, Color sideColor, Color lineColor, MoodialEpShapeMode mode,
		int excludeDir)
	{
		if(mode.lines())
			boxLines(x1, y1, z1, x2, y2, z2, lineColor, excludeDir);
		if(mode.sides())
			boxSides(x1, y1, z1, x2, y2, z2, sideColor, excludeDir);
	}
}
