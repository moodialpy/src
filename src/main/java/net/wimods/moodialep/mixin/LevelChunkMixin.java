/*
 * Adapted from the Meteor Client distribution
 * (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development. GPL-3.0-or-later.
 */
package net.wimods.moodialep.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.wimods.moodialep.MoodialEpMod;

/**
 * Tells the ESP when a block in the (client) world changes. This is what
 * Meteor's LevelChunkMixin does with its BlockUpdateEvent.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin
{
	@Shadow
	@Final
	private Level level;
	
	@Inject(method = "setBlockState", at = @At("TAIL"))
	private void onSetBlockState(BlockPos pos, BlockState state, int flags,
		CallbackInfoReturnable<BlockState> cir)
	{
		if(!level.isClientSide())
			return;
		
		MoodialEpMod mod = MoodialEpMod.getInstance();
		if(mod != null)
			mod.onBlockUpdate(pos, cir.getReturnValue(), state);
	}
}
