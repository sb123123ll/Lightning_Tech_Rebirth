package com.moakiee.ae2lt.celestweave.module;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import org.jetbrains.annotations.Nullable;

public final class ReflectSubmodule extends AbstractCelestweaveArmorSubmodule {
   public static final ReflectSubmodule INSTANCE = new ReflectSubmodule();

   private ReflectSubmodule() {
   }

   @Override
   public String id() {
      return "reflect";
   }

   @Override
   public String nameKey() {
      return "ae2lt.celestweave.feature.reflect.name";
   }

   @Override
   public String descriptionKey() {
      return "ae2lt.celestweave.feature.reflect.desc";
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
   public void onActivated(@Nullable Player player, Dist dist, ItemStack armor) {
   }

   @Override
   public void onDeactivated(@Nullable Player player, Dist dist, ItemStack armor) {
   }

   @Override
   public int tickActive(@Nullable Player player, Dist dist, ItemStack armor) {
      return 0;
   }
}
