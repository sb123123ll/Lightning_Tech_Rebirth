package com.moakiee.ae2lt.blockentity;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.inventories.ItemTransfer;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener.State;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.util.AECableType;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.blockentity.grid.AENetworkInvBlockEntity;
import appeng.client.render.crafting.AssemblerAnimationStatus;
import appeng.core.AELog;
import appeng.crafting.CraftingEvent;
import appeng.menu.AutoCraftingMenu;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.CombinedInternalInventory;
import appeng.util.inv.FilteredInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import com.moakiee.ae2lt.block.PigmeeMolecularAssemblerBlock;
import com.moakiee.ae2lt.network.NetworkInit;
import com.moakiee.ae2lt.network.PigmeeAssemblerAnimationPacket;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

public final class PigmeeMolecularAssemblerBlockEntity extends AENetworkInvBlockEntity implements IGridTickable, ICraftingMachine, IPowerChannelState {
   public static final ResourceLocation INV_MAIN = new ResourceLocation("ae2lt", "pigmee_molecular_assembler");
   private static final String TAG_AUTOMATIC_PATTERN = "automaticPattern";
   private static final String TAG_PUSH_DIRECTION = "pushDirection";
   private static final int CRAFTING_SLOT_COUNT = 9;
   private static final int OUTPUT_SLOT = 9;
   private static final int MAX_PROGRESS = 100;
   private static final int ENERGY_PER_TICK = 10;
   private final CraftingContainer craftingGrid = new TransientCraftingContainer(new AutoCraftingMenu(), 3, 3);
   private final AppEngInternalInventory itemInventory = new AppEngInternalInventory(this, 10, 1);
   private final AppEngInternalInventory patternInventory = new AppEngInternalInventory(this, 1, 1);
   private final InternalInventory exposedInventory = new FilteredInternalInventory(
      this.itemInventory, new PigmeeMolecularAssemblerBlockEntity.CraftingGridFilter()
   );
   private final InternalInventory combinedInventory = new CombinedInternalInventory(new InternalInventory[]{this.itemInventory, this.patternInventory});
   @Nullable
   private IMolecularAssemblerSupportedPattern activePattern;
   private ItemStack savedAutomaticPattern = ItemStack.f_41583_;
   @Nullable
   private Direction pushDirection;
   private double progress;
   private boolean automaticJob;
   private boolean awake;
   private boolean powered;
   private boolean reboot = true;
   @OnlyIn(Dist.CLIENT)
   @Nullable
   private AssemblerAnimationStatus animationStatus;

   public PigmeeMolecularAssemblerBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.PIGMEE_MOLECULAR_ASSEMBLER.get(), pos, state);
      this.getMainNode().setIdlePowerUsage(0.0).addService(IGridTickable.class, this);
   }

   public PatternContainerGroup getCraftingMachineInfo() {
      Component name = this.m_8077_() ? this.m_7770_() : ((PigmeeMolecularAssemblerBlock)ModBlocks.PIGMEE_MOLECULAR_ASSEMBLER.get()).m_5456_().m_41466_();
      return new PatternContainerGroup(AEItemKey.of((ItemLike)ModBlocks.PIGMEE_MOLECULAR_ASSEMBLER.get()), name, List.of());
   }

   public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
      if (this.activePattern == null && this.combinedInventory.isEmpty() && patternDetails instanceof IMolecularAssemblerSupportedPattern supportedPattern) {
         this.automaticJob = true;
         this.activePattern = supportedPattern;
         this.pushDirection = ejectionDirection;
         supportedPattern.fillCraftingGrid(inputs, this.itemInventory::setItemDirect);

         for (KeyCounter input : inputs) {
            input.removeZeros();
            if (!input.isEmpty()) {
               this.clearAutomaticJob();
               this.itemInventory.clear();
               throw new IllegalStateException("Pigmee assembler could not place all supplied crafting inputs");
            }
         }

         this.updateSleepState();
         this.saveChanges();
         return true;
      } else {
         return false;
      }
   }

   public boolean acceptsPlans() {
      return this.patternInventory.isEmpty();
   }

   public InternalInventory getSubInventory(ResourceLocation id) {
      return INV_MAIN.equals(id) ? this.combinedInventory : super.getSubInventory(id);
   }

   public InternalInventory getInternalInventory() {
      return this.combinedInventory;
   }

   protected InternalInventory getExposedInventoryForSide(Direction side) {
      return this.exposedInventory;
   }

   public void onChangeInventory(InternalInventory inventory, int slot) {
      if (inventory == this.itemInventory || inventory == this.patternInventory) {
         this.recalculatePattern();
         this.saveChanges();
      }
   }

   public int getCraftingProgress() {
      return (int)this.progress;
   }

   @Nullable
   public IMolecularAssemblerSupportedPattern getCurrentPattern() {
      if (this.isClientSide()) {
         return PatternDetailsHelper.decodePattern(this.patternInventory.getStackInSlot(0), this.f_58857_) instanceof IMolecularAssemblerSupportedPattern supported
            ? supported
            : null;
      } else {
         return this.activePattern;
      }
   }

   public TickingRequest getTickingRequest(IGridNode node) {
      this.recalculatePattern();
      this.updateSleepState();
      return new TickingRequest(1, 1, !this.awake, true);
   }

   public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
      if (!this.itemInventory.getStackInSlot(9).m_41619_()) {
         this.pushOutput(this.itemInventory.getStackInSlot(9));
         this.ejectInvalidInput();
         this.progress = 0.0;
         this.updateSleepState();
         return this.awake ? TickRateModulation.IDLE : TickRateModulation.SLEEP;
      } else if (this.activePattern != null && this.awake) {
         if (this.reboot) {
            ticksSinceLastCall = 1;
            this.reboot = false;
         }

         this.progress = this.progress + (double)this.usePower(ticksSinceLastCall);
         if (this.progress < 100.0) {
            return TickRateModulation.FASTER;
         } else {
            this.populateTransientCraftingGrid();
            ItemStack output = this.activePattern.assemble(this.craftingGrid, this.f_58857_);
            this.progress = 0.0;
            if (output.m_41619_()) {
               this.updateSleepState();
               return TickRateModulation.IDLE;
            } else {
               CraftingEvent.fireAutoCraftingEvent(this.f_58857_, this.activePattern, output, this.craftingGrid);
               NonNullList<ItemStack> remainders = this.activePattern.getRemainingItems(this.craftingGrid);
               this.pushOutput(output.m_41777_());

               for (int y = 0; y < this.craftingGrid.m_39346_(); y++) {
                  for (int x = 0; x < this.craftingGrid.m_39347_(); x++) {
                     int inventorySlot = x + y * this.craftingGrid.m_39347_();
                     this.itemInventory.setItemDirect(inventorySlot, (ItemStack)remainders.get(inventorySlot));
                  }
               }

               if (this.patternInventory.isEmpty()) {
                  this.clearAutomaticJob();
               }

               this.ejectInvalidInput();
               this.sendCraftingAnimation(node, output);
               this.saveChanges();
               this.updateSleepState();
               return this.awake ? TickRateModulation.IDLE : TickRateModulation.SLEEP;
            }
         }
      } else {
         this.updateSleepState();
         return TickRateModulation.SLEEP;
      }
   }

   private int usePower(int ticksSinceLastCall) {
      IGrid grid = this.getMainNode().getGrid();
      return grid == null ? 0 : (int)grid.getEnergyService().extractAEPower((double)(ticksSinceLastCall * 10), Actionable.MODULATE, PowerMultiplier.CONFIG);
   }

   private void populateTransientCraftingGrid() {
      for (int slot = 0; slot < 9; slot++) {
         this.craftingGrid.m_6836_(slot, this.itemInventory.getStackInSlot(slot));
      }
   }

   private boolean hasMaterials() {
      if (this.activePattern == null) {
         return false;
      } else {
         this.populateTransientCraftingGrid();
         return !this.activePattern.assemble(this.craftingGrid, this.f_58857_).m_41619_();
      }
   }

   private void pushOutput(ItemStack output) {
      if (this.pushDirection == null) {
         for (Direction direction : Direction.values()) {
            output = this.pushTo(output, direction);
         }
      } else {
         output = this.pushTo(output, this.pushDirection);
      }

      this.itemInventory.setItemDirect(9, output);
   }

   private ItemStack pushTo(ItemStack output, Direction direction) {
      if (!output.m_41619_() && this.f_58857_ != null) {
         ItemTransfer target = InternalInventory.wrapExternal(this.f_58857_, this.f_58858_.m_121945_(direction), direction.m_122424_());
         if (target == null) {
            return output;
         } else {
            ItemStack remainder = target.addItems(output);
            if (remainder.m_41613_() != output.m_41613_()) {
               this.saveChanges();
            }

            return remainder;
         }
      } else {
         return output;
      }
   }

   private void ejectInvalidInput() {
      if (this.itemInventory.getStackInSlot(9).m_41619_()) {
         for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = this.itemInventory.getStackInSlot(slot);
            if (!stack.m_41619_() && (this.activePattern == null || !this.activePattern.isItemValid(slot, AEItemKey.of(stack), this.f_58857_))) {
               this.itemInventory.setItemDirect(9, stack);
               this.itemInventory.setItemDirect(slot, ItemStack.f_41583_);
               this.saveChanges();
               return;
            }
         }
      }
   }

   private void recalculatePattern() {
      this.reboot = true;
      if (this.automaticJob) {
         this.restoreAutomaticPatternIfNeeded();
         this.updateSleepState();
      } else {
         ItemStack patternStack = this.patternInventory.getStackInSlot(0);
         if (!patternStack.m_41619_() && ItemStack.m_150942_(patternStack, this.savedAutomaticPattern)) {
            this.updateSleepState();
         } else {
            this.progress = 0.0;
            this.activePattern = null;
            this.savedAutomaticPattern = ItemStack.f_41583_;
            this.pushDirection = null;
            if (PatternDetailsHelper.decodePattern(patternStack, this.f_58857_) instanceof IMolecularAssemblerSupportedPattern supportedPattern) {
               this.activePattern = supportedPattern;
               this.savedAutomaticPattern = patternStack.m_41777_();
            }

            this.updateSleepState();
         }
      }
   }

   private void restoreAutomaticPatternIfNeeded() {
      if (this.activePattern == null && !this.savedAutomaticPattern.m_41619_() && this.f_58857_ != null) {
         if (PatternDetailsHelper.decodePattern(this.savedAutomaticPattern, this.f_58857_) instanceof IMolecularAssemblerSupportedPattern supportedPattern) {
            this.activePattern = supportedPattern;
         } else {
            AELog.warn("Unable to restore Pigmee assembler automatic crafting pattern", new Object[0]);
            this.clearAutomaticJob();
         }

         this.savedAutomaticPattern = ItemStack.f_41583_;
      }
   }

   private void clearAutomaticJob() {
      this.automaticJob = false;
      this.activePattern = null;
      this.savedAutomaticPattern = ItemStack.f_41583_;
      this.pushDirection = null;
   }

   private void updateSleepState() {
      boolean wasAwake = this.awake;
      this.awake = this.activePattern != null && this.hasMaterials() || !this.itemInventory.getStackInSlot(9).m_41619_();
      if (wasAwake != this.awake) {
         this.getMainNode().ifPresent((grid, node) -> {
            if (this.awake) {
               grid.getTickManager().wakeDevice(node);
            } else {
               grid.getTickManager().sleepDevice(node);
            }
         });
      }
   }

   private void sendCraftingAnimation(IGridNode node, ItemStack output) {
      ServerLevel serverLevel = node.getLevel();
      if (serverLevel != null) {
         NetworkInit.sendToPlayersNear(
            serverLevel,
            (double)this.f_58858_.m_123341_(),
            (double)this.f_58858_.m_123342_(),
            (double)this.f_58858_.m_123343_(),
            32.0,
            new PigmeeAssemblerAnimationPacket(this.f_58858_, (byte)10, output.m_41777_())
         );
      }
   }

   public AECableType getCableConnectionType(Direction direction) {
      return AECableType.COVERED;
   }

   public void onMainNodeStateChanged(State reason) {
      if (reason != State.GRID_BOOT) {
         boolean newPowered = false;
         IGrid grid = this.getMainNode().getGrid();
         if (grid != null) {
            newPowered = this.getMainNode().isPowered() && grid.getEnergyService().extractAEPower(1.0, Actionable.SIMULATE, PowerMultiplier.CONFIG) > 1.0E-4;
         }

         if (newPowered != this.powered) {
            this.powered = newPowered;
            this.markForUpdate();
         }
      }
   }

   public boolean isPowered() {
      return this.powered;
   }

   public boolean isActive() {
      return this.powered;
   }

   protected boolean readFromStream(FriendlyByteBuf data) {
      boolean changed = super.readFromStream(data);
      boolean newPowered = data.readBoolean();
      if (newPowered != this.powered) {
         this.powered = newPowered;
         changed = true;
      }

      return changed;
   }

   protected void writeToStream(FriendlyByteBuf data) {
      super.writeToStream(data);
      data.writeBoolean(this.powered);
   }

   public void m_183515_(CompoundTag data) {
      super.m_183515_(data);
      if (this.automaticJob) {
         ItemStack patternStack = this.activePattern != null ? this.activePattern.getDefinition().toStack() : this.savedAutomaticPattern;
         if (!patternStack.m_41619_()) {
            data.m_128365_("automaticPattern", patternStack.m_41739_(new CompoundTag()));
            if (this.pushDirection != null) {
               data.m_128405_("pushDirection", this.pushDirection.m_122411_());
            }
         }
      }
   }

   public void loadTag(CompoundTag data) {
      super.loadTag(data);
      this.clearAutomaticJob();
      if (data.m_128441_("automaticPattern")) {
         ItemStack patternStack = ItemStack.m_41712_(data.m_128469_("automaticPattern"));
         if (!patternStack.m_41619_()) {
            this.automaticJob = true;
            this.savedAutomaticPattern = patternStack;
            if (data.m_128441_("pushDirection")) {
               this.pushDirection = Direction.m_122376_(data.m_128451_("pushDirection"));
            }
         }
      }

      this.recalculatePattern();
   }

   @OnlyIn(Dist.CLIENT)
   @Nullable
   public AssemblerAnimationStatus getAnimationStatus() {
      return this.animationStatus;
   }

   @OnlyIn(Dist.CLIENT)
   public void setAnimationStatus(@Nullable AssemblerAnimationStatus animationStatus) {
      this.animationStatus = animationStatus;
   }

   protected Item getItemFromBlockEntity() {
      return ((PigmeeMolecularAssemblerBlock)ModBlocks.PIGMEE_MOLECULAR_ASSEMBLER.get()).m_5456_();
   }

   public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
      return cap == ForgeCapabilities.ITEM_HANDLER ? LazyOptional.of(() -> this.exposedInventory.toItemHandler()).cast() : super.getCapability(cap, side);
   }

   private final class CraftingGridFilter implements IAEItemFilter {
      public boolean allowExtract(InternalInventory inventory, int slot, int amount) {
         return slot == 9;
      }

      public boolean allowInsert(InternalInventory inventory, int slot, ItemStack stack) {
         return slot < 9
            && !PigmeeMolecularAssemblerBlockEntity.this.patternInventory.isEmpty()
            && PigmeeMolecularAssemblerBlockEntity.this.activePattern != null
            && PigmeeMolecularAssemblerBlockEntity.this.activePattern.isItemValid(slot, AEItemKey.of(stack), PigmeeMolecularAssemblerBlockEntity.this.f_58857_);
      }
   }
}
