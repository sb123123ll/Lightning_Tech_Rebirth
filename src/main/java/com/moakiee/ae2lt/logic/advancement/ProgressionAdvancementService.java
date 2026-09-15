package com.moakiee.ae2lt.logic.advancement;

import com.moakiee.ae2lt.item.FixedInfiniteCellItem;
import com.moakiee.ae2lt.registry.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ProgressionAdvancementService {
   private static final ResourceLocation ANNIHILATION = advancement("annihilation");
   private static final ResourceLocation NEUTRALIZATION = advancement("neutralization");
   private static final ResourceLocation CONSTRUCT_FRAGMENT = advancement("construct_fragment");
   private static final ResourceLocation THUNDERSTORM_GENERATOR = advancement("thunderstorm_generator");
   private static final ResourceLocation OBSERVABLE_BLACK_HOLE = advancement("observable_black_hole");

   private ProgressionAdvancementService() {
   }

   public static void awardOverloadTntIgnited(ServerPlayer player) {
      award(player, ANNIHILATION, "ignite_overload_tnt");
   }

   public static void inspectMysteriousCell(ServerPlayer player, ItemStack stack) {
      if (stack.m_150930_((Item)ModItems.MYSTERIOUS_CELL.get())) {
         if (!FixedInfiniteCellItem.hasType(stack)) {
            award(player, NEUTRALIZATION, "obtain_mysterious_component");
         } else {
            switch (FixedInfiniteCellItem.CellOutcome.fromTypeId(FixedInfiniteCellItem.getType(stack))) {
               case LIGHTNING_ROD:
                  award(player, CONSTRUCT_FRAGMENT, "obtain_infinite_lightning_rod");
                  break;
               case HIGH_VOLTAGE:
                  award(player, THUNDERSTORM_GENERATOR, "obtain_infinite_high_voltage");
                  break;
               case EXTREME_HIGH_VOLTAGE:
                  award(player, THUNDERSTORM_GENERATOR, "obtain_infinite_extreme_high_voltage");
                  break;
               case LIGHTNING_COLLAPSE_MATRIX:
                  award(player, OBSERVABLE_BLACK_HOLE, "obtain_infinite_collapse_matrix");
               case RESEARCH_NOTE:
               case MOAKIEE_FUMO:
               case CYSTRYSU_FUMO:
            }
         }
      }
   }

   private static ResourceLocation advancement(String path) {
      return new ResourceLocation("ae2lt", "main/" + path);
   }

   private static void award(ServerPlayer player, ResourceLocation id, String criterion) {
      Advancement advancement = player.f_8924_.m_129889_().m_136041_(id);
      if (advancement != null) {
         player.m_8960_().m_135988_(advancement, criterion);
      }
   }
}
