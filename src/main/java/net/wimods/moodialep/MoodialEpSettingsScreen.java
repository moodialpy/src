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

import me.shedaniel.autoconfig.ConfigHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * The general settings (shape, opacity, tracers, badge), in the same style as
 * {@link MoodialEpBlockPickerScreen}. Opened from the block picker.
 */
public final class MoodialEpSettingsScreen extends Screen
{
	private static final int ROW_H = 28;
	private static final int CONTROL_W = 150;
	private static final int ROWS = 5;
	
	private final Screen parent;
	private final ConfigHolder<MoodialEpConfig> holder;
	private final MoodialEpConfig config;
	
	private final List<Component> labels = new ArrayList<>();
	private int panelX;
	private int panelY;
	private int panelW;
	private int panelH;
	
	private Button positionButton;
	
	public MoodialEpSettingsScreen(Screen parent)
	{
		super(Component.translatable("text.moodialep.settings.title"));
		this.parent = parent;
		holder = MoodialEpMod.getInstance().getConfigHolder();
		config = holder.get();
	}
	
	private static Component text(String key)
	{
		return Component.translatable("text.moodialep." + key);
	}
	
	private static Component onOff(boolean on)
	{
		return Component.translatable(on ? "options.on" : "options.off")
			.withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
	}
	
	private static Component enumName(Object value)
	{
		return Component.translatable(value.toString());
	}
	
	@Override
	protected void init()
	{
		labels.clear();
		
		panelW = Math.min(width - 20, 380);
		panelX = (width - panelW) / 2;
		panelY = 38;
		panelH = ROWS * ROW_H + 8;
		
		int controlX = panelX + panelW - CONTROL_W - 8;
		int y = panelY + 4;
		
		// highlight style
		labels.add(text("shape"));
		Button shape = Button
			.builder(enumName(config.shape_mode), b -> {
				MoodialEpShapeMode[] all = MoodialEpShapeMode.values();
				config.shape_mode = all[(config.shape_mode.ordinal() + 1)
					% all.length];
				b.setMessage(enumName(config.shape_mode));
			}).bounds(controlX, y + 4, CONTROL_W, 20).build();
		shape.setTooltip(Tooltip.create(text("shape.tooltip")));
		addRenderableWidget(shape);
		y += ROW_H;
		
		// fill strength
		labels.add(text("fill_opacity"));
		AbstractSliderButton slider =
			new OpacitySlider(controlX, y + 4, CONTROL_W, 20);
		slider.setTooltip(Tooltip.create(text("fill_opacity.tooltip")));
		addRenderableWidget(slider);
		y += ROW_H;
		
		// tracers
		labels.add(text("tracers"));
		Button tracers = Button.builder(onOff(config.tracers), b -> {
			config.tracers = !config.tracers;
			b.setMessage(onOff(config.tracers));
		}).bounds(controlX, y + 4, CONTROL_W, 20).build();
		tracers.setTooltip(Tooltip.create(text("tracers.tooltip")));
		addRenderableWidget(tracers);
		y += ROW_H;
		
		// badge on/off
		labels.add(text("indicator"));
		Button indicator =
			Button.builder(onOff(config.show_indicator), b -> {
				config.show_indicator = !config.show_indicator;
				b.setMessage(onOff(config.show_indicator));
				positionButton.active = config.show_indicator;
			}).bounds(controlX, y + 4, CONTROL_W, 20).build();
		indicator.setTooltip(Tooltip.create(text("indicator.tooltip")));
		addRenderableWidget(indicator);
		y += ROW_H;
		
		// badge side
		labels.add(text("indicator_position"));
		positionButton = Button
			.builder(enumName(config.indicator_position), b -> {
				MoodialEpIndicatorPosition[] all =
					MoodialEpIndicatorPosition.values();
				config.indicator_position =
					all[(config.indicator_position.ordinal() + 1) % all.length];
				b.setMessage(enumName(config.indicator_position));
			}).bounds(controlX, y + 4, CONTROL_W, 20).build();
		positionButton.active = config.show_indicator;
		addRenderableWidget(positionButton);
		
		addRenderableWidget(Button
			.builder(CommonComponents.GUI_DONE, b -> onClose())
			.bounds(width / 2 - 100, height - 26, 200, 20).build());
	}
	
	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick)
	{
		super.render(g, mouseX, mouseY, partialTick);
		
		g.drawCenteredString(font, getTitle(), width / 2, 6, 0xFFFFFFFF);
		g.drawString(font, text("settings.hint"), panelX, 26, 0xFFAAAAAA,
			false);
		
		// row labels (the panel itself is drawn in renderBackground)
		for(int i = 0; i < labels.size(); i++)
		{
			int rowY = panelY + 4 + i * ROW_H;
			g.drawString(font, labels.get(i), panelX + 10,
				rowY + (ROW_H - 9) / 2, 0xFFFFFFFF, false);
		}
	}
	
	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY,
		float partialTick)
	{
		super.renderBackground(g, mouseX, mouseY, partialTick);
		
		// same dark panel as the block list, drawn behind the widgets
		g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xA0000000);
		for(int i = 1; i < ROWS; i++)
		{
			int lineY = panelY + 4 + i * ROW_H;
			g.fill(panelX + 6, lineY, panelX + panelW - 6, lineY + 1,
				0x20FFFFFF);
		}
	}
	
	@Override
	public void onClose()
	{
		minecraft.setScreen(parent);
	}
	
	@Override
	public void removed()
	{
		holder.save();
	}
	
	private final class OpacitySlider extends AbstractSliderButton
	{
		OpacitySlider(int x, int y, int width, int height)
		{
			super(x, y, width, height, Component.empty(),
				config.fill_opacity / 100.0);
			updateMessage();
		}
		
		@Override
		protected void updateMessage()
		{
			setMessage(Component.literal(Math.round(value * 100) + "%"));
		}
		
		@Override
		protected void applyValue()
		{
			config.fill_opacity = (int)Math.round(value * 100);
		}
	}
}
