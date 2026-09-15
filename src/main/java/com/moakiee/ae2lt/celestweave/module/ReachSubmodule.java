package com.moakiee.ae2lt.celestweave.module;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ReachSubmodule extends AbstractCelestweaveArmorSubmodule {
   public static final ReachSubmodule INSTANCE = new ReachSubmodule();

   private ReachSubmodule() {
   }

   @Override
   public String id() {
      return "reach_extension";
   }

   @Override
   public String nameKey() {
      return "ae2lt.celestweave.feature.reach_extension.name";
   }

   @Override
   public String descriptionKey() {
      return "ae2lt.celestweave.feature.reach_extension.desc";
   }

   @Override
   public boolean defaultEnabled() {
      return true;
   }

   @Override
   public int getMaxInstallAmount() {
      return 1;
   }

   @Override
   public List<CelestweaveArmorSubmoduleConfig> getConfigs(ItemStack armor) {
      return List.of(this.rangeConfig(armor));
   }

   @Override
   public boolean setConfig(ItemStack armor, String key, @Nullable Tag value) {
      if ("reach_range".equals(key)) {
         ReachDistanceOption option = ReachDistanceOption.fromTag(value);
         CompoundTag options = this.getOptions(armor);
         options.m_128365_("reach_range", option.toTag());
         this.setOptions(armor, options);
         return true;
      } else {
         return false;
      }
   }

   public static ReachDistanceOption selectedRange(ItemStack armor) {
      return INSTANCE.getSelectedRange(armor);
   }

   public static double blockBonus(ItemStack armor) {
      return selectedRange(armor).blockBonus();
   }

   public static double entityBonus(ItemStack armor) {
      return selectedRange(armor).entityBonus();
   }

   private CelestweaveArmorSubmoduleConfig rangeConfig(ItemStack armor) {
      return this.config(
         "reach_range",
         Component.m_237115_("ae2lt.celestweave.config.reach_range"),
         this.getSelectedRange(armor).toTag(),
         this.rangeChoices(),
         Component.m_237115_("ae2lt.celestweave.config.reach_range.hint")
      );
   }

   private List<CelestweaveArmorSubmoduleConfigChoice> rangeChoices() {
      return List.of(
         this.choice(ReachDistanceOption.ONE.toTag(), Component.m_237113_(ReachDistanceOption.ONE.label())),
         this.choice(ReachDistanceOption.TWO.toTag(), Component.m_237113_(ReachDistanceOption.TWO.label())),
         this.choice(ReachDistanceOption.FOUR.toTag(), Component.m_237113_(ReachDistanceOption.FOUR.label()))
      );
   }

   private ReachDistanceOption getSelectedRange(ItemStack armor) {
      CompoundTag options = this.getOptions(armor);
      return ReachDistanceOption.fromTag(options.m_128423_("reach_range"));
   }
}
