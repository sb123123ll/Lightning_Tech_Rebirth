package com.moakiee.ae2lt.crafting.matrix.core;

import appeng.api.stacks.AEKey;

public interface CraftingCoreHost {
   long getGameTime();

   boolean m_58901_();

   boolean isConnected();

   long insertToNetwork(AEKey var1, long var2);

   void spawnToWorld(AEKey var1, long var2);
}
