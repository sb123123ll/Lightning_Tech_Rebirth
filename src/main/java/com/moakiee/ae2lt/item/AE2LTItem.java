package com.moakiee.ae2lt.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class AE2LTItem extends Item {
   public AE2LTItem(Properties properties) {
      super(properties);
   }

   public final void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
      this.appendHoverText(stack, new AE2LTItem.TooltipContext(level), tooltipComponents, tooltipFlag);
   }

   public void appendHoverText(ItemStack stack, AE2LTItem.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
   }

   public static final class TooltipContext {
      private final Level level;

      public TooltipContext(@Nullable Level level) {
         this.level = level;
      }

      @Nullable
      public Level level() {
         return this.level;
      }
   }
}
