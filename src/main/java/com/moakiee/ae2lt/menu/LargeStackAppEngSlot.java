package com.moakiee.ae2lt.menu;

import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import java.util.ArrayList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class LargeStackAppEngSlot extends AppEngSlot {
   private final InternalInventory backingInventory;
   private final int backingSlot;
   private ItemStack synchronizedDisplayStack = ItemStack.f_41583_;
   private boolean hasSynchronizedDisplayStack;

   public LargeStackAppEngSlot(InternalInventory inventory, int slot) {
      super(inventory, slot);
      this.backingInventory = inventory;
      this.backingSlot = slot;
      this.setHideAmount(true);
      this.setNotDraggable();
   }

   public ItemStack m_7993_() {
      if (!this.isSlotEnabled()) {
         return ItemStack.f_41583_;
      } else {
         return this.isRemote() && this.hasSynchronizedDisplayStack ? this.synchronizedDisplayStack : toPresentationStack(this.getBackingItem());
      }
   }

   public void initialize(ItemStack stack) {
      if (this.isRemote()) {
         this.setSynchronizedDisplayStack(stack);
      } else {
         super.initialize(stack);
      }
   }

   public void m_5852_(ItemStack stack) {
      if (this.isRemote()) {
         this.setSynchronizedDisplayStack(stack);
      } else {
         super.m_5852_(stack);
      }
   }

   public int m_5866_(ItemStack stack) {
      return this.m_6641_();
   }

   public boolean m_5857_(ItemStack stack) {
      return this.isSlotEnabled() && !stack.m_41619_() && !GenericStack.isWrapped(stack) && this.backingInventory.isItemValid(this.backingSlot, stack);
   }

   public ItemStack m_150656_(ItemStack stack, int increment) {
      if (!stack.m_41619_() && increment > 0 && this.m_5857_(stack)) {
         int offeredCount = Math.min(increment, stack.m_41613_());
         ItemStack offered = stack.m_255036_(offeredCount);
         ItemStack rejected = this.backingInventory.insertItem(this.backingSlot, offered, false);
         int inserted = offeredCount - rejected.m_41613_();
         if (inserted > 0) {
            stack.m_41774_(inserted);
            this.m_6654_();
         }

         return stack;
      } else {
         return stack;
      }
   }

   public ItemStack m_6201_(int amount) {
      ItemStack actual = this.getBackingItem();
      return !actual.m_41619_() && amount > 0
         ? this.backingInventory.extractItem(this.backingSlot, Math.min(amount, actual.m_41741_()), false)
         : ItemStack.f_41583_;
   }

   private ItemStack getBackingItem() {
      return this.backingInventory.getStackInSlot(this.backingSlot);
   }

   public long getDisplayedAmount() {
      ItemStack displayed = this.m_7993_();
      GenericStack wrapped = GenericStack.unwrapItemStack(displayed);
      return wrapped != null ? wrapped.amount() : (long)displayed.m_41613_();
   }

   private void setSynchronizedDisplayStack(ItemStack stack) {
      this.synchronizedDisplayStack = stack.m_41619_() ? ItemStack.f_41583_ : stack.m_41777_();
      this.hasSynchronizedDisplayStack = true;
   }

   private static ItemStack toPresentationStack(ItemStack actual) {
      if (!actual.m_41619_() && !GenericStack.isWrapped(actual)) {
         AEItemKey key = AEItemKey.of(actual);
         return key != null && actual.m_41613_() > key.getMaxStackSize() ? GenericStack.wrapInItemStack(key, (long)actual.m_41613_()) : actual;
      } else {
         return actual;
      }
   }

   private int insertIntoBacking(ItemStack stack, int amount) {
      if (amount <= 0) {
         return 0;
      } else {
         int offeredCount = Math.min(amount, stack.m_41613_());
         ItemStack offered = stack.m_255036_(offeredCount);
         ItemStack rejected = this.backingInventory.insertItem(this.backingSlot, offered, false);
         int inserted = offeredCount - rejected.m_41613_();
         if (inserted > 0) {
            this.m_6654_();
         }

         return inserted;
      }
   }

   public static boolean mustRejectDirectExtraction(int button, ClickType clickType) {
      return clickType == ClickType.SWAP;
   }

   public static boolean handleMenuInteraction(AEBaseMenu menu, int slotId, int button, ClickType clickType, Player player) {
      if (slotId < 0 || slotId >= menu.f_38839_.size() || !(menu.m_38853_(slotId) instanceof LargeStackAppEngSlot slot)) {
         return false;
      } else if (menu.isClientSide()) {
         return true;
      } else if (mustRejectDirectExtraction(button, clickType)) {
         return true;
      } else {
         switch (clickType) {
            case PICKUP:
               handlePickup(menu, slot, button, player);
               break;
            case QUICK_MOVE:
               handleQuickMove(menu, slot, player);
               break;
            case THROW:
               handleThrow(slot, button, player);
               break;
            case CLONE:
               handleClone(menu, slot, player);
               break;
            case PICKUP_ALL:
               handlePickupAll(menu, slot, player);
            case QUICK_CRAFT:
            case SWAP:
         }

         return true;
      }
   }

   private static void handlePickup(AEBaseMenu menu, LargeStackAppEngSlot slot, int button, Player player) {
      if (button == 0 || button == 1) {
         ItemStack carried = menu.m_142621_();
         ItemStack slotStack = slot.getBackingItem();
         boolean rightClick = button == 1;
         if (carried.m_41619_()) {
            if (!slotStack.m_41619_() && slot.canExtractBacking()) {
               int nativeMax = slotStack.m_41741_();
               int requested = rightClick ? Math.min(nativeMax, Math.max(1, (int)Math.ceil((double)slotStack.m_41613_() / 2.0))) : nativeMax;
               ItemStack taken = slot.m_6201_(requested);
               menu.m_142503_(taken);
               slot.m_142406_(player, taken);
               slot.m_6654_();
            }
         } else if (slotStack.m_41619_()) {
            if (slot.m_5857_(carried)) {
               int toMove = Math.min(rightClick ? 1 : carried.m_41613_(), slot.m_5866_(carried));
               if (toMove > 0) {
                  int inserted = slot.insertIntoBacking(carried, toMove);
                  carried.m_41774_(inserted);
                  menu.m_142503_(carried.m_41619_() ? ItemStack.f_41583_ : carried);
               }
            }
         } else if (ItemStack.m_150942_(slotStack, carried)) {
            if (!slot.m_5857_(carried)) {
               if (slot.canExtractBacking()) {
                  int cursorRoom = carried.m_41741_() - carried.m_41613_();
                  ItemStack taken = slot.m_6201_(cursorRoom);
                  if (!taken.m_41619_()) {
                     carried.m_41769_(taken.m_41613_());
                     menu.m_142503_(carried);
                     slot.m_142406_(player, taken);
                     slot.m_6654_();
                  }
               }
            } else {
               int room = slot.m_5866_(carried) - slotStack.m_41613_();
               int requested = Math.min(rightClick ? 1 : carried.m_41613_(), room);
               int inserted = slot.insertIntoBacking(carried, requested);
               carried.m_41774_(inserted);
               menu.m_142503_(carried.m_41619_() ? ItemStack.f_41583_ : carried);
            }
         } else if (slot.m_5857_(carried)
            && slot.canExtractBacking()
            && carried.m_41613_() <= slot.m_5866_(carried)
            && slotStack.m_41613_() <= slotStack.m_41741_()) {
            slot.m_5852_(carried);
            menu.m_142503_(slotStack.m_41777_());
         }
      }
   }

   private static void handleQuickMove(AEBaseMenu menu, LargeStackAppEngSlot slot, Player player) {
      ItemStack actual = slot.getBackingItem();
      if (!actual.m_41619_() && slot.canExtractBacking()) {
         ItemStack taken = slot.m_6201_(actual.m_41741_());
         if (!taken.m_41619_()) {
            ItemStack original = taken.m_41777_();
            ItemStack remainder = moveIntoPlayerInventory(menu, taken);
            int moved = original.m_41613_() - remainder.m_41613_();
            if (!remainder.m_41619_()) {
               int restored = slot.insertIntoBacking(remainder, remainder.m_41613_());
               remainder.m_41774_(restored);
               if (!remainder.m_41619_()) {
                  player.m_36176_(remainder, false);
               }
            }

            if (moved > 0) {
               ItemStack movedStack = original.m_255036_(moved);
               slot.m_142406_(player, movedStack);
               slot.m_6654_();
            }
         }
      }
   }

   private static ItemStack moveIntoPlayerInventory(AEBaseMenu menu, ItemStack stack) {
      ArrayList<Slot> destinations = new ArrayList<>(menu.getSlots(SlotSemantics.PLAYER_INVENTORY));
      destinations.addAll(menu.getSlots(SlotSemantics.PLAYER_HOTBAR));
      ItemStack remainder = stack;

      for (Slot destination : destinations) {
         if (destination.m_6657_()) {
            remainder = destination.m_150659_(remainder);
            if (remainder.m_41619_()) {
               return ItemStack.f_41583_;
            }
         }
      }

      for (Slot destinationx : destinations) {
         if (!destinationx.m_6657_()) {
            remainder = destinationx.m_150659_(remainder);
            if (remainder.m_41619_()) {
               return ItemStack.f_41583_;
            }
         }
      }

      return remainder;
   }

   private static void handleThrow(LargeStackAppEngSlot slot, int button, Player player) {
      ItemStack actual = slot.getBackingItem();
      if (!actual.m_41619_() && slot.canExtractBacking()) {
         int amount = button == 1 ? actual.m_41741_() : 1;
         ItemStack taken = slot.m_6201_(amount);
         if (!taken.m_41619_()) {
            player.m_36176_(taken, true);
            slot.m_142406_(player, taken);
            slot.m_6654_();
         }
      }
   }

   private static void handleClone(AEBaseMenu menu, LargeStackAppEngSlot slot, Player player) {
      if (player.m_150110_().f_35937_) {
         ItemStack actual = slot.getBackingItem();
         if (!actual.m_41619_()) {
            menu.m_142503_(actual.m_255036_(actual.m_41741_()));
         }
      }
   }

   private static void handlePickupAll(AEBaseMenu menu, LargeStackAppEngSlot slot, Player player) {
      ItemStack carried = menu.m_142621_();
      ItemStack actual = slot.getBackingItem();
      if (!carried.m_41619_() && !actual.m_41619_() && ItemStack.m_150942_(actual, carried) && slot.canExtractBacking()) {
         int room = carried.m_41741_() - carried.m_41613_();
         ItemStack taken = slot.m_6201_(room);
         if (!taken.m_41619_()) {
            carried.m_41769_(taken.m_41613_());
            menu.m_142503_(carried);
            slot.m_142406_(player, taken);
            slot.m_6654_();
         }
      }
   }

   private boolean canExtractBacking() {
      return this.isSlotEnabled() && !this.backingInventory.extractItem(this.backingSlot, 1, true).m_41619_();
   }
}
