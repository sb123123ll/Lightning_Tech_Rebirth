package com.moakiee.ae2lt.celestweave.module;

import java.util.Arrays;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class MovementAssistSubmodule extends AbstractCelestweaveArmorSubmodule {
   public static final MovementAssistSubmodule INSTANCE = new MovementAssistSubmodule();
   public static final String WALK_SPEED_CONFIG_KEY = "walk_speed_multiplier";
   public static final String SPRINT_SPEED_CONFIG_KEY = "sprint_speed_multiplier";
   public static final String SNEAK_SPEED_CONFIG_KEY = "sneak_speed_multiplier";
   public static final String STEP_HEIGHT_CONFIG_KEY = "automatic_step_height";

   private MovementAssistSubmodule() {
   }

   @Override
   public String id() {
      return "movement_assist";
   }

   @Override
   public String nameKey() {
      return "ae2lt.celestweave.feature.movement_assist.name";
   }

   @Override
   public String descriptionKey() {
      return "ae2lt.celestweave.feature.movement_assist.desc";
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
      return List.of(
         this.speedConfig(armor, "walk_speed_multiplier"),
         this.speedConfig(armor, "sprint_speed_multiplier"),
         this.speedConfig(armor, "sneak_speed_multiplier"),
         this.stepHeightConfig(armor)
      );
   }

   @Override
   public boolean setConfig(ItemStack armor, String key, @Nullable Tag value) {
      CompoundTag options = this.getOptions(armor);
      if (isSpeedConfig(key)) {
         options.m_128365_(key, MovementSpeedOption.fromTag(value).toTag());
      } else {
         if (!"automatic_step_height".equals(key)) {
            return false;
         }

         options.m_128365_(key, StepHeightOption.fromTag(value).toTag());
      }

      this.setOptions(armor, options);
      return true;
   }

   public static double walkSpeedMultiplier(ItemStack armor) {
      return selectedSpeed(armor, "walk_speed_multiplier").multiplier();
   }

   public static double sprintSpeedMultiplier(ItemStack armor) {
      return selectedSpeed(armor, "sprint_speed_multiplier").multiplier();
   }

   public static double sneakSpeedMultiplier(ItemStack armor) {
      return selectedSpeed(armor, "sneak_speed_multiplier").multiplier();
   }

   public static double automaticStepHeight(ItemStack armor) {
      CompoundTag options = INSTANCE.getOptions(armor);
      return StepHeightOption.fromTag(options.m_128423_("automatic_step_height")).height();
   }

   private CelestweaveArmorSubmoduleConfig speedConfig(ItemStack armor, String key) {
      MovementSpeedOption selected = selectedSpeed(armor, key);
      return this.config(
         key,
         Component.m_237115_("ae2lt.celestweave.config." + key),
         selected.toTag(),
         this.speedChoices(),
         Component.m_237115_("ae2lt.celestweave.config." + key + ".hint")
      );
   }

   private CelestweaveArmorSubmoduleConfig stepHeightConfig(ItemStack armor) {
      StepHeightOption selected = StepHeightOption.fromTag(this.getOptions(armor).m_128423_("automatic_step_height"));
      return this.config(
         "automatic_step_height",
         Component.m_237115_("ae2lt.celestweave.config.automatic_step_height"),
         selected.toTag(),
         this.stepHeightChoices(),
         Component.m_237115_("ae2lt.celestweave.config.automatic_step_height.hint")
      );
   }

   private List<CelestweaveArmorSubmoduleConfigChoice> speedChoices() {
      return Arrays.stream(MovementSpeedOption.values())
         .map(option -> this.choice(option.toTag(), Component.m_237110_("ae2lt.celestweave.config.value.multiplier", new Object[]{option.label()})))
         .toList();
   }

   private List<CelestweaveArmorSubmoduleConfigChoice> stepHeightChoices() {
      return Arrays.stream(StepHeightOption.values())
         .map(option -> this.choice(option.toTag(), Component.m_237110_("ae2lt.celestweave.config.value.blocks", new Object[]{option.label()})))
         .toList();
   }

   private static MovementSpeedOption selectedSpeed(ItemStack armor, String key) {
      CompoundTag options = INSTANCE.getOptions(armor);
      return MovementSpeedOption.fromTag(options.m_128423_(key));
   }

   private static boolean isSpeedConfig(String key) {
      return "walk_speed_multiplier".equals(key) || "sprint_speed_multiplier".equals(key) || "sneak_speed_multiplier".equals(key);
   }
}
