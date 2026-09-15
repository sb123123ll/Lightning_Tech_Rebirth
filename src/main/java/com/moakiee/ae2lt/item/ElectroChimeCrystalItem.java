package com.moakiee.ae2lt.item;

import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.util.ItemStackTagSupport;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;

public class ElectroChimeCrystalItem extends AE2LTItem {
   private static final String TAG_CATALYSIS = "ae2lt.catalysis_value";
   private static final double STAGE_1_FRACTION = 0.2;
   private static final double STAGE_2_FRACTION = 0.55;
   private static final double STAGE_3_FRACTION = 0.85;

   public ElectroChimeCrystalItem(Properties properties) {
      super(properties.m_41487_(1));
   }

   public static int getMaxCatalysis() {
      return AE2LTCommonConfig.electroChimeMaxCatalysis();
   }

   public static int getCatalysisValue(ItemStack stack) {
      return Mth.m_14045_(ItemStackTagSupport.getTagCopy(stack).m_128451_("ae2lt.catalysis_value"), 0, getMaxCatalysis());
   }

   public static void setCatalysisValue(ItemStack stack, int catalysisValue) {
      int clamped = Mth.m_14045_(catalysisValue, 0, getMaxCatalysis());
      ItemStackTagSupport.updateTag(stack, tag -> tag.m_128405_("ae2lt.catalysis_value", clamped));
   }

   public static int addCatalysis(ItemStack stack, int amount) {
      int next = Mth.m_14045_(getCatalysisValue(stack) + Math.max(0, amount), 0, getMaxCatalysis());
      setCatalysisValue(stack, next);
      return next;
   }

   public static double getCatalysisPercent(ItemStack stack) {
      return (double)getCatalysisValue(stack) / (double)getMaxCatalysis();
   }

   public static int getCatalysisStage(ItemStack stack) {
      return getCatalysisStage(getCatalysisValue(stack));
   }

   public static int getCatalysisStage(int catalysisValue) {
      if (catalysisValue >= stageThreshold(2)) {
         return 3;
      } else if (catalysisValue >= stageThreshold(1)) {
         return 2;
      } else {
         return catalysisValue >= stageThreshold(0) ? 1 : 0;
      }
   }

   public static Component getStageName(ItemStack stack) {
      return getStageName(getCatalysisStage(stack));
   }

   public static Component getStageName(int stage) {
      return Component.m_237115_("item.ae2lt.electro_chime_crystal.stage." + stage);
   }

   @Override
   public void appendHoverText(ItemStack stack, AE2LTItem.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
      tooltipComponents.add(
         Component.m_237110_("item.ae2lt.electro_chime_crystal.percent", new Object[]{String.format("%.1f", getCatalysisPercent(stack) * 100.0)})
            .m_130940_(ChatFormatting.AQUA)
      );
      tooltipComponents.add(
         Component.m_237110_("item.ae2lt.electro_chime_crystal.stage", new Object[]{getStageName(stack)}).m_130940_(ChatFormatting.LIGHT_PURPLE)
      );
   }

   public static int rollCatalysisFeed(RandomSource random) {
      int min = Math.max(1, AE2LTCommonConfig.electroChimeCatalysisPerStrikeMin());
      int max = Math.max(min, AE2LTCommonConfig.electroChimeCatalysisPerStrikeMax());
      return min + random.m_188503_(max - min + 1);
   }

   private static int stageThreshold(int stage) {
      int maxCatalysis = getMaxCatalysis();
      int stage1 = Mth.m_14045_(Mth.m_14165_((double)maxCatalysis * 0.2), 1, maxCatalysis);
      int stage2 = Mth.m_14045_(Mth.m_14165_((double)maxCatalysis * 0.55), stage1, maxCatalysis);
      int stage3 = Mth.m_14045_(Mth.m_14165_((double)maxCatalysis * 0.85), stage2, maxCatalysis);

      return switch (stage) {
         case 0 -> stage1;
         case 1 -> stage2;
         default -> stage3;
      };
   }
}
