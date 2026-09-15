package com.moakiee.ae2lt.menu;

import appeng.api.networking.IGridNode;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.MenuTypeBuilder.MenuFactory;
import appeng.menu.slot.RestrictedInputSlot;
import appeng.menu.slot.RestrictedInputSlot.PlacableItemType;
import com.moakiee.ae2lt.integration.ae2wtlib.TianshuWTMenuHost;
import de.mari_023.ae2wtlib.AE2wtlibSlotSemantics;
import de.mari_023.ae2wtlib.terminal.WTMenuHost;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public final class TianshuWirelessPatternEncodingTermMenu extends TianshuPatternEncodingTermMenu {
   private static final MenuFactory<TianshuWirelessPatternEncodingTermMenu, TianshuWTMenuHost> FACTORY = TianshuWirelessPatternEncodingTermMenu::new;
   public static final MenuType<TianshuWirelessPatternEncodingTermMenu> TYPE = Ae2ltMenuBuilder.buildUnregistered(
      MenuTypeBuilder.create(FACTORY, TianshuWTMenuHost.class), new ResourceLocation("ae2lt", "wireless_tianshu_pattern_encoding_terminal")
   );
   private final TianshuWTMenuHost wirelessHost;

   public TianshuWirelessPatternEncodingTermMenu(int id, Inventory inventory, TianshuWTMenuHost host) {
      super(TYPE, id, inventory, host);
      this.wirelessHost = host;
      this.addSlot(
         new RestrictedInputSlot(PlacableItemType.QE_SINGULARITY, host.getSubInventory(WTMenuHost.INV_SINGULARITY), 0), AE2wtlibSlotSemantics.SINGULARITY
      );
   }

   public IGridNode getNetworkNode() {
      return this.wirelessHost.getActionableNode();
   }

   public boolean isWUT() {
      return this.tianshuHost.isUniversalWirelessTerminal();
   }
}
