/*
 * Adapted from the Meteor Client distribution
 * (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development. GPL-3.0-or-later.
 */

package net.wimods.moodialep.esp;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.wimods.moodialep.render.Renderer3D;

import java.util.Set;

public class ESPChunk {
    private static final Minecraft mc = Minecraft.getInstance();

    private final int x, z;
    public Long2ObjectMap<ESPBlock> blocks;

    public ESPChunk(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public ESPBlock get(int x, int y, int z) {
        return blocks == null ? null : blocks.get(ESPBlock.getKey(x, y, z));
    }

    public void add(BlockPos blockPos, boolean update) {
        ESPBlock block = new ESPBlock(blockPos.getX(), blockPos.getY(), blockPos.getZ());

        if (blocks == null) blocks = new Long2ObjectOpenHashMap<>(64);
        blocks.put(ESPBlock.getKey(blockPos), block);

        if (update) block.update();
    }

    public void add(BlockPos blockPos) {
        add(blockPos, true);
    }

    public void remove(BlockPos blockPos) {
        if (blocks != null) {
            ESPBlock block = blocks.remove(ESPBlock.getKey(blockPos));
            if (block != null) block.group.remove(block);
        }
    }

    public void update() {
        if (blocks != null) {
            for (ESPBlock block : blocks.values()) block.update();
        }
    }

    public void update(int x, int y, int z) {
        if (blocks != null) {
            ESPBlock block = blocks.get(ESPBlock.getKey(x, y, z));
            if (block != null) block.update();
        }
    }

    public int size() {
        return blocks == null ? 0 : blocks.size();
    }

    public boolean shouldBeDeleted() {
        if (mc.player == null || mc.level == null) return true;

        int viewDist = BlockESP.getRenderDistance() + 1;
        int chunkX = mc.player.blockPosition().getX() >> 4;
        int chunkZ = mc.player.blockPosition().getZ() >> 4;

        return x > chunkX + viewDist || x < chunkX - viewDist || z > chunkZ + viewDist || z < chunkZ - viewDist;
    }

    public void render(Renderer3D renderer) {
        if (blocks != null) {
            for (ESPBlock block : blocks.values()) block.render(renderer);
        }
    }


    public static ESPChunk searchChunk(ChunkAccess chunk, Set<Block> blocks) {
        ESPChunk schunk = new ESPChunk(chunk.getPos().x, chunk.getPos().z);
        if (schunk.shouldBeDeleted()) return schunk;

        // Not in Meteor: look at the palette of every 16x16x16 section first and
        // skip the sections that cannot contain any of the wanted blocks. The
        // result is the same as checking every block, but most chunks don't
        // need the full search at all.
        int minY = chunk.getMinY();
        LevelChunkSection[] sections = chunk.getSections();
        boolean[] maybe = new boolean[sections.length];
        boolean any = false;

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir()) continue;

            if (section.maybeHas(state -> blocks.contains(state.getBlock()))) {
                maybe[i] = true;
                any = true;
            }
        }

        if (!any) return schunk;

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        Heightmap heightmap = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE);

        for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
            for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                int height = heightmap.getFirstAvailable(x - chunk.getPos().getMinBlockX(), z - chunk.getPos().getMinBlockZ());

                for (int y = minY; y < height; y++) {
                    int sectionIndex = (y - minY) >> 4;

                    // jump to the end of a section that has none of the blocks
                    if (sectionIndex >= maybe.length || !maybe[sectionIndex]) {
                        y = minY + ((sectionIndex + 1) << 4) - 1;
                        continue;
                    }

                    blockPos.set(x, y, z);
                    BlockState bs = chunk.getBlockState(blockPos);

                    if (blocks.contains(bs.getBlock())) schunk.add(blockPos, false);
                }
            }
        }

        return schunk;
    }
}
