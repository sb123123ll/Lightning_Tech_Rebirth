package com.moakiee.ae2lt.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;

public class PerfectElectroChimeCrystalItem extends AE2LTItem {
   public PerfectElectroChimeCrystalItem(Properties properties) {
      super(properties.m_41487_(1));
   }

   public boolean m_5812_(ItemStack stack) {
      return true;
   }

   @Override
   public void appendHoverText(ItemStack stack, AE2LTItem.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
      tooltipComponents.add(Component.m_237115_("item.ae2lt.perfect_electro_chime_crystal.complete").m_130940_(ChatFormatting.AQUA));
   }
}
