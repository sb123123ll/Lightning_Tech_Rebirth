package com.moakiee.ae2lt.machine.crystalcatalyzer;

import com.moakiee.ae2lt.machine.crystalcatalyzer.recipe.CrystalCatalyzerRecipeService;
import com.moakiee.ae2lt.machine.crystalcatalyzer.recipe.Mode;
import com.moakiee.ae2lt.machine.lightningchamber.LargeStackItemHandler;
import com.moakiee.ae2lt.registry.ModItems;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class CrystalCatalyzerInventory extends LargeStackItemHandler {
   public static final int SLOT_CATALYST = 0;
   public static final int SLOT_MATRIX = 1;
   public static final int SLOT_OUTPUT = 2;
   public static final int SLOT_COUNT = 3;
   public static final int CATALYST_SLOT_LIMIT = 256;
   public static final int OUTPUT_SLOT_LIMIT = 1024;
   public static final int MATRIX_SLOT_LIMIT = 1;
   @Nullable
   private Level level;
   private final Supplier<Mode> modeSupplier;

   public CrystalCatalyzerInventory(@Nullable Runnable changeListener) {
      this(changeListener, () -> Mode.CRYSTAL);
   }

   public CrystalCatalyzerInventory(@Nullable Runnable changeListener, Supplier<Mode> modeSupplier) {
      super(3, changeListener);
      this.modeSupplier = modeSupplier != null ? modeSupplier : () -> Mode.CRYSTAL;
   }

   public void setLevel(@Nullable Level level) {
      this.level = level;
   }

   @Override
   public int getSlotLimit(int slot) {
      this.validateSlotIndex(slot);

      return switch (slot) {
         case 0 -> 256;
         case 1 -> 1;
         case 2 -> 1024;
         default -> 1024;
      };
   }

   @Override
   public boolean isItemValid(int slot, ItemStack stack) {
      this.validateSlotIndex(slot);
      if (stack.m_41619_()) {
         return false;
      } else {
         return switch (slot) {
            case 0 -> CrystalCatalyzerRecipeService.isKnownCatalyst(this.level, stack, this.modeSupplier.get());
            case 1 -> this.isLightningCollapseMatrix(stack);
            case 2 -> false;
            default -> false;
         };
      }
   }

   public boolean isLightningCollapseMatrix(ItemStack stack) {
      return stack.m_150930_((Item)ModItems.LIGHTNING_COLLAPSE_MATRIX.get());
   }

   public boolean hasLightningCollapseMatrix() {
      return this.isLightningCollapseMatrix(this.getStackInSlot(1));
   }

   public ItemStack insertRecipeOutput(ItemStack stack, boolean simulate) {
      return this.insertItemUnchecked(2, stack, simulate);
   }

   public boolean canAcceptRecipeOutput(ItemStack stack) {
      return this.insertRecipeOutput(stack, true).m_41619_();
   }

   public void setClientRenderStack(int slot, ItemStack stack) {
      this.setStackInSlotUnchecked(slot, stack);
   }
}
