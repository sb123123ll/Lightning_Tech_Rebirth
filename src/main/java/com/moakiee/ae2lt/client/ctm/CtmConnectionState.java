package com.moakiee.ae2lt.client.ctm;

import net.minecraft.core.Direction;

public record CtmConnectionState(boolean[] faceCulled, int[] edgeConnect, int[] cornerConnect) {
   public boolean culled(Direction face) {
      return this.faceCulled[face.m_122411_()];
   }

   public int edges(Direction face) {
      return this.edgeConnect[face.m_122411_()];
   }

   public int corners(Direction face) {
      return this.cornerConnect[face.m_122411_()];
   }
}
