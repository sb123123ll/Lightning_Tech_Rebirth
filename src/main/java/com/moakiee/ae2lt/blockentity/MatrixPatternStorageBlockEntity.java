package com.moakiee.ae2lt.blockentity;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.BaseInternalInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.helpers.patternprovider.PatternContainer;
import com.moakiee.ae2lt.block.MatrixControllerBlock;
import com.moakiee.ae2lt.block.MatrixMultiblockComponentBlock;
import com.moakiee.ae2lt.logic.craft.MatrixMultiblockComponent;
import com.moakiee.ae2lt.logic.craft.MatrixPatternCore;
import com.moakiee.ae2lt.logic.craft.MatrixPatternStorageTier;
import com.moakiee.ae2lt.logic.terminal.InternalPatternContainerLink;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.ae2lt.util.NativeStackDropHelper;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandlerModifiable;

public class MatrixPatternStorageBlockEntity extends BlockEntity implements MatrixPatternCore, PatternContainer {
   private static final String TAG_ITEMS = "Items";
   private static final String TAG_SLOT = "Slot";
   private static final String TAG_STACK = "Stack";
   private static final int T1_CAPACITY = 36;
   private static final int T2_CAPACITY = 72;
   private final NonNullList<ItemStack> items = NonNullList.m_122780_(72, ItemStack.f_41583_);
   private final MatrixPatternStorageBlockEntity.PatternInventory inventory = new MatrixPatternStorageBlockEntity.PatternInventory();
   private final InternalInventory terminalPatternInventory = new MatrixPatternStorageBlockEntity.TerminalPatternInventory();
   private final InternalPatternContainerLink terminalLink;
   private final List<IPatternDetails> cachedPatterns = new ArrayList<>();
   private BlockPos controllerPos;
   private boolean patternsDirty = true;
   private int usedSlots;

   public MatrixPatternStorageBlockEntity(BlockPos pos, BlockState blockState) {
      super((BlockEntityType)ModBlockEntities.MATRIX_PATTERN_STORAGE.get(), pos, blockState);
      this.terminalLink = new InternalPatternContainerLink(this, blockState.m_60734_());
   }

   public MatrixPatternStorageTier tier() {
      return this.component().patternStorageTier();
   }

   public int capacity() {
      return this.tier() == MatrixPatternStorageTier.T2 ? 72 : 36;
   }

   public MatrixPatternStorageBlockEntity.PatternInventory getInventory() {
      return this.inventory;
   }

   public boolean isEmpty() {
      return this.usedSlots == 0;
   }

   public boolean hasFreeSlot() {
      return this.usedSlots < this.capacity();
   }

   public List<ItemStack> copyContents() {
      ArrayList<ItemStack> result = new ArrayList<>(this.capacity());

      for (int slot = 0; slot < this.capacity(); slot++) {
         result.add(this.inventory.getStackInSlot(slot).m_41777_());
      }

      return result;
   }

   public void dropStoredPatterns(Level level, BlockPos pos) {
      List<ItemStack> drops = this.copyContents();
      this.inventory.clear();
      this.patternsDirty = true;
      this.setChangedAndUpdate();

      for (ItemStack stack : drops) {
         if (!stack.m_41619_()) {
            NativeStackDropHelper.popResource(level, pos, stack);
         }
      }
   }

   public void loadContents(List<ItemStack> stacks) {
      for (int slot = 0; slot < this.capacity(); slot++) {
         ItemStack stack = slot < stacks.size() ? stacks.get(slot) : ItemStack.f_41583_;
         this.inventory.setStackInSlotInternal(slot, stack, false);
      }

      this.patternsDirty = true;
      this.setChangedAndUpdate();
   }

   public BlockPos getControllerPos() {
      return this.controllerPos;
   }

   public void setControllerPos(BlockPos controllerPos) {
      this.controllerPos = controllerPos == null ? null : controllerPos.m_7949_();
      if (controllerPos == null) {
         this.terminalLink.disconnect();
      }
   }

   public void bindToController(BlockPos controllerPos, MatrixPortBlockEntity port) {
      this.setControllerPos(controllerPos);
      if (controllerPos != null && port != null) {
         this.terminalLink.bind(port.getMainNode());
      } else {
         this.terminalLink.disconnect();
      }
   }

   public IGrid getGrid() {
      return this.terminalLink.getGrid();
   }

   public boolean isVisibleInTerminal() {
      return this.controllerPos != null && this.terminalLink.isActive();
   }

   public InternalInventory getTerminalPatternInventory() {
      return this.terminalPatternInventory;
   }

   public long getTerminalSortOrder() {
      BlockPos pos = this.m_58899_();
      return (long)pos.m_123343_() << 24 ^ (long)pos.m_123341_() << 8 ^ (long)pos.m_123342_();
   }

   public PatternContainerGroup getTerminalGroup() {
      return new PatternContainerGroup(
         AEItemKey.of((ItemLike)ModBlocks.MATTER_WARPING_MATRIX_CONTROLLER.get()),
         ((MatrixControllerBlock)ModBlocks.MATTER_WARPING_MATRIX_CONTROLLER.get()).m_49954_(),
         List.of(Component.m_237110_("ae2lt.matrix.terminal.tooltip", new Object[]{1, this.capacity()}))
      );
   }

   public boolean isT1() {
      return this.tier() == MatrixPatternStorageTier.T1;
   }

   public boolean isValidPatternStack(ItemStack stack) {
      if (stack.m_41619_()) {
         return true;
      } else if (!PatternDetailsHelper.isEncodedPattern(stack)) {
         return false;
      } else if (this.f_58857_ == null) {
         return true;
      } else {
         IPatternDetails details = PatternDetailsHelper.decodePattern(stack, this.f_58857_);
         return details instanceof IMolecularAssemblerSupportedPattern;
      }
   }

   @Override
   public List<IPatternDetails> getAvailablePatterns() {
      if (this.patternsDirty) {
         this.rebuildPatternCache();
      }

      return List.copyOf(this.cachedPatterns);
   }

   @Override
   public boolean hasPattern(IPatternDetails details) {
      if (details == null) {
         return false;
      } else {
         if (this.patternsDirty) {
            this.rebuildPatternCache();
         }

         for (IPatternDetails pattern : this.cachedPatterns) {
            if (MatrixPatternCore.samePattern(pattern, details)) {
               return true;
            }
         }

         return false;
      }
   }

   private void rebuildPatternCache() {
      if (this.f_58857_ != null) {
         this.cachedPatterns.clear();
         this.patternsDirty = false;

         for (int slot = 0; slot < this.capacity(); slot++) {
            ItemStack stack = this.inventory.getStackInSlot(slot);
            if (!stack.m_41619_()) {
               IPatternDetails details = PatternDetailsHelper.decodePattern(stack, this.f_58857_);
               if (details instanceof IMolecularAssemblerSupportedPattern) {
                  this.cachedPatterns.add(details);
               }
            }
         }
      }
   }

   private MatrixMultiblockComponent component() {
      return this.m_58900_().m_60734_() instanceof MatrixMultiblockComponentBlock componentBlock
         ? componentBlock.matrixComponent(this.m_58900_())
         : MatrixMultiblockComponent.PATTERN_STORAGE_T1;
   }

   private void setChangedAndUpdate() {
      this.m_6596_();
      if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
         this.notifyPortPatternsChanged();
      }
   }

   private void notifyPortPatternsChanged() {
      if (this.controllerPos != null && this.f_58857_ != null && this.f_58857_.m_46749_(this.controllerPos)) {
         if (this.f_58857_.m_7702_(this.controllerPos) instanceof MatrixControllerBlockEntity controller) {
            BlockPos portPos = controller.getPortPos();
            if (portPos != null && this.f_58857_.m_46749_(portPos) && this.f_58857_.m_7702_(portPos) instanceof MatrixPortBlockEntity port) {
               port.patternsChanged();
            }
         }
      }
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      ListTag items = new ListTag();

      for (int slot = 0; slot < this.capacity(); slot++) {
         ItemStack stack = this.inventory.getStackInSlot(slot);
         if (!stack.m_41619_()) {
            CompoundTag itemTag = new CompoundTag();
            itemTag.m_128405_("Slot", slot);
            itemTag.m_128365_("Stack", stack.m_41739_(new CompoundTag()));
            items.add(itemTag);
         }
      }

      tag.m_128365_("Items", items);
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      this.controllerPos = null;
      this.inventory.clear();
      this.patternsDirty = true;
      if (tag.m_128425_("Items", 9)) {
         ListTag items = tag.m_128437_("Items", 10);

         for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.m_128728_(i);
            int slot = itemTag.m_128451_("Slot");
            if (slot >= 0 && slot < this.capacity() && itemTag.m_128425_("Stack", 10)) {
               ItemStack stack = ItemStack.m_41712_(itemTag.m_128469_("Stack"));
               this.inventory.setStackInSlotInternal(slot, stack, false);
            }
         }
      }
   }

   public CompoundTag m_5995_() {
      return new CompoundTag();
   }

   public Packet<ClientGamePacketListener> m_58483_() {
      return ClientboundBlockEntityDataPacket.m_195640_(this);
   }

   public void onChunkUnloaded() {
      this.terminalLink.disconnect();
      super.onChunkUnloaded();
   }

   public void m_7651_() {
      this.terminalLink.disconnect();
      super.m_7651_();
   }

   public final class PatternInventory implements IItemHandlerModifiable {
      public int getSlots() {
         return MatrixPatternStorageBlockEntity.this.capacity();
      }

      public ItemStack getStackInSlot(int slot) {
         this.validateSlot(slot);
         return (ItemStack)MatrixPatternStorageBlockEntity.this.items.get(slot);
      }

      public void setStackInSlot(int slot, ItemStack stack) {
         this.setStackInSlotInternal(slot, stack, true);
      }

      private void setStackInSlotInternal(int slot, ItemStack stack, boolean validatePattern) {
         this.validateSlot(slot);
         ItemStack previous = (ItemStack)MatrixPatternStorageBlockEntity.this.items.get(slot);
         if (stack != null && !stack.m_41619_()) {
            ItemStack copy = stack.m_255036_(1);
            if (validatePattern && !MatrixPatternStorageBlockEntity.this.isValidPatternStack(copy)) {
               throw new IllegalArgumentException("Stack is not a molecular assembler pattern: " + copy);
            }

            MatrixPatternStorageBlockEntity.this.items.set(slot, copy);
         } else {
            MatrixPatternStorageBlockEntity.this.items.set(slot, ItemStack.f_41583_);
         }

         this.updateUsedSlots(previous, (ItemStack)MatrixPatternStorageBlockEntity.this.items.get(slot));
         MatrixPatternStorageBlockEntity.this.patternsDirty = true;
         MatrixPatternStorageBlockEntity.this.setChangedAndUpdate();
      }

      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         this.validateSlot(slot);
         if (stack.m_41619_() || !((ItemStack)MatrixPatternStorageBlockEntity.this.items.get(slot)).m_41619_()) {
            return stack;
         } else if (!MatrixPatternStorageBlockEntity.this.isValidPatternStack(stack)) {
            return stack;
         } else {
            if (!simulate) {
               ItemStack previous = (ItemStack)MatrixPatternStorageBlockEntity.this.items.get(slot);
               MatrixPatternStorageBlockEntity.this.items.set(slot, stack.m_255036_(1));
               this.updateUsedSlots(previous, (ItemStack)MatrixPatternStorageBlockEntity.this.items.get(slot));
               MatrixPatternStorageBlockEntity.this.patternsDirty = true;
               MatrixPatternStorageBlockEntity.this.setChangedAndUpdate();
            }

            if (stack.m_41613_() == 1) {
               return ItemStack.f_41583_;
            } else {
               ItemStack remainder = stack.m_41777_();
               remainder.m_41774_(1);
               return remainder;
            }
         }
      }

      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         this.validateSlot(slot);
         if (amount <= 0) {
            return ItemStack.f_41583_;
         } else {
            ItemStack existing = (ItemStack)MatrixPatternStorageBlockEntity.this.items.get(slot);
            if (existing.m_41619_()) {
               return ItemStack.f_41583_;
            } else {
               ItemStack extracted = existing.m_255036_(1);
               if (!simulate) {
                  ItemStack previous = (ItemStack)MatrixPatternStorageBlockEntity.this.items.get(slot);
                  MatrixPatternStorageBlockEntity.this.items.set(slot, ItemStack.f_41583_);
                  this.updateUsedSlots(previous, ItemStack.f_41583_);
                  MatrixPatternStorageBlockEntity.this.patternsDirty = true;
                  MatrixPatternStorageBlockEntity.this.setChangedAndUpdate();
               }

               return extracted;
            }
         }
      }

      public int getSlotLimit(int slot) {
         this.validateSlot(slot);
         return 1;
      }

      public boolean isItemValid(int slot, ItemStack stack) {
         this.validateSlot(slot);
         return MatrixPatternStorageBlockEntity.this.isValidPatternStack(stack);
      }

      private void clear() {
         for (int slot = 0; slot < MatrixPatternStorageBlockEntity.this.items.size(); slot++) {
            MatrixPatternStorageBlockEntity.this.items.set(slot, ItemStack.f_41583_);
         }

         MatrixPatternStorageBlockEntity.this.usedSlots = 0;
         MatrixPatternStorageBlockEntity.this.patternsDirty = true;
      }

      private void updateUsedSlots(ItemStack previous, ItemStack current) {
         boolean wasEmpty = previous == null || previous.m_41619_();
         boolean isEmpty = current == null || current.m_41619_();
         if (wasEmpty && !isEmpty) {
            MatrixPatternStorageBlockEntity.this.usedSlots++;
         } else if (!wasEmpty && isEmpty) {
            MatrixPatternStorageBlockEntity.this.usedSlots--;
         }
      }

      private void validateSlot(int slot) {
         if (slot < 0 || slot >= MatrixPatternStorageBlockEntity.this.capacity()) {
            throw new IllegalArgumentException("Slot " + slot + " not in valid range [0," + MatrixPatternStorageBlockEntity.this.capacity() + ")");
         }
      }
   }

   private final class TerminalPatternInventory extends BaseInternalInventory {
      public int size() {
         return MatrixPatternStorageBlockEntity.this.capacity();
      }

      public ItemStack getStackInSlot(int slotIndex) {
         return MatrixPatternStorageBlockEntity.this.inventory.getStackInSlot(slotIndex);
      }

      public void setItemDirect(int slotIndex, ItemStack stack) {
         MatrixPatternStorageBlockEntity.this.inventory.setStackInSlot(slotIndex, stack == null ? ItemStack.f_41583_ : stack);
      }

      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         return MatrixPatternStorageBlockEntity.this.inventory.insertItem(slot, stack, simulate);
      }

      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         return MatrixPatternStorageBlockEntity.this.inventory.extractItem(slot, amount, simulate);
      }

      public int getSlotLimit(int slot) {
         return MatrixPatternStorageBlockEntity.this.inventory.getSlotLimit(slot);
      }

      public boolean isItemValid(int slot, ItemStack stack) {
         return MatrixPatternStorageBlockEntity.this.inventory.isItemValid(slot, stack);
      }
   }
}
