package com.moakiee.ae2lt.machine.crystalcatalyzer;

import com.moakiee.ae2lt.blockentity.CrystalCatalyzerBlockEntity;
import com.moakiee.ae2lt.machine.common.AbstractGridRecipeMachineLogic;
import com.moakiee.ae2lt.machine.crystalcatalyzer.recipe.CrystalCatalyzerLockedRecipe;
import com.moakiee.ae2lt.machine.crystalcatalyzer.recipe.CrystalCatalyzerRecipeCandidate;
import com.moakiee.ae2lt.machine.crystalcatalyzer.recipe.CrystalCatalyzerRecipeInput;
import com.moakiee.ae2lt.machine.crystalcatalyzer.recipe.CrystalCatalyzerRecipeService;
import java.util.Optional;

public final class CrystalCatalyzerLogic
   extends AbstractGridRecipeMachineLogic<CrystalCatalyzerBlockEntity, CrystalCatalyzerLockedRecipe, CrystalCatalyzerRecipeCandidate> {
   private static final long MAX_ENERGY_PER_TICK = 200000L;

   public CrystalCatalyzerLogic(CrystalCatalyzerBlockEntity host) {
      super(host);
   }

   @Override
   protected int getMinProcessTicks() {
      return this.host.getMode().getMinProcessTicks();
   }

   @Override
   protected long getMaxEnergyPerTickForSpeedCards(int speedCards) {
      return 200000L;
   }

   protected long getTotalEnergy(CrystalCatalyzerLockedRecipe lockedRecipe) {
      return lockedRecipe.totalEnergy();
   }

   protected Optional<CrystalCatalyzerRecipeCandidate> validateLockedRecipe(CrystalCatalyzerLockedRecipe lockedRecipe) {
      return CrystalCatalyzerRecipeService.findRecipeById(this.host.m_58904_(), lockedRecipe.recipeId())
         .filter(candidate -> candidate.recipe().mode() == this.host.getMode())
         .filter(candidate -> candidate.recipe().matches(CrystalCatalyzerRecipeInput.fromMachine(this.host.getInventory()), this.host.m_58904_()));
   }

   protected boolean canAcceptOutputThisTick(CrystalCatalyzerLockedRecipe lockedRecipe) {
      return this.host.canAcceptLockedRecipeOutput(lockedRecipe) && this.host.canAdvanceLockedRecipe(lockedRecipe);
   }
}
