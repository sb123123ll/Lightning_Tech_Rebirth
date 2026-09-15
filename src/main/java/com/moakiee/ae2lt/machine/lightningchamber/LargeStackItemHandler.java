package com.moakiee.ae2lt.machine.lightningchamber;

import appeng.api.inventories.InternalInventory;
import java.util.Objects;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

public abstract class LargeStackItemHandler implements IItemHandlerModifiable, InternalInventory {
   private static final String TAG_SLOT = "Slot";
   private static final String TAG_COUNT_INT = "CountInt";
   private static final String TAG_STACK = "Stack";
   private final NonNullList<ItemStack> stacks;
   @Nullable
   private final Runnable changeListener;

   protected LargeStackItemHandler(int size, @Nullable Runnable changeListener) {
      if (size <= 0) {
         throw new IllegalArgumentException("size must be positive");
      } else {
         this.stacks = NonNullList.m_122780_(size, ItemStack.f_41583_);
         this.changeListener = changeListener;
      }
   }

   public final int getSlots() {
      return this.stacks.size();
   }

   public abstract int getSlotLimit(int var1);

   public final int size() {
      return this.stacks.size();
   }

   public final ItemStack getStackInSlot(int slot) {
      this.validateSlotIndex(slot);
      return (ItemStack)this.stacks.get(slot);
   }

   public void setStackInSlot(int slot, ItemStack stack) {
      this.setStackInSlotInternal(slot, stack, true);
   }

   public final void setItemDirect(int slotIndex, ItemStack stack) {
      this.setStackInSlotUnchecked(slotIndex, stack);
   }

   protected final void setStackInSlotUnchecked(int slot, ItemStack stack) {
      this.setStackInSlotInternal(slot, stack, false);
   }

   private void setStackInSlotInternal(int slot, ItemStack stack, boolean validateItem) {
      this.validateSlotIndex(slot);
      Objects.requireNonNull(stack, "stack");
      if (!stack.m_41619_() && validateItem && !this.isItemValid(slot, stack)) {
         throw new IllegalArgumentException("Stack " + stack + " is not valid for slot " + slot);
      } else {
         this.stacks.set(slot, stack.m_41619_() ? ItemStack.f_41583_ : stack.m_41777_());
         this.onContentsChanged(slot);
      }
   }

   public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
      return this.insertItemInternal(slot, stack, simulate, true);
   }

   protected final ItemStack insertItemUnchecked(int slot, ItemStack stack, boolean simulate) {
      return this.insertItemInternal(slot, stack, simulate, false);
   }

   private ItemStack insertItemInternal(int slot, ItemStack stack, boolean simulate, boolean validateItem) {
      this.validateSlotIndex(slot);
      Objects.requireNonNull(stack, "stack");
      if (stack.m_41619_()) {
         return ItemStack.f_41583_;
      } else if (validateItem && !this.isItemValid(slot, stack)) {
         return stack;
      } else {
         ItemStack existing = (ItemStack)this.stacks.get(slot);
         if (!existing.m_41619_() && !ItemStack.m_150942_(existing, stack)) {
            return stack;
         } else {
            int freeSpace = this.getSlotLimit(slot) - existing.m_41613_();
            if (freeSpace <= 0) {
               return stack;
            } else {
               int toInsert = Math.min(stack.m_41613_(), freeSpace);
               if (toInsert <= 0) {
                  return stack;
               } else {
                  if (!simulate) {
                     ItemStack newStack;
                     if (existing.m_41619_()) {
                        newStack = stack.m_255036_(toInsert);
                     } else {
                        newStack = existing.m_41777_();
                        newStack.m_41769_(toInsert);
                     }

                     this.stacks.set(slot, newStack);
                     this.onContentsChanged(slot);
                  }

                  if (toInsert == stack.m_41613_()) {
                     return ItemStack.f_41583_;
                  } else {
                     ItemStack remainder = stack.m_41777_();
                     remainder.m_41774_(toInsert);
                     return remainder;
                  }
               }
            }
         }
      }
   }

   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      this.validateSlotIndex(slot);
      if (amount <= 0) {
         return ItemStack.f_41583_;
      } else {
         ItemStack existing = (ItemStack)this.stacks.get(slot);
         if (existing.m_41619_()) {
            return ItemStack.f_41583_;
         } else {
            int toExtract = Math.min(amount, existing.m_41613_());
            if (toExtract <= 0) {
               return ItemStack.f_41583_;
            } else {
               ItemStack extracted = existing.m_255036_(toExtract);
               if (!simulate) {
                  if (toExtract == existing.m_41613_()) {
                     this.stacks.set(slot, ItemStack.f_41583_);
                  } else {
                     ItemStack reduced = existing.m_41777_();
                     reduced.m_41774_(toExtract);
                     this.stacks.set(slot, reduced);
                  }

                  this.onContentsChanged(slot);
               }

               return extracted;
            }
         }
      }
   }

   public boolean isItemValid(int slot, ItemStack stack) {
      this.validateSlotIndex(slot);
      return true;
   }

   protected final void validateSlotIndex(int slot) {
      if (slot < 0 || slot >= this.stacks.size()) {
         throw new IllegalArgumentException("Slot " + slot + " not in valid range - [0," + this.stacks.size() + ")");
      }
   }

   protected void onContentsChanged(int slot) {
      if (this.changeListener != null) {
         this.changeListener.run();
      }
   }

   public void sendChangeNotification(int slot) {
      this.validateSlotIndex(slot);
      this.onContentsChanged(slot);
   }

   public final void clear() {
      for (int slot = 0; slot < this.stacks.size(); slot++) {
         if (!((ItemStack)this.stacks.get(slot)).m_41619_()) {
            this.stacks.set(slot, ItemStack.f_41583_);
            this.onContentsChanged(slot);
         }
      }
   }

   public final void saveToTag(CompoundTag tag, String key) {
      if (this.isEmpty()) {
         tag.m_128473_(key);
      } else {
         ListTag items = new ListTag();

         for (int slot = 0; slot < this.stacks.size(); slot++) {
            ItemStack stack = (ItemStack)this.stacks.get(slot);
            if (!stack.m_41619_()) {
               CompoundTag itemTag = new CompoundTag();
               itemTag.m_128405_("Slot", slot);
               itemTag.m_128405_("CountInt", stack.m_41613_());
               Tag stackTag = stack.m_255036_(1).m_41739_(new CompoundTag());
               itemTag.m_128365_("Stack", stackTag);
               items.add(itemTag);
            }
         }

         tag.m_128365_(key, items);
      }
   }

   public final void loadFromTag(CompoundTag tag, String key) {
      for (int slot = 0; slot < this.stacks.size(); slot++) {
         this.stacks.set(slot, ItemStack.f_41583_);
      }

      if (tag.m_128425_(key, 9)) {
         ListTag items = tag.m_128437_(key, 10);

         for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.m_128728_(i);
            int slot = itemTag.m_128451_("Slot");
            if (slot >= 0 && slot < this.stacks.size()) {
               ItemStack stack = itemTag.m_128425_("Stack", 10) ? ItemStack.m_41712_(itemTag.m_128469_("Stack")) : ItemStack.m_41712_(itemTag);
               if (!stack.m_41619_()) {
                  int savedCount = itemTag.m_128425_("CountInt", 3) ? itemTag.m_128451_("CountInt") : stack.m_41613_();
                  stack = stack.m_255036_(Math.max(1, savedCount));
                  this.stacks.set(slot, stack);
               }
            }
         }
      }
   }

   public final boolean isEmpty() {
      for (ItemStack stack : this.stacks) {
         if (!stack.m_41619_()) {
            return false;
         }
      }

      return true;
   }
}
