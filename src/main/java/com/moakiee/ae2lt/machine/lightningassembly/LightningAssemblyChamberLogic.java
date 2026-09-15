package com.moakiee.ae2lt.machine.lightningassembly;

import com.moakiee.ae2lt.blockentity.LightningAssemblyChamberBlockEntity;
import com.moakiee.ae2lt.machine.common.AbstractGridRecipeMachineLogic;
import com.moakiee.ae2lt.machine.lightningassembly.recipe.LightningAssemblyLockedRecipe;
import com.moakiee.ae2lt.machine.lightningassembly.recipe.LightningAssemblyRecipeCandidate;
import com.moakiee.ae2lt.machine.lightningassembly.recipe.LightningAssemblyRecipeService;
import java.util.Optional;

public final class LightningAssemblyChamberLogic
   extends AbstractGridRecipeMachineLogic<LightningAssemblyChamberBlockEntity, LightningAssemblyLockedRecipe, LightningAssemblyRecipeCandidate> {
   public static final int MIN_PROCESS_TICKS = 4;

   public LightningAssemblyChamberLogic(LightningAssemblyChamberBlockEntity host) {
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
         default -> 500000L;
      };
   }

   protected long getTotalEnergy(LightningAssemblyLockedRecipe lockedRecipe) {
      return lockedRecipe.totalEnergy();
   }

   protected Optional<LightningAssemblyRecipeCandidate> validateLockedRecipe(LightningAssemblyLockedRecipe lockedRecipe) {
      return LightningAssemblyRecipeService.findLockedRecipeMatch(
         this.host.m_58904_(), this.host.getInventory(), lockedRecipe, this.host.getAvailableHighVoltage(), this.host.getAvailableExtremeHighVoltage()
      );
   }
}
