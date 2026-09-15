package com.moakiee.ae2lt.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

public final class CelestweaveArmorRenderExtensions implements IClientItemExtensions {
   public static final CelestweaveArmorRenderExtensions INSTANCE = new CelestweaveArmorRenderExtensions();

   private CelestweaveArmorRenderExtensions() {
   }

   @NotNull
   public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
      original.m_8009_(false);
      return original;
   }
}
