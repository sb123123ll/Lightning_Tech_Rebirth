package com.moakiee.ae2lt.machine.common;

import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.core.definitions.AEItems;
import com.moakiee.ae2lt.logic.AppFluxHelper;
import java.util.Optional;

public abstract class AbstractGridRecipeMachineLogic<H extends AENetworkBlockEntity & GridRecipeMachineHost<L, C> & IUpgradeableObject, L, C>
   implements IGridTickable {
   protected final H host;

   protected AbstractGridRecipeMachineLogic(H host) {
      this.host = host;
   }

   public TickingRequest getTickingRequest(IGridNode node) {
      return new TickingRequest(1, 20, false, true);
   }

   public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
      if (!this.host.m_58901_() && this.host.m_58904_() != null && !this.host.isClientSide()) {
         this.rechargeFromAppliedFlux();
         if (!this.host.hasLockedRecipe()) {
            this.tryStartProcessing();
         }

         Optional<L> lockedRecipe = this.host.getLockedRecipe();
         if (lockedRecipe.isEmpty()) {
            this.host.setWorking(false);
            return this.host.pushOutResult() ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
         } else {
            this.host.setWorking(true);
            if (this.canAcceptOutputThisTick(lockedRecipe.get())) {
               Optional<C> lockedCandidate = this.validateLockedRecipe(lockedRecipe.get());
               if (lockedCandidate.isEmpty()) {
                  this.host.abortProcessing();
                  return this.host.pushOutResult() ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
               } else {
                  return this.tickActiveRecipe(lockedRecipe.get(), lockedCandidate.get());
               }
            } else {
               return this.host.pushOutResult() ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
            }
         }
      } else {
         return TickRateModulation.SLEEP;
      }
   }

   public void onStateChanged() {
      this.host.getMainNode().ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
   }

   public long getCurrentMaxEnergyPerTick() {
      return this.getMaxEnergyPerTickForSpeedCards(this.host.getInstalledUpgrades(AEItems.SPEED_CARD));
   }

   public long getMinDurationLimitedMaxEnergyPerTick(long remainingEnergy, int processingTicksSpent) {
      int remainingRequiredTicks = Math.max(1, this.getMinProcessTicks() - processingTicksSpent);
      return divideCeil(remainingEnergy, (long)remainingRequiredTicks);
   }

   public long computeEnergyToConsumeThisTick(L lockedRecipe) {
      long remainingEnergy = this.getTotalEnergy(lockedRecipe) - this.host.getConsumedEnergy();
      if (remainingEnergy <= 0L) {
         return 0L;
      } else {
         long upgradedMachineCap = this.getCurrentMaxEnergyPerTick();
         long minDurationLimitedCap = this.getMinDurationLimitedMaxEnergyPerTick(remainingEnergy, this.host.getProcessingTicksSpent());
         long availableFe = this.host.getMachineStoredEnergy();
         return Math.min(Math.min(upgradedMachineCap, minDurationLimitedCap), Math.min(availableFe, remainingEnergy));
      }
   }

   protected abstract int getMinProcessTicks();

   protected abstract long getMaxEnergyPerTickForSpeedCards(int var1);

   protected abstract long getTotalEnergy(L var1);

   protected abstract Optional<C> validateLockedRecipe(L var1);

   protected boolean canAcceptOutputThisTick(L lockedRecipe) {
      return true;
   }

   private TickRateModulation tickActiveRecipe(L lockedRecipe, C lockedCandidate) {
      if (this.host.getConsumedEnergy() >= this.getTotalEnergy(lockedRecipe)) {
         this.completeRecipe(lockedRecipe, lockedCandidate);
         return this.host.hasLockedRecipe() ? TickRateModulation.SLOWER : TickRateModulation.URGENT;
      } else if (!this.canAcceptOutputThisTick(lockedRecipe)) {
         return this.host.pushOutResult() ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
      } else {
         long toConsume = this.computeEnergyToConsumeThisTick(lockedRecipe);
         if (toConsume <= 0L) {
            return this.host.pushOutResult() ? TickRateModulation.URGENT : this.energyWaitModulation();
         } else {
            int consumed = this.host.extractMachineEnergy(toConsume);
            if (consumed <= 0) {
               return this.host.pushOutResult() ? TickRateModulation.URGENT : this.energyWaitModulation();
            } else {
               this.host.onEnergyConsumed(consumed);
               if (this.host.getConsumedEnergy() >= this.getTotalEnergy(lockedRecipe)) {
                  this.completeRecipe(lockedRecipe, lockedCandidate);
                  return this.host.hasLockedRecipe() ? TickRateModulation.SLOWER : TickRateModulation.URGENT;
               } else {
                  this.host.pushOutResult();
                  return TickRateModulation.URGENT;
               }
            }
         }
      }
   }

   private TickRateModulation energyWaitModulation() {
      return AppFluxHelper.isAvailable() ? TickRateModulation.SLOWER : TickRateModulation.SLEEP;
   }

   private void tryStartProcessing() {
      Optional<L> lockedRecipe = this.host.lockCurrentRecipe();
      if (lockedRecipe.isEmpty()) {
         this.host.resetProgressState();
         this.host.setWorking(false);
      } else {
         this.host.resetProgressState();
         this.host.setWorking(true);
      }
   }

   private void completeRecipe(L lockedRecipe, C lockedCandidate) {
      if (!this.host.completeLockedRecipe(lockedRecipe, lockedCandidate)) {
         this.host.abortProcessing();
      }
   }

   private void rechargeFromAppliedFlux() {
      if (AppFluxHelper.isAvailable()) {
         this.host
            .getMainNode()
            .ifPresent(
               (grid, node) -> AppFluxHelper.pullPowerFromNetwork(
                     grid.getStorageService().getInventory(), this.host.getMachineEnergyStorage(), IActionSource.ofMachine(this.host)
                  )
            );
      }
   }

   private static long divideCeil(long dividend, long divisor) {
      if (divisor <= 0L) {
         throw new IllegalArgumentException("divisor must be positive");
      } else {
         return dividend <= 0L ? 0L : (dividend + divisor - 1L) / divisor;
      }
   }
}
