package com.moakiee.ae2lt.machine.teslacoil;

import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import com.moakiee.ae2lt.blockentity.TeslaCoilBlockEntity;
import com.moakiee.ae2lt.logic.AppFluxHelper;

public final class TeslaCoilLogic implements IGridTickable {
   private final TeslaCoilBlockEntity host;

   public TeslaCoilLogic(TeslaCoilBlockEntity host) {
      this.host = host;
   }

   public TickingRequest getTickingRequest(IGridNode node) {
      return new TickingRequest(1, 20, false, true);
   }

   public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
      if (!this.host.m_58901_() && this.host.m_58904_() != null && !this.host.isClientSide()) {
         this.rechargeFromAppliedFlux();
         if (!this.host.hasLockedMode()) {
            if (!this.host.hasLocalResourcesForMinimumOperation()) {
               this.host.setWorking(false);
               return TickRateModulation.SLEEP;
            }

            if (!this.host.canStartSelectedMode() || !this.host.hasEnoughEnergyForSelectedStart()) {
               this.host.setWorking(false);
               return TickRateModulation.SLOWER;
            }

            if (!this.host.lockSelectedMode()) {
               this.host.setWorking(false);
               return TickRateModulation.SLOWER;
            }
         }

         this.host.setWorking(true);
         if (this.host.isReadyToCommit()) {
            return this.host.commitLockedMode() ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
         } else if (!this.host.hasLockedModeLocalPrerequisites()) {
            return TickRateModulation.SLOWER;
         } else {
            long required = this.host.getRequiredEnergyForNextTick();
            if (required <= 0L) {
               return TickRateModulation.SLOWER;
            } else if (this.host.getEnergyStorage().getStoredEnergyLong() < required) {
               return TickRateModulation.SLOWER;
            } else {
               int extracted = this.host.getEnergyStorage().extractInternal(required, false);
               if ((long)extracted < required) {
                  return TickRateModulation.SLOWER;
               } else {
                  this.host.advanceProgress((long)extracted);
                  return this.host.isReadyToCommit() && this.host.commitLockedMode() ? TickRateModulation.URGENT : TickRateModulation.URGENT;
               }
            }
         }
      } else {
         return TickRateModulation.SLEEP;
      }
   }

   public boolean hasGridTickWork() {
      return this.host.hasLockedMode() || this.host.hasLocalResourcesForMinimumOperation();
   }

   public void onStateChanged() {
      this.host.getMainNode().ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
   }

   private void rechargeFromAppliedFlux() {
      if (AppFluxHelper.isAvailable()) {
         this.host
            .getMainNode()
            .ifPresent(
               (grid, node) -> AppFluxHelper.pullPowerFromNetwork(
                     grid.getStorageService().getInventory(), this.host.getEnergyStorage(), IActionSource.ofMachine(this.host)
                  )
            );
      }
   }
}
