package com.moakiee.ae2lt.blockentity;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.inventories.BaseInternalInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.orientation.BlockOrientation;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.me.helpers.MachineSource;
import com.moakiee.ae2lt.block.MatrixPortBlock;
import com.moakiee.ae2lt.logic.craft.MatrixCraftingMath;
import com.moakiee.ae2lt.logic.craft.MatrixCraftingProfile;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.ae2lt.util.NativeStackDropHelper;
import com.moakiee.thunderbolt.api.crafting.batch.BatchDispatchMode;
import com.moakiee.thunderbolt.api.crafting.batch.IBatchCraftingProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandlerModifiable;

public class MatrixPortBlockEntity extends AENetworkBlockEntity implements IBatchCraftingProvider {
   private static final String TAG_CONTROLLER_POS = "ControllerPos";
   private static final String TAG_FORMED = "Formed";
   private static final String TAG_CLUSTER = "Cluster";
   private static final int BINDING_CHECK_INTERVAL_TICKS = 20;
   private final IActionSource actionSource = new MachineSource(this.getMainNode()::getNode);
   private final MatrixPortBlockEntity.PortPatternItemHandler itemHandler = new MatrixPortBlockEntity.PortPatternItemHandler();
   private final MatrixPortBlockEntity.MatrixTerminalPatternInventory terminalPatternInventory = new MatrixPortBlockEntity.MatrixTerminalPatternInventory();
   private BlockPos controllerPos;
   private UUID boundMachineId;
   private CompoundTag legacyClusterState;
   private boolean formed;
   private boolean patternUpdatePending;
   private List<MatrixPatternStorageBlockEntity> exposedPatternStorages = List.of();
   private boolean exposedPatternStorageDirty = true;
   private List<MatrixPortBlockEntity.TerminalPatternSlot> terminalPatternSlots = List.of();
   private boolean terminalPatternSlotsDirty = true;
   private long nextBindingCheckTick;

   public MatrixPortBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.MATRIX_PORT.get(), pos, state);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, MatrixPortBlockEntity port) {
      if (!level.f_46443_) {
         port.flushPatternUpdate();
         if (level.m_46467_() >= port.nextBindingCheckTick) {
            port.nextBindingCheckTick = level.m_46467_() + 20L;
            port.validateControllerBinding();
         }
      }
   }

   protected IManagedGridNode createMainNode() {
      return super.createMainNode()
         .setTagName("matter_warping_matrix_port")
         .setVisualRepresentation((ItemLike)ModBlocks.MATTER_WARPING_MATRIX_PORT.get())
         .setIdlePowerUsage(8.0)
         .setFlags(new GridFlags[]{GridFlags.REQUIRE_CHANNEL})
         .addService(ICraftingProvider.class, this);
   }

   public AECableType getCableConnectionType(Direction dir) {
      return this.formed ? AECableType.DENSE_SMART : AECableType.NONE;
   }

   public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
      return (Set<Direction>)(this.formed ? EnumSet.allOf(Direction.class) : Collections.emptySet());
   }

   public IItemHandlerModifiable getPatternItemHandler() {
      return this.itemHandler;
   }

   public BlockPos getControllerPos() {
      return this.controllerPos;
   }

   public void bindToController(BlockPos controllerPos) {
      if (controllerPos != null) {
         throw new IllegalArgumentException("A matrix link requires its controller UUID");
      } else {
         boolean bindingChanged = this.formed || this.controllerPos != null || this.boundMachineId != null;
         boolean formedChanged = this.formed;
         this.controllerPos = null;
         this.boundMachineId = null;
         this.formed = false;
         if (formedChanged) {
            this.onGridConnectableSidesChanged();
         }

         this.updateLinkState(bindingChanged);
      }
   }

   public void bindToController(BlockPos controllerPos, UUID machineId) {
      if (controllerPos != null && machineId != null) {
         boolean bindingChanged = !this.formed || !controllerPos.equals(this.controllerPos) || !machineId.equals(this.boundMachineId);
         boolean formedChanged = !this.formed;
         this.controllerPos = controllerPos.m_7949_();
         this.boundMachineId = machineId;
         this.formed = true;
         if (formedChanged) {
            this.onGridConnectableSidesChanged();
         }

         this.updateLinkState(bindingChanged);
      } else {
         this.bindToController(null);
      }
   }

   public void suspendFromController(BlockPos expectedControllerPos) {
      if (this.formed && expectedControllerPos != null && expectedControllerPos.equals(this.controllerPos)) {
         this.formed = false;
         this.onGridConnectableSidesChanged();
         this.updateLinkState(true);
      }
   }

   private void updateLinkState(boolean bindingChanged) {
      boolean blockStateChanged = false;
      if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
         BlockState state = this.m_58900_();
         if (state.m_61138_(MatrixPortBlock.FORMED) && (Boolean)state.m_61143_(MatrixPortBlock.FORMED) != this.formed) {
            this.f_58857_.m_7731_(this.f_58858_, (BlockState)state.m_61124_(MatrixPortBlock.FORMED, this.formed), 3);
            blockStateChanged = true;
         }
      }

      this.invalidateExposedPatternStorage();
      this.invalidateTerminalPatternSlots();
      if ((bindingChanged || blockStateChanged) && this.f_58857_ != null && !this.f_58857_.f_46443_) {
         this.f_58857_.m_46672_(this.f_58858_, this.m_58900_().m_60734_());
      }

      this.saveChanges();
      this.markForUpdate();
      this.requestCraftingUpdate();
   }

   public boolean isFormed() {
      MatrixControllerBlockEntity controller = this.getController();
      return this.formed && this.boundMachineId != null && controller != null;
   }

   public boolean isLinkedTo(BlockPos controllerPos, UUID machineId) {
      return this.formed && controllerPos != null && machineId != null && controllerPos.equals(this.controllerPos) && machineId.equals(this.boundMachineId);
   }

   public MatrixControllerBlockEntity getController() {
      if (this.formed && this.controllerPos != null && this.f_58857_ != null && this.f_58857_.m_46749_(this.controllerPos)) {
         if (this.f_58857_.m_7702_(this.controllerPos) instanceof MatrixControllerBlockEntity controller
            && controller.isPersistentStateOwner()
            && this.boundMachineId != null
            && this.boundMachineId.equals(controller.getMachineId())
            && controller.isPortActive(this.f_58858_)) {
            return controller;
         }

         return null;
      } else {
         return null;
      }
   }

   public List<MatrixPatternStorageBlockEntity> getPatternStorages() {
      MatrixControllerBlockEntity controller = this.getController();
      return controller != null ? controller.findPatternStorages() : List.of();
   }

   public MatrixCraftingProfile getCraftingProfile() {
      MatrixControllerBlockEntity controller = this.getController();
      return controller != null ? controller.getCraftingProfile() : MatrixCraftingProfile.empty();
   }

   public MatrixCraftingMath.Snapshot getLimiterSnapshot() {
      MatrixControllerBlockEntity controller = this.getController();
      return controller != null ? controller.getLimiterSnapshot() : MatrixCraftingMath.idleSnapshot(0.0, 0.0);
   }

   public boolean isWorking() {
      MatrixControllerBlockEntity controller = this.getController();
      return controller != null && controller.isWorking();
   }

   public void patternsChanged() {
      this.invalidateExposedPatternStorage();
      this.patternUpdatePending = true;
   }

   public IGrid getGrid() {
      return this.isFormed() ? this.getMainNode().getGrid() : null;
   }

   public InternalInventory getTerminalPatternInventory() {
      return this.terminalPatternInventory;
   }

   public List<IPatternDetails> getAvailablePatterns() {
      MatrixControllerBlockEntity controller = this.getController();
      return controller != null ? controller.getAvailablePatterns() : List.of();
   }

   public boolean isBusy() {
      MatrixControllerBlockEntity controller = this.getController();
      return controller == null || controller.isMatrixBusy();
   }

   public long getBatchCapacity(IPatternDetails details) {
      MatrixControllerBlockEntity controller = this.getController();
      return controller != null ? controller.getBatchCapacity(details) : 0L;
   }

   public boolean supportsSharedBatchInputs() {
      return this.isFormed();
   }

   public BatchDispatchMode getBatchDispatchMode(IPatternDetails details) {
      MatrixControllerBlockEntity controller = this.getController();
      return controller != null ? controller.getBatchDispatchMode() : BatchDispatchMode.NORMAL;
   }

   public long pushBatch(IPatternDetails details, KeyCounter[] oneCopyTemplate, long maxCraft) {
      MatrixControllerBlockEntity controller = this.getController();
      return controller != null ? controller.pushBatch(details, oneCopyTemplate, maxCraft) : maxCraft;
   }

   public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
      MatrixControllerBlockEntity controller = this.getController();
      return controller != null && controller.pushPattern(patternDetails, inputHolder);
   }

   public boolean isLinkConnected() {
      return this.isFormed() && this.getMainNode().isActive() && this.getMainNode().getGrid() != null;
   }

   public long insertToNetworkLink(AEKey key, long amount) {
      IGrid grid = this.getMainNode().getGrid();
      return grid != null && key != null && amount > 0L
         ? grid.getStorageService().getInventory().insert(key, amount, Actionable.MODULATE, this.actionSource)
         : 0L;
   }

   public boolean isConnected() {
      return this.isLinkConnected();
   }

   public long insertToNetwork(AEKey key, long amount) {
      return this.insertToNetworkLink(key, amount);
   }

   public void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      if (this.controllerPos != null) {
         tag.m_128356_("ControllerPos", this.controllerPos.m_121878_());
      }

      tag.m_128379_("Formed", this.formed);
      if (this.legacyClusterState != null) {
         tag.m_128365_("Cluster", this.legacyClusterState.m_6426_());
      }
   }

   public void loadTag(CompoundTag tag) {
      super.loadTag(tag);
      this.controllerPos = tag.m_128425_("ControllerPos", 4) ? BlockPos.m_122022_(tag.m_128454_("ControllerPos")) : null;
      this.formed = false;
      this.boundMachineId = null;
      this.legacyClusterState = null;
      if (tag.m_128425_("Cluster", 10)) {
         this.legacyClusterState = tag.m_128469_("Cluster").m_6426_();
      }

      this.invalidateExposedPatternStorage();
   }

   public void spawnToWorld(AEKey key, long amount) {
      Level level = this.m_58904_();
      if (level != null && !level.f_46443_ && key != null && amount > 0L) {
         ArrayList<ItemStack> drops = new ArrayList<>();
         key.addDrops(amount, drops, level, this.m_58899_());

         for (ItemStack drop : drops) {
            if (!drop.m_41619_()) {
               NativeStackDropHelper.popResource(level, this.m_58899_(), drop);
            }
         }
      }
   }

   public void onLoad() {
      super.onLoad();
      this.nextBindingCheckTick = this.f_58857_ != null ? this.f_58857_.m_46467_() : 0L;
   }

   public void onReady() {
      super.onReady();
      MatrixControllerBlockEntity controller = this.getController();
      if (controller != null) {
         controller.scheduleStructureCheck();
      }
   }

   public CompoundTag copyLegacyClusterState() {
      return this.legacyClusterState != null ? this.legacyClusterState.m_6426_() : null;
   }

   public void consumeLegacyClusterState() {
      this.legacyClusterState = null;
      this.saveChanges();
   }

   protected Item getItemFromBlockEntity() {
      return ((MatrixPortBlock)ModBlocks.MATTER_WARPING_MATRIX_PORT.get()).m_5456_();
   }

   private void validateControllerBinding() {
      if (this.f_58857_ != null && !this.f_58857_.f_46443_ && this.controllerPos != null) {
         if (!this.f_58857_.m_46749_(this.controllerPos)) {
            this.suspendFromController(this.controllerPos);
         } else {
            if (this.f_58857_.m_7702_(this.controllerPos) instanceof MatrixControllerBlockEntity controller) {
               if (controller.isPortActive(this.f_58858_)) {
                  if (!this.formed) {
                     controller.scheduleStructureCheck();
                  }
               } else if (controller.ownsPort(this.f_58858_)) {
                  this.suspendFromController(this.controllerPos);
                  controller.scheduleStructureCheck();
               } else {
                  this.bindToController(null);
               }
            } else if (!this.f_58857_.m_8055_(this.controllerPos).m_60713_((Block)ModBlocks.MATTER_WARPING_MATRIX_CONTROLLER.get())) {
               this.bindToController(null);
            }
         }
      }
   }

   private void invalidateExposedPatternStorage() {
      this.exposedPatternStorageDirty = true;
   }

   private void invalidateTerminalPatternSlots() {
      this.terminalPatternSlotsDirty = true;
   }

   private List<MatrixPortBlockEntity.TerminalPatternSlot> getTerminalPatternSlots() {
      List<MatrixPatternStorageBlockEntity> storages = this.getPatternStorages();
      if (this.terminalPatternSlotsDirty) {
         ArrayList<MatrixPortBlockEntity.TerminalPatternSlot> slots = new ArrayList<>();

         for (MatrixPatternStorageBlockEntity storage : storages) {
            for (int slot = 0; slot < storage.capacity(); slot++) {
               slots.add(new MatrixPortBlockEntity.TerminalPatternSlot(storage, slot));
            }
         }

         this.terminalPatternSlots = List.copyOf(slots);
         this.terminalPatternSlotsDirty = false;
      }

      return this.terminalPatternSlots;
   }

   private List<MatrixPatternStorageBlockEntity> getExposedPatternStorages() {
      if (this.exposedPatternStorageDirty) {
         this.exposedPatternStorages = this.selectExposedPatternStorages();
         this.exposedPatternStorageDirty = false;
      }

      return this.exposedPatternStorages;
   }

   private List<MatrixPatternStorageBlockEntity> selectExposedPatternStorages() {
      List<MatrixPatternStorageBlockEntity> storages = this.getPatternStorages();
      MatrixPatternStorageBlockEntity readable = storages.stream().filter(storage -> !storage.isEmpty()).findFirst().orElse(null);
      MatrixPatternStorageBlockEntity writable = storages.stream().filter(storage -> storage != readable && storage.hasFreeSlot()).findFirst().orElse(null);
      if (readable == null) {
         return writable != null ? List.of(writable) : List.of();
      } else if (writable == null && readable.hasFreeSlot()) {
         return List.of(readable);
      } else {
         return writable != null ? List.of(readable, writable) : List.of(readable);
      }
   }

   private void requestCraftingUpdate() {
      if (this.getMainNode().isReady()) {
         ICraftingProvider.requestUpdate(this.getMainNode());
         this.patternUpdatePending = false;
      }
   }

   private void flushPatternUpdate() {
      if (this.patternUpdatePending) {
         this.requestCraftingUpdate();
      }
   }

   private MatrixPortBlockEntity.TerminalPatternSlot terminalPatternSlot(int slot) {
      List<MatrixPortBlockEntity.TerminalPatternSlot> slots = this.getTerminalPatternSlots();
      return slot >= 0 && slot < slots.size() ? slots.get(slot) : null;
   }

   private final class MatrixTerminalPatternInventory extends BaseInternalInventory {
      public int size() {
         return MatrixPortBlockEntity.this.getTerminalPatternSlots().size();
      }

      public ItemStack getStackInSlot(int slotIndex) {
         MatrixPortBlockEntity.TerminalPatternSlot slot = MatrixPortBlockEntity.this.terminalPatternSlot(slotIndex);
         return slot == null ? ItemStack.f_41583_ : slot.storage().getInventory().getStackInSlot(slot.slot());
      }

      public void setItemDirect(int slotIndex, ItemStack stack) {
         MatrixPortBlockEntity.TerminalPatternSlot slot = MatrixPortBlockEntity.this.terminalPatternSlot(slotIndex);
         if (slot != null) {
            if (stack == null || stack.m_41619_() || slot.storage().isValidPatternStack(stack)) {
               slot.storage().getInventory().setStackInSlot(slot.slot(), stack == null ? ItemStack.f_41583_ : stack);
            }
         }
      }

      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         MatrixPortBlockEntity.TerminalPatternSlot target = MatrixPortBlockEntity.this.terminalPatternSlot(slot);
         return target == null ? stack : target.storage().getInventory().insertItem(target.slot(), stack, simulate);
      }

      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         MatrixPortBlockEntity.TerminalPatternSlot target = MatrixPortBlockEntity.this.terminalPatternSlot(slot);
         return target == null ? ItemStack.f_41583_ : target.storage().getInventory().extractItem(target.slot(), amount, simulate);
      }

      public int getSlotLimit(int slot) {
         return MatrixPortBlockEntity.this.terminalPatternSlot(slot) == null ? 0 : 1;
      }

      public boolean isItemValid(int slot, ItemStack stack) {
         MatrixPortBlockEntity.TerminalPatternSlot target = MatrixPortBlockEntity.this.terminalPatternSlot(slot);
         return target != null && target.storage().isValidPatternStack(stack);
      }
   }

   private final class PortPatternItemHandler implements IItemHandlerModifiable {
      public int getSlots() {
         int slots = 0;

         for (MatrixPatternStorageBlockEntity storage : MatrixPortBlockEntity.this.getExposedPatternStorages()) {
            slots += storage.capacity();
         }

         return slots;
      }

      public ItemStack getStackInSlot(int slot) {
         MatrixPortBlockEntity.TerminalPatternSlot target = this.exposedPatternSlot(slot);
         return target == null ? ItemStack.f_41583_ : target.storage().getInventory().getStackInSlot(target.slot());
      }

      public void setStackInSlot(int slot, ItemStack stack) {
         MatrixPortBlockEntity.TerminalPatternSlot target = this.exposedPatternSlot(slot);
         if (target != null) {
            target.storage().getInventory().setStackInSlot(target.slot(), stack);
         }
      }

      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         if (stack.m_41619_()) {
            return ItemStack.f_41583_;
         } else {
            MatrixPortBlockEntity.TerminalPatternSlot target = this.exposedPatternSlot(slot);
            if (target != null && target.storage().isValidPatternStack(stack)) {
               ItemStack remainder = stack.m_41777_();

               for (MatrixPatternStorageBlockEntity storage : MatrixPortBlockEntity.this.getPatternStorages()) {
                  for (int i = 0; i < storage.capacity(); i++) {
                     remainder = storage.getInventory().insertItem(i, remainder, simulate);
                     if (remainder.m_41619_()) {
                        return ItemStack.f_41583_;
                     }
                  }
               }

               return remainder;
            } else {
               return stack;
            }
         }
      }

      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         MatrixPortBlockEntity.TerminalPatternSlot target = this.exposedPatternSlot(slot);
         return target == null ? ItemStack.f_41583_ : target.storage().getInventory().extractItem(target.slot(), amount, simulate);
      }

      public int getSlotLimit(int slot) {
         return this.exposedPatternSlot(slot) == null ? 0 : 1;
      }

      public boolean isItemValid(int slot, ItemStack stack) {
         MatrixPortBlockEntity.TerminalPatternSlot target = this.exposedPatternSlot(slot);
         return target != null && target.storage().isValidPatternStack(stack);
      }

      private MatrixPortBlockEntity.TerminalPatternSlot exposedPatternSlot(int slot) {
         if (slot < 0) {
            return null;
         } else {
            for (MatrixPatternStorageBlockEntity storage : MatrixPortBlockEntity.this.getExposedPatternStorages()) {
               if (slot < storage.capacity()) {
                  return new MatrixPortBlockEntity.TerminalPatternSlot(storage, slot);
               }

               slot -= storage.capacity();
            }

            return null;
         }
      }
   }

   private static record TerminalPatternSlot(MatrixPatternStorageBlockEntity storage, int slot) {
   }
}
