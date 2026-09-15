package com.moakiee.ae2lt.machine.atmosphericionizer;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import com.moakiee.ae2lt.blockentity.AtmosphericIonizerBlockEntity;

public final class AtmosphericIonizerLogic implements IGridTickable {
   private final AtmosphericIonizerBlockEntity host;

   public AtmosphericIonizerLogic(AtmosphericIonizerBlockEntity host) {
      this.host = host;
   }

   public TickingRequest getTickingRequest(IGridNode node) {
      return new TickingRequest(1, 20, !this.hasGridTickWork(), true);
   }

   public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
      if (this.host.m_58904_() == null || this.host.isClientSide() || this.host.m_58901_()) {
         return TickRateModulation.SLEEP;
      } else if (!this.host.isInstalledInWorld()) {
         this.host.cancelProcessingForRemoval();
         return TickRateModulation.SLEEP;
      } else {
         if (!this.host.hasLockedType()) {
            if (!this.host.hasLocalStartPrerequisites()) {
               this.host.setWorking(false);
               return TickRateModulation.SLEEP;
            }

            if (!this.host.canOperateInCurrentDimension() || this.host.isSelectedWeatherAlreadyActive()) {
               this.host.setWorking(false);
               return TickRateModulation.SLOWER;
            }

            if (!this.host.hasEnoughEnergyForSelectedStart()) {
               this.host.setWorking(false);
               return TickRateModulation.SLOWER;
            }

            if (!this.host.lockSelectedCondensate()) {
               this.host.setWorking(false);
               return TickRateModulation.SLOWER;
            }
         }

         if (!this.host.canOperateInCurrentDimension() || this.host.isLockedWeatherAlreadyActive()) {
            this.host.setWorking(false);
            return TickRateModulation.SLOWER;
         } else if (!this.host.hasLockedCondensateInput()) {
            this.host.setWorking(false);
            return TickRateModulation.SLOWER;
         } else {
            this.host.setWorking(true);
            if (!this.host.isReadyToCommit()) {
               long required = this.host.getRequiredEnergyForNextTick();
               if (required <= 0L) {
                  return TickRateModulation.SLOWER;
               } else if (!this.host.canExtractAEPower(required)) {
                  return TickRateModulation.SLOWER;
               } else if (!this.host.tryExtractAEPower(required)) {
                  return TickRateModulation.SLOWER;
               } else {
                  this.host.advanceProgress(required);
                  return this.host.isReadyToCommit() && this.host.commitLockedCondensate() ? TickRateModulation.URGENT : TickRateModulation.URGENT;
               }
            } else if (this.host.commitLockedCondensate()) {
               return TickRateModulation.URGENT;
            } else {
               this.host.setWorking(false);
               return TickRateModulation.SLOWER;
            }
         }
      }
   }

   public boolean hasGridTickWork() {
      return this.host.hasLockedType() || this.host.hasLocalStartPrerequisites();
   }

   public void onStateChanged() {
      this.host.getMainNode().ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
   }
}
