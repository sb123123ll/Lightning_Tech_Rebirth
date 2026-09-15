package com.moakiee.ae2lt.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

@Deprecated(
   forRemoval = false
)
public interface WirelessConnectionRef extends com.moakiee.ae2lt.logic.wireless.support.WirelessConnectionRef {
   @Override
   default boolean sameTarget(ResourceKey<Level> otherDimension, BlockPos otherPos) {
      return this.dimension().equals(otherDimension) && this.pos().equals(otherPos);
   }
}
