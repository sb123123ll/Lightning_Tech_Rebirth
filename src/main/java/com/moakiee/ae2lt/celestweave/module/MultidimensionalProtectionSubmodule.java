package com.moakiee.ae2lt.celestweave.module;

import java.util.List;
import java.util.Set;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class MultidimensionalProtectionSubmodule extends AbstractCelestweaveArmorSubmodule {
   public static final MultidimensionalProtectionSubmodule INSTANCE = new MultidimensionalProtectionSubmodule();
   public static final String ID = "multidimensional_protection";

   private MultidimensionalProtectionSubmodule() {
   }

   @Override
   public String id() {
      return "multidimensional_protection";
   }

   @Override
   public String nameKey() {
      return "ae2lt.celestweave.feature.multidimensional_protection.name";
   }

   @Override
   public String descriptionKey() {
      return "ae2lt.celestweave.feature.multidimensional_protection.desc";
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
   public Set<String> installGroupIds() {
      return Set.of("mitigation", UndyingSubmodule.INSTANCE.installGroupId());
   }

   @Override
   public List<CelestweaveArmorSubmoduleConfig> getConfigs(ItemStack armor) {
      return List.of(
         this.config(
            "hit_feedback",
            Component.m_237115_("ae2lt.celestweave.config.hit_feedback"),
            ByteTag.m_128273_(isHitFeedbackEnabled(armor)),
            this.booleanChoices(),
            null
         )
      );
   }

   @Override
   public boolean setConfig(ItemStack armor, String key, @Nullable Tag value) {
      if (!"hit_feedback".equals(key)) {
         return false;
      } else {
         CompoundTag options = this.getOptions(armor);
         options.m_128365_("hit_feedback", value instanceof ByteTag byteTag ? byteTag : ByteTag.m_128273_(true));
         this.setOptions(armor, options);
         return true;
      }
   }

   public static boolean isHitFeedbackEnabled(ItemStack armor) {
      CompoundTag options = INSTANCE.getOptions(armor);
      return !options.m_128425_("hit_feedback", 1) ? true : options.m_128471_("hit_feedback");
   }
}
