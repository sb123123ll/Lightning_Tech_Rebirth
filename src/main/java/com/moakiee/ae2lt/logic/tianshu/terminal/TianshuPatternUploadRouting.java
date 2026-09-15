package com.moakiee.ae2lt.logic.tianshu.terminal;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import com.moakiee.ae2lt.item.ClosedLoopPatternItem;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class TianshuPatternUploadRouting {
   private static final ResourceLocation MATTER_WARPING_MATRIX_CONTROLLER_ID = new ResourceLocation("ae2lt", "matter_warping_matrix_controller");
   private static final Set<ResourceLocation> CRAFTING_UPLOAD_GROUP_IDS = Set.of(
      new ResourceLocation("ae2", "molecular_assembler"),
      new ResourceLocation("expatternprovider", "ex_molecular_assembler"),
      new ResourceLocation("expatternprovider", "assembler_matrix_pattern"),
      new ResourceLocation("extendedae_plus", "assembler_matrix_pattern_plus"),
      new ResourceLocation("neoecoae", "crafting_system_l4"),
      new ResourceLocation("neoecoae", "crafting_system_l6"),
      new ResourceLocation("neoecoae", "crafting_system_l9"),
      new ResourceLocation("ae2cs", "meteorite_pattern_provider"),
      MATTER_WARPING_MATRIX_CONTROLLER_ID
   );

   private TianshuPatternUploadRouting() {
   }

   public static TianshuPatternUploadRouting.Route forEncodingMode(TianshuEncodingMode mode) {
      if (mode == null) {
         return TianshuPatternUploadRouting.Route.INVALID;
      } else {
         return switch (mode) {
            case CLOSED_LOOP -> TianshuPatternUploadRouting.Route.CLOSED_LOOP_STORAGE;
            case CRAFTING, STONECUTTING, SMITHING_TABLE -> TianshuPatternUploadRouting.Route.CRAFTING_ASSEMBLER;
            case PROCESSING -> TianshuPatternUploadRouting.Route.PROCESSING_PROVIDER;
         };
      }
   }

   public static TianshuPatternUploadRouting.Route classify(ItemStack stack, Level level) {
      if (stack != null && !stack.m_41619_() && level != null) {
         if (stack.m_41720_() instanceof ClosedLoopPatternItem item) {
            return item.readPayload(stack, level).isPresent()
               ? TianshuPatternUploadRouting.Route.CLOSED_LOOP_STORAGE
               : TianshuPatternUploadRouting.Route.INVALID;
         } else {
            IPatternDetails details = PatternDetailsHelper.decodePattern(stack, level);
            if (details == null) {
               return TianshuPatternUploadRouting.Route.INVALID;
            } else {
               return !details.supportsPushInputsToExternalInventory()
                  ? TianshuPatternUploadRouting.Route.CRAFTING_ASSEMBLER
                  : TianshuPatternUploadRouting.Route.PROCESSING_PROVIDER;
            }
         }
      } else {
         return TianshuPatternUploadRouting.Route.INVALID;
      }
   }

   public static boolean isCraftingUploadGroup(PatternContainerGroup group) {
      return group != null && group.icon() != null && isCraftingUploadGroupId(group.icon().getId());
   }

   static boolean isCraftingUploadGroupId(ResourceLocation id) {
      return id != null && CRAFTING_UPLOAD_GROUP_IDS.contains(id);
   }

   public static boolean isMatterWarpingMatrixGroup(PatternContainerGroup group) {
      return group != null && group.icon() != null && isMatterWarpingMatrixId(group.icon().getId());
   }

   static boolean isMatterWarpingMatrixId(ResourceLocation id) {
      return MATTER_WARPING_MATRIX_CONTROLLER_ID.equals(id);
   }

   public static boolean isValidEncodingResult(ItemStack stack, Level level) {
      return classify(stack, level) != TianshuPatternUploadRouting.Route.INVALID;
   }

   public static enum Route {
      CLOSED_LOOP_STORAGE,
      CRAFTING_ASSEMBLER,
      PROCESSING_PROVIDER,
      INVALID;
   }
}
