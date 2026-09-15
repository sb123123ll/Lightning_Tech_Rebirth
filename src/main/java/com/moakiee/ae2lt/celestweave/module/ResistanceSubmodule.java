package com.moakiee.ae2lt.celestweave.module;

import java.util.List;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import org.jetbrains.annotations.Nullable;

public final class ResistanceSubmodule extends AbstractCelestweaveArmorSubmodule {
   public static final ResistanceSubmodule T1 = new ResistanceSubmodule(
      "matrix_shield", "ae2lt.celestweave.feature.matrix_shield.name", "ae2lt.celestweave.feature.matrix_shield.desc"
   );
   public static final ResistanceSubmodule T2 = new ResistanceSubmodule(
      "phase_shield", "ae2lt.celestweave.feature.phase_shield.name", "ae2lt.celestweave.feature.phase_shield.desc"
   );
   public static final String INSTALL_GROUP = "mitigation";
   public static final String HIT_FEEDBACK_CONFIG_KEY = "hit_feedback";
   private final String id;
   private final String nameKey;
   private final String descriptionKey;

   private ResistanceSubmodule(String id, String nameKey, String descriptionKey) {
      this.id = id;
      this.nameKey = nameKey;
      this.descriptionKey = descriptionKey;
   }

   @Override
   public String id() {
      return this.id;
   }

   @Override
   public String nameKey() {
      return this.nameKey;
   }

   @Override
   public String descriptionKey() {
      return this.descriptionKey;
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
   public String installGroupId() {
      return "mitigation";
   }

   @Override
   public void onActivated(@Nullable Player player, Dist dist, ItemStack armor) {
   }

   @Override
   public void onDeactivated(@Nullable Player player, Dist dist, ItemStack armor) {
   }

   @Override
   public List<CelestweaveArmorSubmoduleConfig> getConfigs(ItemStack armor) {
      return List.of(this.hitFeedbackConfig(armor));
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

   @Override
   public int tickActive(@Nullable Player player, Dist dist, ItemStack armor) {
      return 0;
   }

   public static boolean isHitFeedbackEnabled(ItemStack armor, String stage) {
      return submoduleForStage(stage).isHitFeedbackEnabled(armor);
   }

   private CelestweaveArmorSubmoduleConfig hitFeedbackConfig(ItemStack armor) {
      return this.config(
         "hit_feedback",
         Component.m_237115_("ae2lt.celestweave.config.hit_feedback"),
         ByteTag.m_128273_(this.isHitFeedbackEnabled(armor)),
         this.booleanChoices(),
         null
      );
   }

   private boolean isHitFeedbackEnabled(ItemStack armor) {
      CompoundTag options = this.getOptions(armor);
      return !options.m_128425_("hit_feedback", 1) ? true : options.m_128471_("hit_feedback");
   }

   private static ResistanceSubmodule submoduleForStage(String stage) {
      return "phase_shield".equals(stage) ? T2 : T1;
   }
}
