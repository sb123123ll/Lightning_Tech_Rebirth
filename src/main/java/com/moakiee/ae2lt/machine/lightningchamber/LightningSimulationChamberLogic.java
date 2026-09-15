package com.moakiee.ae2lt.machine.lightningchamber;

import com.moakiee.ae2lt.blockentity.LightningSimulationChamberBlockEntity;
import com.moakiee.ae2lt.machine.common.AbstractGridRecipeMachineLogic;
import com.moakiee.ae2lt.machine.lightningchamber.recipe.LightningSimulationLockedRecipe;
import com.moakiee.ae2lt.machine.lightningchamber.recipe.LightningSimulationRecipeCandidate;
import com.moakiee.ae2lt.machine.lightningchamber.recipe.LightningSimulationRecipeService;
import java.util.Optional;

public final class LightningSimulationChamberLogic
   extends AbstractGridRecipeMachineLogic<LightningSimulationChamberBlockEntity, LightningSimulationLockedRecipe, LightningSimulationRecipeCandidate> {
   public static final int MIN_PROCESS_TICKS = 4;

   public LightningSimulationChamberLogic(LightningSimulationChamberBlockEntity host) {
      super(host);
   }

   @Override
   protected int getMinProcessTicks() {
      return 4;
   }

   @Override
   protected long getMaxEnergyPerTickForSpeedCards(int speedCards) {
      return switch (speedCards) {
         case 0 -> 200L;
         case 1 -> 2000L;
         case 2 -> 10000L;
         case 3 -> 50000L;
         default -> 200000L;
      };
   }

   protected long getTotalEnergy(LightningSimulationLockedRecipe lockedRecipe) {
      return lockedRecipe.totalEnergy();
   }

   protected Optional<LightningSimulationRecipeCandidate> validateLockedRecipe(LightningSimulationLockedRecipe lockedRecipe) {
      return LightningSimulationRecipeService.findLockedRecipeMatch(
         this.host.m_58904_(), this.host.getInventory(), lockedRecipe, this.host.getAvailableHighVoltage(), this.host.getAvailableExtremeHighVoltage()
      );
   }
}
