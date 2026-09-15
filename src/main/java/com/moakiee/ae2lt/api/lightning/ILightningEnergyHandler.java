package com.moakiee.ae2lt.api.lightning;

public interface ILightningEnergyHandler {
   long getStored(LightningTier var1);

   default long getCapacity(LightningTier tier) {
      return Long.MAX_VALUE;
   }

   long insert(LightningTier var1, long var2, boolean var4);

   long extract(LightningTier var1, long var2, boolean var4);

   default boolean canInsert(LightningTier tier) {
      return true;
   }

   default boolean canExtract(LightningTier tier) {
      return true;
   }

   default boolean isEmpty(LightningTier tier) {
      return this.getStored(tier) == 0L;
   }

   default boolean isFull(LightningTier tier) {
      long cap = this.getCapacity(tier);
      return cap > 0L && cap != Long.MAX_VALUE ? this.getStored(tier) >= cap : false;
   }
}
