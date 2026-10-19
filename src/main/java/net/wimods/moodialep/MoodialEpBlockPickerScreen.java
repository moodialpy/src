/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.moodialep;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import me.shedaniel.autoconfig.ConfigHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.wimods.moodialep.MoodialEpPresets.Preset;

/**
 * The main settings window (opened from Mod Menu): search for a block, click
 * it to highlight it, click the colored square to pick a color from the
 * palette or type your own #RRGGBB code. No block IDs to type.
 */
public final class MoodialEpBlockPickerScreen extends Screen
{
	private static final int ROW_H = 24;
	private static final int SCROLLBAR_W = 6;
	private static final int DEFAULT_COLOR = 0x00FFC8;
	
	private static final int[] PALETTE = {0xFF2D2D, 0xFF8A3D, 0xFCEE4B,
		0x17DD62, 0x33EBCB, 0x3B6CFF, 0xB04BFF, 0xFF5CC8, 0xFFFFFF, 0xB0B0B0,
		0x6B6B6B, 0x6B3E26, 0xD8AF93, 0x00FFC8, 0x4BC6FF, 0xC77DFF};
	private static final int PALETTE_COLS = 8;
	private static final int CELL = 16;
	private static final int GAP = 2;
	private static final int HEX_H = 16;
	
	/**
	 * One row of the list: a single block, or a group like "Coal" that
	 * contains the normal and the deepslate ore.
	 */
	private static final class Entry
	{
		final String name;
		final List<String> ids;
		final ItemStack icon;
		final String search;
		final String idText;
		final int defaultColor;
		
		Entry(String name, List<Block> blocks, int defaultColor)
		{
			this.name = name;
			this.defaultColor = defaultColor;
			
			List<String> list = new ArrayList<>();
			for(Block block : blocks)
				list.add(BuiltInRegistries.BLOCK.getKey(block).toString());
			ids = List.copyOf(list);
			
			icon = new ItemStack(blocks.get(0).asItem());
			idText = String.join(", ", ids);
			search = (name + " " + idText).toLowerCase(Locale.ROOT);
		}
	}
	
	private final Screen parent;
	private final ConfigHolder<MoodialEpConfig> holder;
	private final MoodialEpConfig config;
	private final Map<String, Integer> selected;
	private final List<Entry> all;
	private List<Entry> visible = List.of();
	
	private String searchText = "";
	private boolean onlySelected;
	private double scroll;
	private boolean draggingBar;
	
	// color palette popup
	private Entry popup;
	private int popupX;
	private int popupY;
	private EditBox hexBox;
	
	private EditBox searchBox;
	private int listX;
	private int listY;
	private int listW;
	private int listH;
	
	public MoodialEpBlockPickerScreen(Screen parent)
	{
		super(Component.translatable("text.moodialep.title"));
		this.parent = parent;
		holder = MoodialEpMod.getInstance().getConfigHolder();
		config = holder.get();
		selected = config.blocks();
		all = buildEntries();
	}
	
	private static Component text(String key)
	{
		return Component.translatable("text.moodialep." + key);
	}
	
	// ---------------------------------------------------------------- data
	
	private static List<Entry> buildEntries()
	{
		List<Entry> groups = new ArrayList<>();
		Set<Block> grouped = new HashSet<>();
		
		for(Preset preset : MoodialEpPresets.ALL)
		{
			List<Block> blocks = new ArrayList<>();
			for(String id : preset.blocks())
			{
				Block block = MoodialEpResolvedConfig.findBlock(id);
				if(block != null)
					blocks.add(block);
			}
			
			if(blocks.isEmpty())
				continue;
			
			grouped.addAll(blocks);
			groups.add(new Entry(
				text("preset." + preset.key()).getString(), blocks,
				preset.defaultColor()));
		}
		
		List<Entry> singles = new ArrayList<>();
		for(Block block : BuiltInRegistries.BLOCK)
		{
			if(block == Blocks.AIR || grouped.contains(block))
				continue;
			
			// technical blocks without an item (fire, plants of vines...)
			if(block.asItem() == Items.AIR)
				continue;
			
			singles.add(
				new Entry(block.getName().getString(), List.of(block),
					DEFAULT_COLOR));
		}
		singles.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.name,
			b.name));
		
		List<Entry> list = new ArrayList<>(groups);
		list.addAll(singles);
		return list;
	}
	
	private boolean isOn(Entry e)
	{
		for(String id : e.ids)
			if(selected.containsKey(id))
				return true;
		return false;
	}
	
	private int colorOf(Entry e)
	{
		for(String id : e.ids)
		{
			Integer color = selected.get(id);
			if(color != null)
				return color & 0xFFFFFF;
		}
		return e.defaultColor & 0xFFFFFF;
	}
	
	private void setOn(Entry e, boolean on)
	{
		int color = colorOf(e);
		for(String id : e.ids)
			if(on)
				selected.put(id, color);
			else
				selected.remove(id);
	}
	
	private void setColor(Entry e, int rgb)
	{
		for(String id : e.ids)
			selected.put(id, rgb & 0xFFFFFF);
	}
	
	private void refilter()
	{
		String[] words = searchText.toLowerCase(Locale.ROOT).trim().split("\\s+");
		List<Entry> result = new ArrayList<>();
		
		for(Entry e : all)
		{
			if(onlySelected && !isOn(e))
				continue;
			
			boolean match = true;
			for(String word : words)
				if(!word.isEmpty() && !e.search.contains(word))
				{
					match = false;
					break;
				}
			
			if(match)
				result.add(e);
		}
		
		visible = result;
		clampScroll();
	}
	
	// -------------------------------------------------------------- layout
	
	@Override
	protected void init()
	{
		popup = null;
		hexBox = null;
		
		listW = Math.min(width - 20, 380);
		listX = (width - listW) / 2;
		listY = 78;
		listH = Math.max(ROW_H * 2, height - 32 - listY);
		
		int half = (listW - 4) / 2;
		
		addRenderableWidget(Button
			.builder(enableLabel(), b -> {
				MoodialEpMod.getInstance().setEnabled(!config.enable);
				b.setMessage(enableLabel());
			}).bounds(listX, 18, half, 20).build());
		
		addRenderableWidget(Button
			.builder(text("picker.settings"),
				b -> minecraft.setScreen(new MoodialEpSettingsScreen(this)))
			.bounds(listX + half + 4, 18, half, 20).build());
		
		searchBox = new EditBox(font, listX, 42, listW - 104, 20,
			text("picker.search"));
		searchBox.setHint(text("picker.search.hint"));
		searchBox.setMaxLength(64);
		searchBox.setValue(searchText);
		searchBox.setResponder(s -> {
			searchText = s;
			scroll = 0;
			refilter();
		});
		addRenderableWidget(searchBox);
		
		addRenderableWidget(Button.builder(filterLabel(), b -> {
			onlySelected = !onlySelected;
			b.setMessage(filterLabel());
			scroll = 0;
			refilter();
		}).bounds(listX + listW - 100, 42, 100, 20).build());
		
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE,
			b -> onClose()).bounds(width / 2 - 100, height - 26, 200, 20)
			.build());
		
		setInitialFocus(searchBox);
		refilter();
	}
	
	private Component enableLabel()
	{
		boolean on = config.enable;
		return Component.literal("MoodialEP: ")
			.append(Component.translatable(on ? "options.on" : "options.off")
				.withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
	}
	
	private Component filterLabel()
	{
		return text(onlySelected ? "picker.filter.selected"
			: "picker.filter.all");
	}
	
	private int maxScroll()
	{
		return Math.max(0, visible.size() * ROW_H - listH);
	}
	
	private void clampScroll()
	{
		scroll = Math.max(0, Math.min(scroll, maxScroll()));
	}
	
	private String fit(String s, int maxWidth)
	{
		if(font.width(s) <= maxWidth)
			return s;
		
		while(s.length() > 1 && font.width(s + "...") > maxWidth)
			s = s.substring(0, s.length() - 1);
		return s + "...";
	}
	
	// ----------------------------------------------------------- rendering
	
	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick)
	{
		super.render(g, mouseX, mouseY, partialTick);
		
		Font f = font;
		g.drawCenteredString(f, getTitle(), width / 2, 6, 0xFFFFFFFF);
		g.drawString(f, fit(text("picker.hint").getString(), listW),
			listX, 66, 0xFFAAAAAA, false);
		
		g.fill(listX, listY, listX + listW, listY + listH, 0xA0000000);
		
		if(visible.isEmpty())
			g.drawCenteredString(f, text("picker.empty"), width / 2,
				listY + listH / 2 - 4, 0xFFAAAAAA);
		
		boolean canHover = popup == null;
		
		g.enableScissor(listX, listY, listX + listW, listY + listH);
		int first = (int)(scroll / ROW_H);
		for(int i = first; i < visible.size(); i++)
		{
			int y = listY + i * ROW_H - (int)scroll;
			if(y >= listY + listH)
				break;
			
			renderRow(g, visible.get(i), y, mouseX, mouseY, canHover);
		}
		g.disableScissor();
		
		renderScrollbar(g);
		
		if(popup != null)
			renderPopup(g, mouseX, mouseY, partialTick);
	}
	
	private void renderRow(GuiGraphics g, Entry e, int y, int mouseX,
		int mouseY, boolean canHover)
	{
		boolean on = isOn(e);
		int right = listX + listW - SCROLLBAR_W;
		
		boolean hover = canHover && mouseX >= listX && mouseX < right
			&& mouseY >= Math.max(y, listY)
			&& mouseY < Math.min(y + ROW_H, listY + listH);
		
		int rgb = colorOf(e);
		if(on)
			g.fill(listX, y, right, y + ROW_H - 1, 0x38000000 | rgb);
		if(hover)
			g.fill(listX, y, right, y + ROW_H - 1, 0x28FFFFFF);
		
		// checkbox
		int cx = listX + 6;
		int cy = y + 6;
		g.fill(cx, cy, cx + 12, cy + 12, 0xFFFFFFFF);
		g.fill(cx + 1, cy + 1, cx + 11, cy + 11, 0xFF202020);
		if(on)
			g.fill(cx + 3, cy + 3, cx + 9, cy + 9, 0xFF000000 | rgb);
		
		g.renderItem(e.icon, listX + 24, y + 4);
		
		int textX = listX + 46;
		int textW = right - textX - (on ? 28 : 6);
		g.drawString(font, fit(e.name, textW), textX, y + 3, 0xFFFFFFFF, false);
		g.drawString(font, fit(e.idText, textW), textX, y + 13, 0xFF888888,
			false);
		
		if(on)
		{
			int sx = swatchX();
			g.fill(sx, y + 4, sx + 16, y + 20, 0xFFFFFFFF);
			g.fill(sx + 1, y + 5, sx + 15, y + 19, 0xFF000000 | rgb);
		}
	}
	
	private int swatchX()
	{
		return listX + listW - SCROLLBAR_W - 22;
	}
	
	private void renderScrollbar(GuiGraphics g)
	{
		int max = maxScroll();
		if(max <= 0)
			return;
		
		int x = listX + listW - SCROLLBAR_W;
		g.fill(x, listY, x + SCROLLBAR_W, listY + listH, 0x60000000);
		
		int total = visible.size() * ROW_H;
		int thumbH = Math.max(16, listH * listH / total);
		int thumbY = listY + (int)((listH - thumbH) * (scroll / max));
		g.fill(x + 1, thumbY, x + SCROLLBAR_W - 1, thumbY + thumbH,
			draggingBar ? 0xFFFFFFFF : 0xFFAAAAAA);
	}
	
	private int popupW()
	{
		return PALETTE_COLS * (CELL + GAP) + GAP;
	}
	
	private int popupH()
	{
		return paletteH() + HEX_H + GAP;
	}
	
	private int paletteH()
	{
		int rows = (PALETTE.length + PALETTE_COLS - 1) / PALETTE_COLS;
		return rows * (CELL + GAP) + GAP;
	}
	
	private void renderPopup(GuiGraphics g, int mouseX, int mouseY,
		float partialTick)
	{
		int w = popupW();
		int h = popupH();
		g.fill(popupX - 1, popupY - 1, popupX + w + 1, popupY + h + 1,
			0xFFFFFFFF);
		g.fill(popupX, popupY, popupX + w, popupY + h, 0xFF101010);
		
		int current = colorOf(popup);
		int hovered = paletteIndexAt(mouseX, mouseY);
		
		for(int i = 0; i < PALETTE.length; i++)
		{
			int cx = popupX + GAP + (i % PALETTE_COLS) * (CELL + GAP);
			int cy = popupY + GAP + (i / PALETTE_COLS) * (CELL + GAP);
			
			if(i == hovered || PALETTE[i] == current)
				g.fill(cx - 1, cy - 1, cx + CELL + 1, cy + CELL + 1,
					i == hovered ? 0xFFFFFFFF : 0xFF888888);
			g.fill(cx, cy, cx + CELL, cy + CELL, 0xFF000000 | PALETTE[i]);
		}
		
		if(hexBox != null)
			hexBox.render(g, mouseX, mouseY, partialTick);
	}
	
	private void openPopup(Entry e, int rowY)
	{
		closePopup();
		
		popup = e;
		popupX = Math.max(2,
			Math.min(swatchX() + 16 - popupW(), width - popupW() - 2));
		popupY = rowY + ROW_H;
		if(popupY + popupH() > height - 2)
			popupY = rowY - popupH() - 2;
		
		hexBox = new EditBox(font, popupX + GAP, popupY + paletteH(),
			popupW() - 2 * GAP, HEX_H, Component.literal("#RRGGBB"));
		hexBox.setMaxLength(7);
		hexBox.setFilter(s -> s.matches("#?[0-9a-fA-F]*"));
		hexBox.setHint(Component.literal("#RRGGBB"));
		hexBox.setValue(String.format("#%06X", colorOf(e)));
		hexBox.setResponder(text -> {
			if(popup == null)
				return;
			
			Integer rgb = MoodialEpResolvedConfig.parseColor(text.trim());
			if(rgb != null)
			{
				setColor(popup, rgb);
				hexBox.setTextColor(0xFFE0E0E0);
			}else
				hexBox.setTextColor(0xFFFF5555);
		});
		addWidget(hexBox);
		setFocused(hexBox);
		hexBox.setFocused(true);
	}
	
	private void closePopup()
	{
		if(hexBox != null)
		{
			removeWidget(hexBox);
			hexBox = null;
		}
		popup = null;
	}
	
	private boolean inPopup(double mx, double my)
	{
		return mx >= popupX && mx < popupX + popupW() && my >= popupY
			&& my < popupY + popupH();
	}
	
	private int paletteIndexAt(double mx, double my)
	{
		if(popup == null)
			return -1;
		
		double rx = mx - popupX - GAP;
		double ry = my - popupY - GAP;
		if(rx < 0 || ry < 0)
			return -1;
		
		int col = (int)(rx / (CELL + GAP));
		int row = (int)(ry / (CELL + GAP));
		if(col >= PALETTE_COLS || rx % (CELL + GAP) >= CELL
			|| ry % (CELL + GAP) >= CELL)
			return -1;
		
		int index = row * PALETTE_COLS + col;
		return index >= 0 && index < PALETTE.length ? index : -1;
	}
	
	// --------------------------------------------------------------- input
	
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
	{
		double mx = event.x();
		double my = event.y();
		
		// while the color popup is open it takes every click
		if(popup != null)
		{
			if(inPopup(mx, my))
			{
				int index = paletteIndexAt(mx, my);
				if(index >= 0 && event.button() == 0)
				{
					setColor(popup, PALETTE[index]);
					closePopup();
					return true;
				}
				
				if(hexBox != null)
				{
					setFocused(hexBox);
					return hexBox.mouseClicked(event, doubleClick);
				}
				return true;
			}
			
			closePopup();
			return true;
		}
		
		boolean inList = mx >= listX && mx < listX + listW && my >= listY
			&& my < listY + listH;
		
		if(inList && event.button() == 0)
		{
			if(mx >= listX + listW - SCROLLBAR_W && maxScroll() > 0)
			{
				draggingBar = true;
				dragBarTo(my);
				return true;
			}
			
			int row = (int)((my - listY + scroll) / ROW_H);
			if(row >= 0 && row < visible.size())
			{
				Entry e = visible.get(row);
				int rowY = listY + row * ROW_H - (int)scroll;
				
				if(isOn(e) && mx >= swatchX() && mx < swatchX() + 16)
				{
					openPopup(e, rowY);
				}else
				{
					setOn(e, !isOn(e));
					if(onlySelected)
						refilter();
				}
				return true;
			}
		}
		
		return super.mouseClicked(event, doubleClick);
	}
	
	private void dragBarTo(double my)
	{
		double ratio = (my - listY) / listH;
		scroll = Math.max(0, Math.min(1, ratio)) * maxScroll();
	}
	
	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX,
		double dragY)
	{
		if(draggingBar)
		{
			dragBarTo(event.y());
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}
	
	@Override
	public boolean mouseReleased(MouseButtonEvent event)
	{
		draggingBar = false;
		return super.mouseReleased(event);
	}
	
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY,
		double scrollX, double scrollY)
	{
		if(popup != null)
			return true;
		
		scroll -= scrollY * ROW_H * 1.5;
		clampScroll();
		return true;
	}
	
	@Override
	public boolean keyPressed(KeyEvent event)
	{
		if(popup != null)
		{
			int key = event.key();
			// Escape, Enter, keypad Enter: close the color popup only
			if(key == 256 || key == 257 || key == 335)
			{
				closePopup();
				return true;
			}
		}
		
		return super.keyPressed(event);
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
}
