package com.moakiee.ae2lt.blockentity;

import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.BaseInternalInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEItemKey;
import appeng.helpers.patternprovider.PatternContainer;
import com.moakiee.ae2lt.block.TianshuPatternStorageBlock;
import com.moakiee.ae2lt.item.ClosedLoopPatternItem;
import com.moakiee.ae2lt.logic.terminal.InternalPatternContainerLink;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopPatternPayload;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopPatternRepository;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopPatternValidator;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.ae2lt.registry.ModItems;
import com.moakiee.ae2lt.util.NativeStackDropHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class TianshuPatternStorageBlockEntity extends BlockEntity implements PatternContainer {
   private static final String TAG_PATTERNS = "ClosedLoopPatterns";
   private static final String TAG_PORT_POS = "PortPos";
   private final ClosedLoopPatternRepository patterns = new ClosedLoopPatternRepository(() -> 36);
   private final List<ClosedLoopPatternPayload> terminalPatternSlots = new ArrayList<>();
   private final InternalInventory terminalPatternInventory = new TianshuPatternStorageBlockEntity.TerminalPatternInventory();
   private final InternalPatternContainerLink terminalLink = new InternalPatternContainerLink(this, (ItemLike)ModBlocks.CLOSED_LOOP_PATTERN_STORAGE.get());
   private BlockPos portPos;

   public TianshuPatternStorageBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.TIANSHU_PATTERN_STORAGE.get(), pos, state);
   }

   public List<ClosedLoopPatternPayload> patterns() {
      return this.patterns.patterns();
   }

   public void replacePatterns(List<ClosedLoopPatternPayload> payloads) {
      this.patterns.replaceAll(payloads);
      this.m_6596_();
   }

   public void bindToPort(BlockPos newPortPos) {
      BlockPos immutable = newPortPos == null ? null : newPortPos.m_7949_();
      boolean changed = !Objects.equals(this.portPos, immutable);
      this.portPos = immutable;
      if (this.portPos == null) {
         this.terminalLink.disconnect();
      } else {
         TianshuSupercomputerPortBlockEntity port = this.resolvePort();
         if (port != null && port.isFormed()) {
            this.terminalLink.bind(port.getMainNode());
         } else {
            this.terminalLink.disconnect();
         }
      }

      if (changed) {
         this.m_6596_();
      }
   }

   public BlockPos getPortPos() {
      return this.portPos;
   }

   public IGrid getGrid() {
      return this.terminalLink.getGrid();
   }

   public boolean isVisibleInTerminal() {
      TianshuSupercomputerPortBlockEntity port = this.resolvePort();
      return port != null && port.isFormed() && this.terminalLink.isActive();
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
         AEItemKey.of((ItemLike)ModBlocks.CLOSED_LOOP_PATTERN_STORAGE.get()),
         ((TianshuPatternStorageBlock)ModBlocks.CLOSED_LOOP_PATTERN_STORAGE.get()).m_49954_(),
         List.of(Component.m_237110_("ae2lt.tianshu.terminal.tooltip", new Object[]{this.patterns.size(), this.patterns.capacity()}))
      );
   }

   public void dropStoredPatterns(Level level, BlockPos pos) {
      ClosedLoopPatternItem item = (ClosedLoopPatternItem)ModItems.CLOSED_LOOP_PATTERN.get();
      List<ClosedLoopPatternPayload> storedPatterns = List.copyOf(this.patterns.patterns());
      this.patterns.clear();
      this.m_6596_();

      for (ClosedLoopPatternPayload payload : storedPatterns) {
         NativeStackDropHelper.popResource(level, pos, item.createStack(payload, level.m_9598_()));
      }
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      CompoundTag patternTag = new CompoundTag();
      this.patterns.writeTo(patternTag);
      tag.m_128365_("ClosedLoopPatterns", patternTag);
      if (this.portPos != null) {
         tag.m_128356_("PortPos", this.portPos.m_121878_());
      }
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      this.patterns.readFrom(tag.m_128469_("ClosedLoopPatterns"));
      this.portPos = tag.m_128425_("PortPos", 4) ? BlockPos.m_122022_(tag.m_128454_("PortPos")) : null;
   }

   public void onChunkUnloaded() {
      this.terminalLink.disconnect();
      super.onChunkUnloaded();
   }

   public void m_7651_() {
      this.terminalLink.disconnect();
      super.m_7651_();
   }

   private TianshuSupercomputerPortBlockEntity resolvePort() {
      if (this.portPos != null && this.f_58857_ != null && this.f_58857_.m_46749_(this.portPos)) {
         return this.f_58857_.m_7702_(this.portPos) instanceof TianshuSupercomputerPortBlockEntity port ? port : null;
      } else {
         return null;
      }
   }

   private void notifyPatternsChanged() {
      this.m_6596_();
      TianshuSupercomputerPortBlockEntity port = this.resolvePort();
      TianshuSupercomputerControllerBlockEntity controller = port != null ? port.getController() : null;
      if (controller != null) {
         controller.patternWarehouseChanged();
      }
   }

   private final class TerminalPatternInventory extends BaseInternalInventory {
      public int size() {
         this.syncSlots();
         return TianshuPatternStorageBlockEntity.this.patterns.capacity();
      }

      public ItemStack getStackInSlot(int slotIndex) {
         ClosedLoopPatternPayload payload = this.payloadAt(slotIndex);
         if (payload != null && TianshuPatternStorageBlockEntity.this.f_58857_ != null) {
            ClosedLoopPatternItem item = (ClosedLoopPatternItem)ModItems.CLOSED_LOOP_PATTERN.get();
            return item.createStack(payload, TianshuPatternStorageBlockEntity.this.f_58857_.m_9598_());
         } else {
            return ItemStack.f_41583_;
         }
      }

      public void setItemDirect(int slotIndex, ItemStack stack) {
         if (slotIndex >= 0 && slotIndex < TianshuPatternStorageBlockEntity.this.patterns.capacity()) {
            this.syncSlots();
            ClosedLoopPatternPayload current = this.payloadAt(slotIndex);
            if (stack != null && !stack.m_41619_()) {
               ClosedLoopPatternPayload payload = this.readValidPayload(stack);
               if (payload != null) {
                  ClosedLoopPatternRepository.PutResult result = current == null
                     ? TianshuPatternStorageBlockEntity.this.patterns.add(payload)
                     : TianshuPatternStorageBlockEntity.this.patterns.replace(current, payload);
                  if (result == ClosedLoopPatternRepository.PutResult.ADDED || result == ClosedLoopPatternRepository.PutResult.UPDATED) {
                     TianshuPatternStorageBlockEntity.this.terminalPatternSlots.set(slotIndex, payload);
                     TianshuPatternStorageBlockEntity.this.notifyPatternsChanged();
                  }
               }
            } else {
               if (current != null && TianshuPatternStorageBlockEntity.this.patterns.remove(current)) {
                  TianshuPatternStorageBlockEntity.this.terminalPatternSlots.set(slotIndex, null);
                  TianshuPatternStorageBlockEntity.this.notifyPatternsChanged();
               }
            }
         }
      }

      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         if (stack != null && !stack.m_41619_() && slot >= 0 && slot < TianshuPatternStorageBlockEntity.this.patterns.capacity()) {
            this.syncSlots();
            if (this.payloadAt(slot) == null
               && TianshuPatternStorageBlockEntity.this.patterns.size() < TianshuPatternStorageBlockEntity.this.patterns.capacity()) {
               ClosedLoopPatternPayload payload = this.readValidPayload(stack);
               if (payload == null) {
                  return stack;
               } else {
                  if (!simulate) {
                     if (TianshuPatternStorageBlockEntity.this.patterns.add(payload) != ClosedLoopPatternRepository.PutResult.ADDED) {
                        return stack;
                     }

                     TianshuPatternStorageBlockEntity.this.terminalPatternSlots.set(slot, payload);
                     TianshuPatternStorageBlockEntity.this.notifyPatternsChanged();
                  }

                  return stack.m_41613_() <= 1 ? ItemStack.f_41583_ : stack.m_255036_(stack.m_41613_() - 1);
               }
            } else {
               return stack;
            }
         } else {
            return stack;
         }
      }

      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         if (amount > 0 && slot >= 0 && slot < TianshuPatternStorageBlockEntity.this.patterns.capacity()) {
            this.syncSlots();
            ClosedLoopPatternPayload payload = this.payloadAt(slot);
            if (payload == null) {
               return ItemStack.f_41583_;
            } else {
               ItemStack extracted = this.getStackInSlot(slot);
               if (!simulate && TianshuPatternStorageBlockEntity.this.patterns.remove(payload)) {
                  TianshuPatternStorageBlockEntity.this.terminalPatternSlots.set(slot, null);
                  TianshuPatternStorageBlockEntity.this.notifyPatternsChanged();
               }

               return extracted;
            }
         } else {
            return ItemStack.f_41583_;
         }
      }

      public int getSlotLimit(int slot) {
         return slot >= 0 && slot < TianshuPatternStorageBlockEntity.this.patterns.capacity() ? 1 : 0;
      }

      public boolean isItemValid(int slot, ItemStack stack) {
         return slot >= 0
            && slot < TianshuPatternStorageBlockEntity.this.patterns.capacity()
            && this.payloadAt(slot) == null
            && this.readValidPayload(stack) != null;
      }

      private ClosedLoopPatternPayload payloadAt(int slot) {
         if (slot >= 0 && slot < TianshuPatternStorageBlockEntity.this.patterns.capacity()) {
            this.syncSlots();
            ClosedLoopPatternPayload payload = TianshuPatternStorageBlockEntity.this.terminalPatternSlots.get(slot);
            return TianshuPatternStorageBlockEntity.this.patterns.indexOf(payload) >= 0 ? payload : null;
         } else {
            return null;
         }
      }

      private ClosedLoopPatternPayload readValidPayload(ItemStack stack) {
         if (TianshuPatternStorageBlockEntity.this.f_58857_ != null && stack.m_41720_() instanceof ClosedLoopPatternItem item) {
            ClosedLoopPatternPayload payload = item.readPayload(stack, TianshuPatternStorageBlockEntity.this.f_58857_).orElse(null);
            return payload != null && ClosedLoopPatternValidator.validate(payload, TianshuPatternStorageBlockEntity.this.f_58857_).valid() ? payload : null;
         } else {
            return null;
         }
      }

      private void syncSlots() {
         int capacity = TianshuPatternStorageBlockEntity.this.patterns.capacity();

         while (TianshuPatternStorageBlockEntity.this.terminalPatternSlots.size() < capacity) {
            TianshuPatternStorageBlockEntity.this.terminalPatternSlots.add(null);
         }

         while (TianshuPatternStorageBlockEntity.this.terminalPatternSlots.size() > capacity) {
            TianshuPatternStorageBlockEntity.this.terminalPatternSlots.remove(TianshuPatternStorageBlockEntity.this.terminalPatternSlots.size() - 1);
         }

         List<ClosedLoopPatternPayload> active = TianshuPatternStorageBlockEntity.this.patterns.activePatterns();

         for (int i = 0; i < TianshuPatternStorageBlockEntity.this.terminalPatternSlots.size(); i++) {
            ClosedLoopPatternPayload pattern = TianshuPatternStorageBlockEntity.this.terminalPatternSlots.get(i);
            if (pattern != null && !this.containsReference(active, pattern)) {
               TianshuPatternStorageBlockEntity.this.terminalPatternSlots.set(i, null);
            }
         }

         for (ClosedLoopPatternPayload pattern : active) {
            if (!this.containsReference(TianshuPatternStorageBlockEntity.this.terminalPatternSlots, pattern)) {
               int free = TianshuPatternStorageBlockEntity.this.terminalPatternSlots.indexOf(null);
               if (free < 0) {
                  break;
               }

               TianshuPatternStorageBlockEntity.this.terminalPatternSlots.set(free, pattern);
            }
         }
      }

      private boolean containsReference(List<ClosedLoopPatternPayload> payloads, ClosedLoopPatternPayload candidate) {
         for (ClosedLoopPatternPayload payload : payloads) {
            if (payload == candidate) {
               return true;
            }
         }

         return false;
      }
   }
}
