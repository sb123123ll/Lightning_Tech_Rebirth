package com.moakiee.ae2lt.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public class FumoBlockItem extends AE2LTBlockItem implements Equipable {
   @Nullable
   private final String tooltipKey;

   public FumoBlockItem(Block block, Properties properties) {
      this(block, properties, null);
   }

   public FumoBlockItem(Block block, Properties properties, String tooltipKey) {
      super(block, properties);
      this.tooltipKey = tooltipKey;
   }

   public EquipmentSlot m_40402_() {
      return EquipmentSlot.HEAD;
   }

   @Override
   public void appendHoverText(ItemStack stack, AE2LTBlockItem.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
      super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
      if (this.tooltipKey != null) {
         tooltipComponents.add(Component.m_237115_(this.tooltipKey + ".1").m_130940_(ChatFormatting.GRAY));
         tooltipComponents.add(Component.m_237115_(this.tooltipKey + ".2").m_130940_(ChatFormatting.GRAY));
         tooltipComponents.add(Component.m_237115_(this.tooltipKey + ".3").m_130940_(ChatFormatting.GRAY));
         tooltipComponents.add(Component.m_237115_(this.tooltipKey + ".4").m_130940_(ChatFormatting.LIGHT_PURPLE));
      }
   }
}
