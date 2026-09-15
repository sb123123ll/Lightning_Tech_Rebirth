package com.moakiee.ae2lt.client;

import net.minecraft.client.resources.model.BakedModel;

final class HyperdimensionalPigmeeBakedModel extends SpinningFumoBakedModel {
   HyperdimensionalPigmeeBakedModel(BakedModel originalModel) {
      super(originalModel);
   }

   public boolean m_7521_() {
      return true;
   }

   BakedModel baseModel() {
      return this.originalModel;
   }
}
