package com.moakiee.ae2lt.menu;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import com.moakiee.ae2lt.me.cell.VoidCellData;
import com.moakiee.ae2lt.me.cell.VoidCellMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public final class VoidCellMenu extends AEBaseMenu {
   private static final String ACTION_SET_MODE = "setMode";
   public static final MenuType<VoidCellMenu> TYPE = Ae2ltMenuBuilder.buildUnregistered(
      MenuTypeBuilder.create(VoidCellMenu::new, ItemMenuHost.class), new ResourceLocation("ae2lt", "void_cell")
   );
   private final ItemStack stack;
   @GuiSync(1)
   private int mode;

   public VoidCellMenu(int id, Inventory playerInventory, ItemMenuHost host) {
      super(TYPE, id, playerInventory, host);
      this.stack = host.getItemStack();
      this.mode = VoidCellData.readMode(this.stack).ordinal();
      this.registerClientAction("setMode", Integer.class, this::setModeFromClient);
   }

   public void m_38946_() {
      if (this.isServerSide()) {
         this.mode = VoidCellData.readMode(this.stack).ordinal();
      }

      super.m_38946_();
   }

   public VoidCellMode getMode() {
      return VoidCellMode.fromOrdinal(this.mode);
   }

   public void selectMode(VoidCellMode selectedMode) {
      if (this.isClientSide()) {
         this.mode = selectedMode.ordinal();
         this.sendClientAction("setMode", this.mode);
      } else {
         this.setMode(selectedMode);
      }
   }

   private void setModeFromClient(int ordinal) {
      if (this.isServerSide() && ordinal >= 0 && ordinal < VoidCellMode.values().length) {
         this.setMode(VoidCellMode.values()[ordinal]);
      }
   }

   private void setMode(VoidCellMode selectedMode) {
      VoidCellData.writeMode(this.stack, selectedMode);
      this.mode = selectedMode.ordinal();
      this.m_38946_();
   }
}
