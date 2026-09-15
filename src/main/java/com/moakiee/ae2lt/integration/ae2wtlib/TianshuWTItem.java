package com.moakiee.ae2lt.integration.ae2wtlib;

import com.moakiee.ae2lt.menu.TianshuWirelessPatternEncodingTermMenu;
import de.mari_023.ae2wtlib.terminal.ItemWT;
import de.mari_023.ae2wtlib.wut.ItemWUT;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public final class TianshuWTItem extends ItemWT {
   private static final String DESCRIPTION_ID = "item.ae2lt.wireless_tianshu_pattern_encoding_terminal";

   public MenuType<?> getMenuType() {
      return TianshuWirelessPatternEncodingTermMenu.TYPE;
   }

   public MenuType<?> getMenuType(ItemStack stack) {
      return TianshuWirelessPatternEncodingTermMenu.TYPE;
   }

   public boolean checkUniversalPreconditions(ItemStack stack, Player player) {
      if (!stack.m_41619_() && !player.m_9236_().m_5776_() && (stack.m_41720_() == this || stack.m_41720_() instanceof ItemWUT)) {
         WirelessTerminalFrequencyLink.Resolution frequencyRoute = WirelessTerminalFrequencyLink.resolveRoute(player, stack);
         return frequencyRoute.usesFrequencyRoute() ? frequencyRoute.isNetworkPowered() : super.checkUniversalPreconditions(stack, player);
      } else {
         return false;
      }
   }

   public String m_5524_() {
      return "item.ae2lt.wireless_tianshu_pattern_encoding_terminal";
   }
}
