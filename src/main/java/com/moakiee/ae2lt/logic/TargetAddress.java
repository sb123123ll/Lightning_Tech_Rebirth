package com.moakiee.ae2lt.logic;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public abstract class TargetAddress {
   private final ResourceKey<Level> dimension;
   private final BlockPos pos;
   private final Direction boundFace;
   private final int hashCode;

   protected TargetAddress(ResourceKey<Level> dimension, BlockPos pos, Direction boundFace) {
      this.dimension = Objects.requireNonNull(dimension, "dimension");
      this.pos = Objects.requireNonNull(pos, "pos").m_7949_();
      this.boundFace = Objects.requireNonNull(boundFace, "boundFace");
      int hash = this.dimension.hashCode();
      hash = 31 * hash + this.pos.hashCode();
      this.hashCode = 31 * hash + this.boundFace.hashCode();
   }

   public final ResourceKey<Level> dimension() {
      return this.dimension;
   }

   public final BlockPos pos() {
      return this.pos;
   }

   public final Direction boundFace() {
      return this.boundFace;
   }

   public final boolean sameTarget(ResourceKey<Level> otherDimension, BlockPos otherPos) {
      return this.dimension.equals(otherDimension) && this.pos.equals(otherPos);
   }

   @Override
   public final boolean equals(Object other) {
      if (this == other) {
         return true;
      } else {
         return !(other instanceof TargetAddress address)
            ? false
            : this.dimension.equals(address.dimension) && this.pos.equals(address.pos) && this.boundFace == address.boundFace;
      }
   }

   @Override
   public final int hashCode() {
      return this.hashCode;
   }

   @Override
   public String toString() {
      return this.getClass().getSimpleName() + "[dimension=" + this.dimension.m_135782_() + ", pos=" + this.pos + ", boundFace=" + this.boundFace + "]";
   }
}
