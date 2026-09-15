package com.moakiee.ae2lt.machine.crystalcatalyzer.recipe;

import com.moakiee.ae2lt.machine.crystalcatalyzer.CrystalCatalyzerInventory;
import com.moakiee.ae2lt.recipe.RecipeContainerInput;
import net.minecraft.world.item.ItemStack;

public final class CrystalCatalyzerRecipeInput extends RecipeContainerInput {
   private final ItemStack catalyst;

   public CrystalCatalyzerRecipeInput(ItemStack catalyst) {
      this.catalyst = catalyst == null ? ItemStack.f_41583_ : catalyst.m_41777_();
   }

   public static CrystalCatalyzerRecipeInput fromMachine(CrystalCatalyzerInventory inventory) {
      return new CrystalCatalyzerRecipeInput(inventory.getStackInSlot(0));
   }

   public ItemStack catalyst() {
      return this.catalyst;
   }

   @Override
   public boolean m_7983_() {
      return this.catalyst.m_41619_();
   }

   @Override
   public ItemStack m_8020_(int slotIndex) {
      return slotIndex == 0 ? this.catalyst : ItemStack.f_41583_;
   }

   @Override
   public int size() {
      return 1;
   }
}
