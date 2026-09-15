package com.moakiee.ae2lt.machine.overloadfactory;

import com.moakiee.ae2lt.blockentity.OverloadProcessingFactoryBlockEntity;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.machine.common.AbstractGridRecipeMachineLogic;
import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingLockedRecipe;
import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipeCandidate;
import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipeService;
import java.util.Optional;

public final class OverloadProcessingFactoryLogic
   extends AbstractGridRecipeMachineLogic<OverloadProcessingFactoryBlockEntity, OverloadProcessingLockedRecipe, OverloadProcessingRecipeCandidate> {
   public static final int MIN_PROCESS_TICKS = 4;

   public OverloadProcessingFactoryLogic(OverloadProcessingFactoryBlockEntity host) {
      super(host);
   }

   @Override
   protected int getMinProcessTicks() {
      return 4;
   }

   @Override
   protected long getMaxEnergyPerTickForSpeedCards(int speedCards) {
      return switch (speedCards) {
         case 0 -> AE2LTCommonConfig.overloadFactoryFePerTickNoSpeedCard();
         case 1 -> AE2LTCommonConfig.overloadFactoryFePerTickOneSpeedCard();
         case 2 -> AE2LTCommonConfig.overloadFactoryFePerTickTwoSpeedCards();
         case 3 -> AE2LTCommonConfig.overloadFactoryFePerTickThreeSpeedCards();
         default -> AE2LTCommonConfig.overloadFactoryFePerTickFourSpeedCards();
      };
   }

   protected long getTotalEnergy(OverloadProcessingLockedRecipe lockedRecipe) {
      return lockedRecipe.totalEnergy();
   }

   protected Optional<OverloadProcessingRecipeCandidate> validateLockedRecipe(OverloadProcessingLockedRecipe lockedRecipe) {
      return OverloadProcessingRecipeService.findLockedRecipeMatch(
         this.host.m_58904_(),
         this.host.getInventory(),
         this.host.getInputFluid(),
         this.host.getOutputFluid(),
         lockedRecipe,
         this.host.getAvailableHighVoltage(),
         this.host.getAvailableExtremeHighVoltage()
      );
   }
}
