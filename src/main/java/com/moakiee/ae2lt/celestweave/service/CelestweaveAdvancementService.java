package com.moakiee.ae2lt.celestweave.service;

import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class CelestweaveAdvancementService {
   private static final ResourceLocation RADIATION_ASSIMILATION = new ResourceLocation("ae2lt", "main/radiation_assimilation");
   private static final String RADIATION_HEALING_CRITERION = "radiation_healing";

   private CelestweaveAdvancementService() {
   }

   public static void awardRadiationAssimilation(ServerPlayer player) {
      Advancement advancement = player.f_8924_.m_129889_().m_136041_(RADIATION_ASSIMILATION);
      if (advancement != null) {
         player.m_8960_().m_135988_(advancement, "radiation_healing");
      }
   }
}
