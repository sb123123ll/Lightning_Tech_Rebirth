package com.moakiee.ae2lt.recipe;

import com.moakiee.ae2lt.block.MatrixFormedBlock;
import com.moakiee.ae2lt.block.TianshuSupercomputingUnitBlock;
import com.moakiee.ae2lt.item.FixedInfiniteCellItem;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.ae2lt.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

final class PigmeeConversionLogic {
   private PigmeeConversionLogic() {
   }

   static boolean canConvert(ItemStack target) {
      return identify(target) != null;
   }

   static ItemStack createResult(ItemStack target) {
      PigmeeConversionLogic.Conversion conversion = identify(target);
      if (conversion == null) {
         return ItemStack.f_41583_;
      } else {
         return switch (conversion) {
            case HIGH_VOLTAGE -> FixedInfiniteCellItem.createDisplayedResultStack(FixedInfiniteCellItem.CellOutcome.HIGH_VOLTAGE);
            case EXTREME_HIGH_VOLTAGE -> FixedInfiniteCellItem.createDisplayedResultStack(FixedInfiniteCellItem.CellOutcome.EXTREME_HIGH_VOLTAGE);
            case LIGHTNING_COLLAPSE_MATRIX -> FixedInfiniteCellItem.createDisplayedResultStack(FixedInfiniteCellItem.CellOutcome.LIGHTNING_COLLAPSE_MATRIX);
            case INFINITE_STORAGE -> new ItemStack((ItemLike)ModItems.INFINITE_STORAGE_CELL.get());
            case MULTIDIMENSIONAL_SUPERCOMPUTER -> new ItemStack(
            ((TianshuSupercomputingUnitBlock)ModBlocks.MULTIDIMENSIONAL_SUPERCOMPUTING_UNIT.get()).m_5456_()
         );
            case MULTIDIMENSIONAL_MATRIX -> new ItemStack(((MatrixFormedBlock)ModBlocks.MATTER_WARPING_MATRIX_MULTIDIMENSIONAL_MAIN_CORE.get()).m_5456_());
            case MULTIDIMENSIONAL_EXECUTION -> new ItemStack((ItemLike)ModItems.RAILGUN_MODULE_MULTIDIMENSIONAL_EXECUTION.get());
            case MULTIDIMENSIONAL_PROTECTION -> new ItemStack((ItemLike)ModItems.CELESTWEAVE_SUBMODULE_MULTIDIMENSIONAL_PROTECTION.get());
         };
      }
   }

   @Nullable
   private static PigmeeConversionLogic.Conversion identify(ItemStack target) {
      if (target.m_150930_(Items.f_151041_)) {
         return PigmeeConversionLogic.Conversion.HIGH_VOLTAGE;
      } else if (target.m_150930_((Item)ModItems.THUNDERSTORM_CONDENSATE.get())) {
         return PigmeeConversionLogic.Conversion.EXTREME_HIGH_VOLTAGE;
      } else if (target.m_150930_((Item)ModItems.LIGHTNING_COLLAPSE_MATRIX.get())) {
         return PigmeeConversionLogic.Conversion.LIGHTNING_COLLAPSE_MATRIX;
      } else if (target.m_150930_((Item)ModItems.BULK_LIGHTNING_CELL_COMPONENT.get())) {
         return PigmeeConversionLogic.Conversion.INFINITE_STORAGE;
      } else if (target.m_150930_(((TianshuSupercomputingUnitBlock)ModBlocks.OVERLOAD_SUPERCOMPUTING_UNIT.get()).m_5456_())) {
         return PigmeeConversionLogic.Conversion.MULTIDIMENSIONAL_SUPERCOMPUTER;
      } else if (target.m_150930_(((MatrixFormedBlock)ModBlocks.MATTER_WARPING_MATRIX_OVERLOAD_MAIN_CORE.get()).m_5456_())) {
         return PigmeeConversionLogic.Conversion.MULTIDIMENSIONAL_MATRIX;
      } else if (target.m_150930_((Item)ModItems.RAILGUN_MODULE_OVERLOAD_EXECUTION.get())) {
         return PigmeeConversionLogic.Conversion.MULTIDIMENSIONAL_EXECUTION;
      } else {
         return target.m_150930_((Item)ModItems.CELESTWEAVE_SUBMODULE_PHASE_SHIELD.get()) ? PigmeeConversionLogic.Conversion.MULTIDIMENSIONAL_PROTECTION : null;
      }
   }

   private static enum Conversion {
      HIGH_VOLTAGE,
      EXTREME_HIGH_VOLTAGE,
      LIGHTNING_COLLAPSE_MATRIX,
      INFINITE_STORAGE,
      MULTIDIMENSIONAL_SUPERCOMPUTER,
      MULTIDIMENSIONAL_MATRIX,
      MULTIDIMENSIONAL_EXECUTION,
      MULTIDIMENSIONAL_PROTECTION;
   }
}
