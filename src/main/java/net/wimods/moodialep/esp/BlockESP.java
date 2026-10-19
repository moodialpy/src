/*
 * Adapted from the Meteor Client distribution
 * (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development. GPL-3.0-or-later.
 */

package net.wimods.moodialep.esp;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.dimension.DimensionType;
import net.wimods.moodialep.MoodialEpShapeMode;
import net.wimods.moodialep.render.Color;
import net.wimods.moodialep.render.Renderer3D;
import net.wimods.moodialep.util.ChunkUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The BlockESP module of Meteor Client without the module system around it.
 * The search, the chunk/group bookkeeping and the rendering are the same as
 * in Meteor. What was a module setting there (the block list, the per-block
 * look, tracers) is now set from the config window through
 * {@link #setConfig(Set, Map, boolean)}, and what Meteor got from its event
 * bus (chunk loaded, block changed, tick, render) is called from MoodialEpMod
 * and the mixins.
 */
public class BlockESP {
    private static final BlockESP INSTANCE = new BlockESP();

    private static final Minecraft mc = Minecraft.getInstance();

    // General (the settings of the Meteor module)

    /** Blocks to search for. */
    private volatile Set<Block> blocks = Set.of();

    /** Used for blocks that have no config of their own. Same default as Meteor. */
    private final ESPBlockData defaultBlockConfig = new ESPBlockData(
        MoodialEpShapeMode.LINES,
        new Color(0, 255, 200),
        new Color(0, 255, 200, 25),
        true,
        new Color(0, 255, 200, 125)
    );

    /** Config for each block. */
    private volatile Map<Block, ESPBlockData> blockConfigs = Map.of();

    private volatile boolean tracers = false;

    private final BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

    private final Long2ObjectMap<ESPChunk> chunks = new Long2ObjectOpenHashMap<>();
    private final Set<ESPGroup> groups = new ReferenceOpenHashSet<>();
    private final ExecutorService workerThread = Executors.newSingleThreadExecutor(task -> {
        Thread t = new Thread(task, "MoodialEP-BlockESP");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean active;
    private DimensionType lastDimension;

    // Only used for the [perf] line in the log
    private final AtomicLong scanCount = new AtomicLong();
    private final AtomicLong scanNanos = new AtomicLong();

    private BlockESP() {
    }

    public static BlockESP get() {
        return INSTANCE;
    }

    /** Same as the render distance check in Meteor's Utils.getRenderDistance(). */
    public static int getRenderDistance() {
        return Math.max(2, mc.options.getEffectiveRenderDistance());
    }

    /** Chunks are stored by position, like ChunkPos.pack() in newer versions. */
    public static long chunkKey(int x, int z) {
        return (long) x & 0xFFFFFFFFL | ((long) z & 0xFFFFFFFFL) << 32;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Applies the config. The look of the blocks (colors, style) is read again
     * every frame, so that part takes effect right away. A different block
     * list needs a new search, the same as in Meteor when the "blocks" setting
     * changes.
     */
    public void setConfig(Set<Block> newBlocks, Map<Block, ESPBlockData> newConfigs, boolean newTracers) {
        boolean blocksChanged = !newBlocks.equals(blocks);

        blocks = newBlocks;
        blockConfigs = newConfigs;
        tracers = newTracers;

        if (blocksChanged && active && mc.level != null && mc.player != null) onActivate();
    }

    /** Switches the ESP on (and searches all loaded chunks). */
    public void activate() {
        active = true;
        onActivate();
    }

    /** Switches the ESP off. */
    public void deactivate() {
        active = false;
        onDeactivate();
    }

    public void onActivate() {
        synchronized (chunks) {
            chunks.clear();
            groups.clear();
        }

        if (mc.level == null || mc.player == null) return;

        ChunkUtils.getLoadedChunks().forEach(this::searchChunk);

        lastDimension = mc.level.dimensionType();
    }

    public void onDeactivate() {
        synchronized (chunks) {
            chunks.clear();
            groups.clear();
        }
    }

    ESPBlockData getBlockData(Block block) {
        ESPBlockData blockData = blockConfigs.get(block);
        return blockData == null ? defaultBlockConfig : blockData;
    }

    private void updateChunk(int x, int z) {
        ESPChunk chunk = chunks.get(chunkKey(x, z));
        if (chunk != null) chunk.update();
    }

    private void updateBlock(int x, int y, int z) {
        ESPChunk chunk = chunks.get(chunkKey(x >> 4, z >> 4));
        if (chunk != null) chunk.update(x, y, z);
    }

    public ESPBlock getBlock(int x, int y, int z) {
        ESPChunk chunk = chunks.get(chunkKey(x >> 4, z >> 4));
        return chunk == null ? null : chunk.get(x, y, z);
    }

    public ESPGroup newGroup(Block block) {
        synchronized (chunks) {
            ESPGroup group = new ESPGroup(block);
            groups.add(group);
            return group;
        }
    }

    public void removeGroup(ESPGroup group) {
        synchronized (chunks) {
            groups.remove(group);
        }
    }

    /** A chunk was loaded or sent again (ChunkDataEvent in Meteor). */
    public void onChunkData(ChunkAccess chunk) {
        if (!active) return;
        searchChunk(chunk);
    }

    private void searchChunk(ChunkAccess chunk) {
        Set<Block> searchBlocks = blocks;
        if (searchBlocks.isEmpty()) return;

        workerThread.submit(() -> {
            if (!isActive()) return;

            long start = System.nanoTime();
            ESPChunk schunk = ESPChunk.searchChunk(chunk, searchBlocks);
            scanNanos.addAndGet(System.nanoTime() - start);
            scanCount.incrementAndGet();

            if (schunk.size() > 0) {
                synchronized (chunks) {
                    chunks.put(chunkKey(chunk.getPos().x, chunk.getPos().z), schunk);
                    schunk.update();

                    // Update neighbour chunks
                    updateChunk(chunk.getPos().x - 1, chunk.getPos().z);
                    updateChunk(chunk.getPos().x + 1, chunk.getPos().z);
                    updateChunk(chunk.getPos().x, chunk.getPos().z - 1);
                    updateChunk(chunk.getPos().x, chunk.getPos().z + 1);
                }
            }
        });
    }

    /** A block in the world changed (BlockUpdateEvent in Meteor). */
    public void onBlockUpdate(BlockPos pos, BlockState oldState, BlockState newState) {
        if (!active || oldState == null || newState == null) return;

        // Minecraft probably reuses the pos BlockPos instance because it causes problems when trying to use it inside another thread
        int bx = pos.getX();
        int by = pos.getY();
        int bz = pos.getZ();

        int chunkX = bx >> 4;
        int chunkZ = bz >> 4;
        long key = chunkKey(chunkX, chunkZ);

        Set<Block> searchBlocks = blocks;
        boolean added = searchBlocks.contains(newState.getBlock()) && !searchBlocks.contains(oldState.getBlock());
        boolean removed = !added && !searchBlocks.contains(newState.getBlock()) && searchBlocks.contains(oldState.getBlock());

        if (added || removed) {
            workerThread.submit(() -> {
                synchronized (chunks) {
                    ESPChunk chunk = chunks.get(key);

                    if (chunk == null) {
                        chunk = new ESPChunk(chunkX, chunkZ);
                        if (chunk.shouldBeDeleted()) return;

                        chunks.put(key, chunk);
                    }

                    blockPos.set(bx, by, bz);

                    if (added) chunk.add(blockPos);
                    else chunk.remove(blockPos);

                    // Update neighbour blocks
                    for (int x = -1; x < 2; x++) {
                        for (int z = -1; z < 2; z++) {
                            for (int y = -1; y < 2; y++) {
                                if (x == 0 && y == 0 && z == 0) continue;

                                updateBlock(bx + x, by + y, bz + z);
                            }
                        }
                    }
                }
            });
        }
    }

    /** Runs every tick (TickEvent.Post in Meteor). */
    public void onPostTick() {
        if (!active || mc.level == null) return;

        DimensionType dimension = mc.level.dimensionType();

        if (lastDimension != dimension) onActivate();
        lastDimension = dimension;
    }

    /** Runs every frame, after the world is drawn (Render3DEvent in Meteor). */
    public void onRender(Renderer3D renderer) {
        if (!active) return;

        synchronized (chunks) {
            for (Iterator<ESPChunk> it = chunks.values().iterator(); it.hasNext(); ) {
                ESPChunk chunk = it.next();

                if (chunk.shouldBeDeleted()) {
                    workerThread.submit(() -> {
                        for (ESPBlock block : chunk.blocks.values()) {
                            block.group.remove(block, false);
                            block.loaded = false;
                        }
                    });

                    it.remove();
                } else chunk.render(renderer);
            }

            if (tracers) {
                for (ESPGroup group : groups) {
                    group.render(renderer);
                }
            }
        }
    }

    /** Number of highlighted blocks (for the badge next to the hotbar). */
    public int getBlockCount() {
        int count = 0;

        synchronized (chunks) {
            for (ESPChunk chunk : chunks.values()) count += chunk.size();
        }

        return count;
    }

    /** Number of kinds of blocks that are searched for. */
    public int getSearchedBlockTypes() {
        return blocks.size();
    }

    /** {chunks searched, nanoseconds spent searching} since the last call. */
    public long[] takeScanStats() {
        return new long[] {scanCount.getAndSet(0), scanNanos.getAndSet(0)};
    }

    public String getInfoString() {
        return "%s groups".formatted(groups.size());
    }
}
