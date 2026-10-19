/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.moodialep;

import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.wimods.moodialep.esp.BlockESP;
import net.wimods.moodialep.render.Renderer3D;
import net.wimods.moodialep.util.RenderUtils;

/**
 * Standalone version of the BlockESP module of Meteor Client. The search and
 * the drawing are in the {@code esp} package (ported from Meteor), this class
 * connects them to Minecraft: it reads the config of the settings window,
 * switches the ESP on and off, and passes on the events Meteor's event bus
 * used to deliver (chunk loaded, block changed, tick, render).
 */
public final class MoodialEpMod
{
	private static final Minecraft MC = Minecraft.getInstance();
	public static final Logger LOGGER = LoggerFactory.getLogger("MoodialEP");
	
	private final ConfigHolder<MoodialEpConfig> configHolder;
	private final KeyMapping toggleKey;
	
	private final BlockESP blockEsp = BlockESP.get();
	private final Renderer3D renderer = new Renderer3D();
	
	private MoodialEpResolvedConfig resolved;
	private String lastSignature = null;
	private boolean needsActivate = true;
	private volatile int foundCount;
	private int tickCounter;
	private boolean enabled;
	
	// [perf] log line, see logPerf()
	private long perfWindowStart = System.nanoTime();
	private long perfRenderNanos;
	private int perfFrames;
	
	public MoodialEpMod()
	{
		LOGGER.info("Starting MoodialEP...");
		
		configHolder = AutoConfig.register(MoodialEpConfig.class,
			GsonConfigSerializer::new);
		
		KeyMapping.Category kbCategory = KeyMapping.Category
			.register(Identifier.fromNamespaceAndPath("moodialep", "moodialep"));
		toggleKey = KeyBindingHelper
			.registerKeyBinding(new KeyMapping("key.moodialep.toggle",
				InputConstants.UNKNOWN.getValue(), kbCategory));
		
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			boolean enabled = configHolder.get().enable;
			while(toggleKey.consumeClick())
				setEnabled(!enabled);
		});
		
		// ChunkDataEvent in Meteor
		ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
			if(enabled)
				blockEsp.onChunkData(chunk);
		});
		
		// Joining another world or server: everything gets searched again
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, level) -> {
			blockEsp.onDeactivate();
			foundCount = 0;
			needsActivate = true;
		});
		
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR,
			Identifier.fromNamespaceAndPath("moodialep", "indicator"),
			(graphics, deltaTracker) -> MoodialEpHud.render(graphics));
	}
	
	public void setEnabled(boolean enabled)
	{
		if(this.enabled == enabled)
			return;
		
		LOGGER.info("{} MoodialEP.", enabled ? "Enabling" : "Disabling");
		
		this.enabled = enabled;
		
		if(enabled)
		{
			// forces a fresh search of all chunks
			lastSignature = null;
			needsActivate = true;
		}else
		{
			blockEsp.deactivate();
			foundCount = 0;
		}
		
		if(configHolder.get().enable != enabled)
		{
			configHolder.get().enable = enabled;
			configHolder.save();
		}
	}
	
	/**
	 * Runs once per tick, from the player's tick method.
	 */
	public void onUpdate()
	{
		setEnabled(configHolder.get().enable);
		if(!isEnabled() || MC.level == null || MC.player == null)
			return;
		
		tickCounter++;
		
		// config changed (or just enabled) -> hand it to the ESP
		String signature =
			MoodialEpResolvedConfig.signature(configHolder.get());
		boolean configChanged = !signature.equals(lastSignature);
		if(configChanged)
		{
			lastSignature = signature;
			resolved = MoodialEpResolvedConfig.from(configHolder.get());
		}
		
		if(needsActivate)
		{
			// switch on with the current config, then search every chunk
			needsActivate = false;
			blockEsp.setConfig(resolved.blocks(), resolved.data(),
				configHolder.get().tracers);
			blockEsp.activate();
			
		}else if(configChanged)
			blockEsp.setConfig(resolved.blocks(), resolved.data(),
				configHolder.get().tracers);
		
		blockEsp.onPostTick();
		
		if(tickCounter % 10 == 0)
			foundCount = blockEsp.getBlockCount();
	}
	
	/** BlockUpdateEvent in Meteor, sent by LevelChunkMixin. */
	public void onBlockUpdate(BlockPos pos, BlockState oldState,
		BlockState newState)
	{
		if(enabled)
			blockEsp.onBlockUpdate(pos, oldState, newState);
	}
	
	public boolean shouldCancelViewBobbing()
	{
		return enabled && configHolder.get().tracers;
	}
	
	/**
	 * Render3DEvent in Meteor: collects everything the ESP wants to draw this
	 * frame and draws it.
	 */
	public void onRender(PoseStack matrixStack, float partialTicks,
		Matrix4f viewMatrix, Matrix4f projectionMatrix)
	{
		long start = System.nanoTime();
		
		Renderer3D.updateScreenCenter(projectionMatrix, viewMatrix,
			RenderUtils.getCameraPos());
		
		renderer.begin();
		blockEsp.onRender(renderer);
		renderer.render(matrixStack);
		
		perfRenderNanos += System.nanoTime() - start;
		perfFrames++;
		if(start - perfWindowStart >= 10_000_000_000L)
			logPerf(start);
	}
	
	/**
	 * One line in latest.log every 10 seconds while the ESP is on: how long
	 * the draw code takes per frame and how much time the search thread spent.
	 * Useful to find out where an FPS drop comes from.
	 */
	private void logPerf(long now)
	{
		long windowNanos = now - perfWindowStart;
		long[] scans = blockEsp.takeScanStats();
		
		LOGGER.info(
			"[perf] draw {} us/frame ({} frames) | highlighted={} types={} lines={} quads={} | searched {} chunks, {} ms total, worker busy {}%",
			perfFrames == 0 ? 0 : perfRenderNanos / perfFrames / 1000,
			perfFrames, foundCount, blockEsp.getSearchedBlockTypes(),
			renderer.getLineCount(), renderer.getQuadCount(), scans[0],
			scans[1] / 1_000_000, scans[1] * 100 / windowNanos);
		
		perfWindowStart = now;
		perfRenderNanos = 0;
		perfFrames = 0;
	}
	
	public static MoodialEpMod getInstance()
	{
		return MoodialEpModInitializer.getInstance();
	}
	
	public boolean isEnabled()
	{
		return enabled;
	}
	
	/** How many highlighted blocks there are right now. */
	public int getFoundCount()
	{
		return foundCount;
	}
	
	public ConfigHolder<MoodialEpConfig> getConfigHolder()
	{
		return configHolder;
	}
}
