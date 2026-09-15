package com.moakiee.ae2lt.me;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import com.moakiee.ae2lt.api.lightning.ILightningEnergyHandler;
import com.moakiee.ae2lt.api.lightning.LightningTier;
import com.moakiee.ae2lt.me.key.LightningKey;
import org.jetbrains.annotations.Nullable;

public final class GridLightningEnergyHandler implements ILightningEnergyHandler {
   private final IActionHost host;

   public GridLightningEnergyHandler(IActionHost host) {
      this.host = host;
   }

   @Nullable
   private IGrid getGrid() {
      return GridNodeAccess.getActiveGrid(this.host.getActionableNode());
   }

   @Override
   public long getStored(LightningTier tier) {
      IGrid grid = this.getGrid();
      return grid == null ? 0L : grid.getStorageService().getCachedInventory().get(LightningKey.of(tier));
   }

   @Override
   public long getCapacity(LightningTier tier) {
      return Long.MAX_VALUE;
   }

   @Override
   public long insert(LightningTier tier, long amount, boolean simulate) {
      if (amount <= 0L) {
         return 0L;
      } else {
         IGrid grid = this.getGrid();
         return grid == null
            ? 0L
            : grid.getStorageService()
               .getInventory()
               .insert(LightningKey.of(tier), amount, simulate ? Actionable.SIMULATE : Actionable.MODULATE, IActionSource.ofMachine(this.host));
      }
   }

   @Override
   public long extract(LightningTier tier, long amount, boolean simulate) {
      if (amount <= 0L) {
         return 0L;
      } else {
         IGrid grid = this.getGrid();
         return grid == null
            ? 0L
            : grid.getStorageService()
               .getInventory()
               .extract(LightningKey.of(tier), amount, simulate ? Actionable.SIMULATE : Actionable.MODULATE, IActionSource.ofMachine(this.host));
      }
   }

   @Override
   public boolean canInsert(LightningTier tier) {
      return this.getGrid() != null;
   }

   @Override
   public boolean canExtract(LightningTier tier) {
      return this.getGrid() != null;
   }
}
