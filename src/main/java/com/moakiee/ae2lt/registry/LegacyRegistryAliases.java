package com.moakiee.ae2lt.registry;

import net.minecraft.resources.ResourceLocation;

public final class LegacyRegistryAliases {
   private static final String MATRIX_PREFIX = "matter_warping_matrix_";
   private static boolean registered;

   private LegacyRegistryAliases() {
   }

   public static void register() {
      if (!registered) {
         registered = true;
         aliasTianshuComputeUnits();
         aliasBlockAndItem("matter_warping_matrix_blank_sub_core", "tianshu_blank_unit");
         aliasBlockAndItem("matter_warping_matrix_blank_unit", "tianshu_blank_unit");
         aliasMatrixBlock("thread_sub_core_t1", "thread_unit_t1");
         aliasMatrixBlock("thread_sub_core_t2", "thread_unit_t2");
         aliasMatrixBlock("cooling_sub_core_t1", "thermal_control_unit_t1");
         aliasMatrixBlock("cooling_sub_core_t2", "thermal_control_unit_t2");
         aliasMatrixBlock("creative_main_core", "multidimensional_main_core");
         aliasBlockAndItem("matter_warping_matrix_multiplier_sub_core_t1", "tianshu_amplifier_unit");
         aliasBlockAndItem("matter_warping_matrix_multiplier_sub_core_t2", "tianshu_amplifier_unit");
      }
   }

   private static void aliasTianshuComputeUnits() {
      aliasBlockAndItem("baseline_supercomputing_unit", "tianshu_baseline_main_core");
      aliasBlockAndItem("quantum_supercomputing_unit", "tianshu_quantum_main_core");
      aliasBlockAndItem("overload_supercomputing_unit", "tianshu_overload_main_core");
      aliasBlockAndItem("multidimensional_supercomputing_unit", "tianshu_multidimensional_main_core");
      aliasBlockAndItem("blank_supercomputing_unit", "tianshu_blank_unit");
      aliasBlockAndItem("amplifier_supercomputing_unit", "tianshu_amplifier_unit");
   }

   private static void aliasMatrixBlock(String oldSuffix, String newSuffix) {
      String target = "matter_warping_matrix_" + newSuffix;
      aliasBlockAndItem("matter_warping_matrix_" + oldSuffix, target);
   }

   private static void aliasBlockAndItem(String from, String to) {
      id(from);
      id(to);
   }

   private static ResourceLocation id(String path) {
      return new ResourceLocation("ae2lt", path);
   }
}
