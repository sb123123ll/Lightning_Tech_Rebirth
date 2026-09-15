package com.moakiee.ae2lt.item;

import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.moakiee.ae2lt.menu.OverloadPatternEncoderHost;
import com.moakiee.ae2lt.menu.OverloadPatternEncoderMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class OverloadPatternEncoderItem extends AE2LTItem implements IMenuItem {
   public OverloadPatternEncoderItem(Properties properties) {
      super(properties.m_41487_(1));
   }

   @Nullable
   public OverloadPatternEncoderHost getMenuHost(Player player, int inventorySlot, ItemStack stack, @Nullable BlockPos pos) {
      return new OverloadPatternEncoderHost(player, inventorySlot, stack);
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
      if (!level.m_5776_()) {
         MenuOpener.open(OverloadPatternEncoderMenu.TYPE, player, MenuLocators.forHand(player, hand));
      }

      return new InteractionResultHolder(InteractionResult.m_19078_(level.m_5776_()), player.m_21120_(hand));
   }
}
