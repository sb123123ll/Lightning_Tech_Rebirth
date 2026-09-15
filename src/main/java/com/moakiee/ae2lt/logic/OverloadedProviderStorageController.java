package com.moakiee.ae2lt.logic;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.patternprovider.PatternProviderReturnInventory;
import appeng.util.inv.AppEngInternalInventory;
import com.moakiee.ae2lt.blockentity.OverloadedPatternProviderBlockEntity;
import com.moakiee.ae2lt.mixin.PatternProviderLogicAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.LoggerFactory;

final class OverloadedProviderStorageController {
   private static final String TAG_RESTORE_OVERFLOW = "ae2lt:restore_overflow";
   private final OverloadedPatternProviderBlockEntity host;
   private final PatternProviderLogicAccessor logic;
   private final int totalCapacity;
   private final List<GenericStack> pendingRestore = new ArrayList<>();
   private boolean needsSavedDataLoad;

   OverloadedProviderStorageController(OverloadedPatternProviderBlockEntity host, PatternProviderLogicAccessor logic, int totalCapacity) {
      this.host = host;
      this.logic = logic;
      this.totalCapacity = totalCapacity;
   }

   boolean hasPendingRestore() {
      return !this.pendingRestore.isEmpty();
   }

   void writeToNBT(CompoundTag tag) {
      if (!this.pendingRestore.isEmpty()) {
         ListTag list = new ListTag();

         for (GenericStack stack : this.pendingRestore) {
            list.add(GenericStack.writeTag(stack));
         }

         tag.m_128365_("ae2lt:restore_overflow", list);
      }
   }

   void readFromNBT(CompoundTag tag, PatternProviderReturnInventory returnInventory) {
      this.needsSavedDataLoad = this.totalCapacity > 36 && !this.hasPatternInventoryContents();
      this.pendingRestore.clear();
      this.salvageTruncatedNbtSlots(tag, returnInventory);
      if (tag.m_128425_("ae2lt:restore_overflow", 9)) {
         this.pendingRestore.addAll(readGenericStackList(tag.m_128437_("ae2lt:restore_overflow", 10)));
      }
   }

   void drainPendingRestore(OverloadedProviderStorageController.NetworkInserter networkInserter, Runnable saveChanges) {
      if (!this.pendingRestore.isEmpty()) {
         boolean changed = false;
         int i = 0;

         while (i < this.pendingRestore.size()) {
            GenericStack stack = this.pendingRestore.get(i);
            long remaining = networkInserter.insert(stack.what(), stack.amount());
            if (remaining <= 0L) {
               this.pendingRestore.remove(i);
               changed = true;
            } else {
               if (remaining != stack.amount()) {
                  this.pendingRestore.set(i, new GenericStack(stack.what(), remaining));
                  changed = true;
               }

               i++;
            }
         }

         if (changed) {
            saveChanges.run();
         }
      }
   }

   void addDrops(List<ItemStack> drops) {
      for (GenericStack stack : this.pendingRestore) {
         stack.what().addDrops(stack.amount(), drops, this.host.m_58904_(), this.host.m_58899_());
      }

      if (this.totalCapacity > 36) {
         this.removeSavedData();
      }
   }

   void clear() {
      if (this.totalCapacity > 36) {
         this.removeSavedData();
      }

      this.pendingRestore.clear();
   }

   boolean loadOnReady() {
      if (!this.needsSavedDataLoad) {
         return false;
      } else {
         this.needsSavedDataLoad = false;
         return this.loadFromSavedData();
      }
   }

   void removeSavedData() {
      if (this.host.m_58904_() instanceof ServerLevel serverLevel) {
         PatternStorageSavedData.get(serverLevel).remove(this.host.m_58899_().m_121878_());
      }
   }

   private boolean loadFromSavedData() {
      Level level = this.host.m_58904_();
      if (level instanceof ServerLevel serverLevel) {
         PatternStorageSavedData savedData = PatternStorageSavedData.get(serverLevel);
         ItemStack[] stored = savedData.get(this.host.m_58899_().m_121878_());
         if (stored == null) {
            LoggerFactory.getLogger("ae2lt").info("[SavedData] No stored data for pos={}", this.host.m_58899_());
            return false;
         } else {
            LoggerFactory.getLogger("ae2lt").info("[SavedData] Loaded {} patterns for pos={}", stored.length, this.host.m_58899_());
            AppEngInternalInventory inventory = this.logic.getPatternInventory();
            int limit = Math.min(stored.length, inventory.size());

            for (int i = 0; i < limit; i++) {
               inventory.setItemDirect(i, stored[i] != null ? stored[i] : ItemStack.f_41583_);
            }

            int salvaged = 0;

            for (int i = limit; i < stored.length; i++) {
               ItemStack stack = stored[i];
               AEItemKey key = stack != null ? AEItemKey.of(stack) : null;
               if (key != null && !stack.m_41619_()) {
                  this.pendingRestore.add(new GenericStack(key, (long)stack.m_41613_()));
                  salvaged++;
               }
            }

            if (salvaged > 0) {
               LoggerFactory.getLogger("ae2lt")
                  .warn("[SavedData] Capacity at {} smaller than stored data; salvaged {} patterns into the restore queue", this.host.m_58899_(), salvaged);
            }

            savedData.remove(this.host.m_58899_().m_121878_());
            return true;
         }
      } else {
         LoggerFactory.getLogger("ae2lt").warn("[SavedData] loadFromSavedData skipped: level={} pos={}", level, this.host.m_58899_());
         return false;
      }
   }

   private boolean hasPatternInventoryContents() {
      AppEngInternalInventory inventory = this.logic.getPatternInventory();

      for (int i = 0; i < inventory.size(); i++) {
         if (!inventory.getStackInSlot(i).m_41619_()) {
            return true;
         }
      }

      return false;
   }

   private void salvageTruncatedNbtSlots(CompoundTag tag, PatternProviderReturnInventory returnInventory) {
      int salvaged = 0;
      int patternInventorySize = this.logic.getPatternInventory().size();
      ListTag patternsTag = tag.m_128437_("patterns", 10);

      for (int i = 0; i < patternsTag.size(); i++) {
         CompoundTag itemTag = patternsTag.m_128728_(i);
         if (itemTag.m_128451_("Slot") >= patternInventorySize) {
            ItemStack stack = ItemStack.m_41712_(itemTag);
            AEItemKey key = AEItemKey.of(stack);
            if (key != null && !stack.m_41619_()) {
               this.pendingRestore.add(new GenericStack(key, (long)stack.m_41613_()));
               salvaged++;
            }
         }
      }

      ListTag returnTag = tag.m_128437_("returnInv", 10);

      for (int ix = returnInventory.size(); ix < returnTag.size(); ix++) {
         GenericStack stack = GenericStack.readTag(returnTag.m_128728_(ix));
         if (stack != null && stack.amount() > 0L) {
            this.pendingRestore.add(stack);
            salvaged++;
         }
      }

      if (salvaged > 0) {
         LoggerFactory.getLogger("ae2lt")
            .warn("Pattern provider at {} shrank below its saved size; salvaged {} stacks into the restore queue", this.host.m_58899_(), salvaged);
      }
   }

   private static List<GenericStack> readGenericStackList(ListTag list) {
      ArrayList<GenericStack> stacks = new ArrayList<>(list.size());

      for (int i = 0; i < list.size(); i++) {
         GenericStack stack = GenericStack.readTag(list.m_128728_(i));
         if (stack != null && stack.amount() > 0L) {
            stacks.add(stack);
         }
      }

      return stacks;
   }

   @FunctionalInterface
   interface NetworkInserter {
      long insert(AEKey var1, long var2);
   }
}
