package com.moakiee.ae2lt.menu;

import appeng.api.storage.StorageCells;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.AppEngSlot;
import com.moakiee.ae2lt.blockentity.TianshuSeedStorageBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class TianshuSeedStorageMenu extends AEBaseMenu {
   public static final MenuType<TianshuSeedStorageMenu> TYPE = MenuTypeBuilder.create(TianshuSeedStorageMenu::new, TianshuSeedStorageBlockEntity.class)
      .withMenuTitle(host -> Component.m_237115_("block.ae2lt.closed_loop_seed_storage"))
      .build("closed_loop_seed_storage");
   private final TianshuSeedStorageBlockEntity host;
   private final List<Slot> cellSlots = new ArrayList<>();

   public TianshuSeedStorageMenu(int id, Inventory playerInventory, TianshuSeedStorageBlockEntity host) {
      super(TYPE, id, playerInventory, host);
      this.host = host;

      for (int slot = 0; slot < 10; slot++) {
         this.cellSlots.add(this.addSlot(new AppEngSlot(host.getCellInventory(), slot), SlotSemantics.STORAGE_CELL));
      }

      this.createPlayerInventorySlots(playerInventory);
   }

   public ItemStack m_7648_(Player player, int index) {
      if (!this.isClientSide() && index >= 0 && index < this.f_38839_.size()) {
         Slot sourceSlot = this.m_38853_(index);
         if (sourceSlot.m_6657_() && sourceSlot.m_8010_(player)) {
            ItemStack original = sourceSlot.m_7993_().m_41777_();
            ItemStack remainder;
            if (this.isPlayerSideSlot(sourceSlot)) {
               if (!StorageCells.isCellHandled(original)) {
                  return ItemStack.f_41583_;
               }

               remainder = moveIntoSlots(original.m_41777_(), this.cellSlots);
            } else {
               remainder = moveIntoSlots(original.m_41777_(), this.getPlayerDestinationSlots());
            }

            int moved = original.m_41613_() - remainder.m_41613_();
            if (moved <= 0) {
               return ItemStack.f_41583_;
            } else {
               sourceSlot.m_6201_(moved);
               sourceSlot.m_6654_();
               return original;
            }
         } else {
            return ItemStack.f_41583_;
         }
      } else {
         return ItemStack.f_41583_;
      }
   }

   public boolean m_6875_(Player player) {
      return !this.host.m_58901_()
         && this.host.m_58904_() != null
         && this.host.m_58904_().m_7702_(this.host.m_58899_()) == this.host
         && player.m_9236_() == this.host.m_58904_()
         && player.m_20238_(this.host.m_58899_().m_252807_()) <= 64.0;
   }

   private boolean isPlayerSideSlot(Slot slot) {
      return this.getSlots(SlotSemantics.PLAYER_INVENTORY).contains(slot) || this.getSlots(SlotSemantics.PLAYER_HOTBAR).contains(slot);
   }

   private List<Slot> getPlayerDestinationSlots() {
      ArrayList<Slot> result = new ArrayList<>(this.getSlots(SlotSemantics.PLAYER_INVENTORY));
      result.addAll(this.getSlots(SlotSemantics.PLAYER_HOTBAR));
      return result;
   }

   private static ItemStack moveIntoSlots(ItemStack stack, List<Slot> destinations) {
      ItemStack remainder = stack;

      for (Slot slot : destinations) {
         if (slot.m_6657_()) {
            remainder = slot.m_150659_(remainder);
            if (remainder.m_41619_()) {
               return ItemStack.f_41583_;
            }
         }
      }

      for (Slot slotx : destinations) {
         if (!slotx.m_6657_()) {
            remainder = slotx.m_150659_(remainder);
            if (remainder.m_41619_()) {
               return ItemStack.f_41583_;
            }
         }
      }

      return remainder;
   }
}
