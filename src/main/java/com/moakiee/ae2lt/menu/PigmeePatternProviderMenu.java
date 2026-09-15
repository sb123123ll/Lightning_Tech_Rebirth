package com.moakiee.ae2lt.menu;

import appeng.api.inventories.InternalInventory;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.RestrictedInputSlot;
import appeng.menu.slot.RestrictedInputSlot.PlacableItemType;
import appeng.util.ConfigMenuInventory;
import com.moakiee.ae2lt.blockentity.PigmeePatternProviderBlockEntity;
import com.moakiee.ae2lt.util.SlotPositionAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

public final class PigmeePatternProviderMenu extends AEBaseMenu {
   public static final MenuType<PigmeePatternProviderMenu> TYPE = Ae2ltMenuBuilder.buildUnregistered(
      MenuTypeBuilder.create(PigmeePatternProviderMenu::new, PigmeePatternProviderBlockEntity.class)
         .withMenuTitle(host -> Component.m_237115_("block.ae2lt.pigmee_pattern_provider")),
      new ResourceLocation("ae2lt", "pigmee_pattern_provider")
   );
   private static final int PATTERN_X = 62;
   private static final int PATTERN_Y = 49;
   private static final int RETURN_X = 8;
   private static final int RETURN_Y = 113;
   private static final int PLAYER_X = 8;
   private static final int PLAYER_Y = 147;
   private static final int HOTBAR_Y = 205;
   private static final int SLOT_SPACING = 18;

   public PigmeePatternProviderMenu(int id, Inventory playerInventory, PigmeePatternProviderBlockEntity host) {
      super(TYPE, id, playerInventory, host);
      InternalInventory patternInventory = host.getPatternInventory();

      for (int slot = 0; slot < 3; slot++) {
         RestrictedInputSlot patternSlot = new RestrictedInputSlot(PlacableItemType.ENCODED_PATTERN, patternInventory, slot);
         patternSlot.setIcon(null);
         SlotPositionAccess.set(patternSlot, 62 + slot * 18, 49);
         this.addSlot(patternSlot, SlotSemantics.ENCODED_PATTERN);
      }

      ConfigMenuInventory returnMenuInventory = host.getReturnInventory().createMenuWrapper();

      for (int slot = 0; slot < 9; slot++) {
         AppEngSlot returnSlot = new AppEngSlot(returnMenuInventory, slot);
         SlotPositionAccess.set(returnSlot, 8 + slot * 18, 113);
         this.addSlot(returnSlot, SlotSemantics.STORAGE);
      }

      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < 9; column++) {
            int inventorySlot = column + row * 9 + 9;
            this.addSlot(new Slot(playerInventory, inventorySlot, 8 + column * 18, 147 + row * 18), SlotSemantics.PLAYER_INVENTORY);
         }
      }

      for (int column = 0; column < 9; column++) {
         this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 205), SlotSemantics.PLAYER_HOTBAR);
      }
   }
}
