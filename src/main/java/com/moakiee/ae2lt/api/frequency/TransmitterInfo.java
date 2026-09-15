package com.moakiee.ae2lt.api.frequency;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record TransmitterInfo(ResourceKey<Level> dimension, BlockPos pos, boolean advanced) {
}
