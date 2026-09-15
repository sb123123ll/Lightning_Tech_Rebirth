package com.moakiee.ae2lt.menu;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class OverloadPatternEncoderHost extends ItemMenuHost {
   public OverloadPatternEncoderHost(Player player, int inventorySlot, ItemStack stack) {
      super(player, inventorySlot, stack);
   }
}
