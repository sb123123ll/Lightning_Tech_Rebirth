package com.moakiee.ae2lt.blockentity;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.orientation.BlockOrientation;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.util.AECableType;
import appeng.block.crafting.PushDirection;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.helpers.InterfaceLogicHost;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.helpers.patternprovider.PatternProviderTarget;
import appeng.me.helpers.MachineSource;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import com.moakiee.ae2lt.block.PigmeePatternProviderBlock;
import com.moakiee.ae2lt.logic.PigmeePatternProviderReturnInventory;
import com.moakiee.ae2lt.menu.PigmeePatternProviderMenu;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import it.unimi.dsi.fastutil.objects.Object2LongMap.Entry;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class PigmeePatternProviderBlockEntity extends AENetworkBlockEntity implements InternalInventoryHost, ICraftingProvider, PatternContainer {
   public static final int PATTERN_SLOT_COUNT = 3;
   private static final String TAG_PATTERNS = "Patterns";
   private static final String TAG_RETURN_INVENTORY = "ReturnInventory";
   private static final String TAG_PENDING_DISPATCH = "PendingDispatch";
   private static final String TAG_PENDING_DIRECTION = "PendingDirection";
   private final AppEngInternalInventory patternInventory = new AppEngInternalInventory(this, 3, 1) {
      public boolean isItemValid(int slot, ItemStack stack) {
         return PatternDetailsHelper.isEncodedPattern(stack);
      }
   };
   private final PigmeePatternProviderReturnInventory returnInventory = new PigmeePatternProviderReturnInventory(this::onReturnInventoryChanged);
   private final List<IPatternDetails> patterns = new ArrayList<>(3);
   private final List<GenericStack> pendingDispatch = new ArrayList<>();
   @Nullable
   private Direction pendingDirection;
   private final IActionSource actionSource = new MachineSource(this.getMainNode()::getNode);

   public PigmeePatternProviderBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.PIGMEE_PATTERN_PROVIDER.get(), pos, state);
   }

   protected IManagedGridNode createMainNode() {
      return super.createMainNode()
         .setTagName("pigmee_pattern_provider")
         .setVisualRepresentation((ItemLike)ModBlocks.PIGMEE_PATTERN_PROVIDER.get())
         .setIdlePowerUsage(1.0)
         .setFlags(new GridFlags[]{GridFlags.REQUIRE_CHANNEL})
         .addService(ICraftingProvider.class, this);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, PigmeePatternProviderBlockEntity blockEntity) {
      if (!level.m_5776_() && blockEntity.getMainNode().isActive()) {
         blockEntity.flushPendingDispatch();
         blockEntity.returnItemsToNetwork();
      }
   }

   public InternalInventory getPatternInventory() {
      return this.patternInventory;
   }

   public IGrid getGrid() {
      return this.getMainNode().getGrid();
   }

   public InternalInventory getTerminalPatternInventory() {
      return this.patternInventory;
   }

   public long getTerminalSortOrder() {
      return (long)this.f_58858_.m_123343_() << 24 ^ (long)this.f_58858_.m_123341_() << 8 ^ (long)this.f_58858_.m_123342_();
   }

   public PatternContainerGroup getTerminalGroup() {
      return new PatternContainerGroup(AEItemKey.of((ItemLike)ModBlocks.PIGMEE_PATTERN_PROVIDER.get()), this.m_5446_(), List.of());
   }

   public PigmeePatternProviderReturnInventory getReturnInventory() {
      return this.returnInventory;
   }

   public List<IPatternDetails> getAvailablePatterns() {
      return this.patterns;
   }

   public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
      if (this.getMainNode().isActive() && !this.isBusy() && this.patterns.contains(patternDetails) && this.f_58857_ != null) {
         ArrayList<Direction> inventoryTargets = new ArrayList<>();

         for (Direction direction : this.getActiveTargetDirections()) {
            BlockPos targetPos = this.f_58858_.m_121945_(direction);
            Direction targetSide = direction.m_122424_();
            ICraftingMachine craftingMachine = ICraftingMachine.of(this.f_58857_, targetPos, targetSide, this.f_58857_.m_7702_(targetPos));
            if (craftingMachine != null && craftingMachine.acceptsPlans()) {
               if (craftingMachine.pushPattern(patternDetails, inputHolder, targetSide)) {
                  return true;
               }
            } else {
               inventoryTargets.add(direction);
            }
         }

         if (!patternDetails.supportsPushInputsToExternalInventory()) {
            return false;
         } else {
            for (Direction directionx : inventoryTargets) {
               PatternProviderTarget target = this.getTarget(directionx);
               if (target != null && acceptsEveryInput(target, inputHolder)) {
                  this.pendingDirection = directionx;
                  patternDetails.pushInputsToExternalInventory(inputHolder, (what, amount) -> {
                     long inserted = target.insert(what, amount, Actionable.MODULATE);
                     if (inserted < amount) {
                        this.pendingDispatch.add(new GenericStack(what, amount - inserted));
                     }
                  });
                  if (this.pendingDispatch.isEmpty()) {
                     this.pendingDirection = null;
                  } else {
                     this.saveChanges();
                  }

                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public boolean isBusy() {
      return !this.pendingDispatch.isEmpty();
   }

   private static boolean acceptsEveryInput(PatternProviderTarget target, KeyCounter[] inputHolder) {
      KeyCounter[] var2 = inputHolder;
      int var3 = inputHolder.length;

      for (int var4 = 0; var4 < var3; var4++) {
         for (Entry<AEKey> stack : var2[var4]) {
            if (target.insert((AEKey)stack.getKey(), stack.getLongValue(), Actionable.SIMULATE) <= 0L) {
               return false;
            }
         }
      }

      return true;
   }

   private PatternProviderTarget getTarget(Direction direction) {
      if (this.f_58857_ == null) {
         return null;
      } else {
         BlockPos targetPos = this.f_58858_.m_121945_(direction);
         BlockEntity targetBlockEntity = this.f_58857_.m_7702_(targetPos);
         return PatternProviderTarget.get(this.f_58857_, targetPos, targetBlockEntity, direction.m_122424_(), this.actionSource);
      }
   }

   private void flushPendingDispatch() {
      if (!this.pendingDispatch.isEmpty()) {
         PatternProviderTarget target = this.pendingDirection == null ? null : this.getTarget(this.pendingDirection);
         if (target != null) {
            boolean changed = false;
            ListIterator<GenericStack> iterator = this.pendingDispatch.listIterator();

            while (iterator.hasNext()) {
               GenericStack stack = iterator.next();
               long inserted = target.insert(stack.what(), stack.amount(), Actionable.MODULATE);
               if (inserted >= stack.amount()) {
                  iterator.remove();
                  changed = true;
               } else if (inserted > 0L) {
                  iterator.set(new GenericStack(stack.what(), stack.amount() - inserted));
                  changed = true;
               }
            }

            if (changed) {
               if (this.pendingDispatch.isEmpty()) {
                  this.pendingDirection = null;
               }

               this.saveChanges();
            }
         }
      }
   }

   private void returnItemsToNetwork() {
      IGrid grid = this.getMainNode().getGrid();
      if (grid != null) {
         this.returnInventory.drainInto(grid.getStorageService().getInventory(), this.actionSource);
      }
   }

   private EnumSet<Direction> getTargetDirections() {
      Direction direction = ((PushDirection)this.m_58900_().m_61143_(PigmeePatternProviderBlock.PUSH_DIRECTION)).getDirection();
      return direction == null ? EnumSet.allOf(Direction.class) : EnumSet.of(direction);
   }

   private EnumSet<Direction> getActiveTargetDirections() {
      EnumSet<Direction> directions = this.getTargetDirections();
      IGridNode node = this.getMainNode().getNode();
      if (node == null) {
         return directions;
      } else {
         for (java.util.Map.Entry<Direction, IGridConnection> entry : node.getInWorldConnections().entrySet()) {
            IGridNode otherNode = entry.getValue().getOtherSide(node);
            Object owner = otherNode.getOwner();
            if (owner instanceof PatternProviderLogicHost
               || owner instanceof PigmeePatternProviderBlockEntity
               || owner instanceof InterfaceLogicHost && otherNode.getGrid().equals(this.getMainNode().getGrid())) {
               directions.remove(entry.getKey());
            }
         }

         return directions;
      }
   }

   private void updatePatterns() {
      this.patterns.clear();
      if (this.f_58857_ != null) {
         for (ItemStack stack : this.patternInventory) {
            IPatternDetails details = PatternDetailsHelper.decodePattern(stack, this.f_58857_);
            if (details != null) {
               this.patterns.add(details);
            }
         }
      }

      ICraftingProvider.requestUpdate(this.getMainNode());
   }

   private void onReturnInventoryChanged() {
      this.saveChanges();
   }

   public void saveChangedInventory(AppEngInternalInventory inventory) {
      this.saveChanges();
   }

   public void onChangeInventory(InternalInventory inventory, int slot) {
      this.saveChanges();
      if (inventory == this.patternInventory) {
         this.updatePatterns();
      }
   }

   public boolean isClientSide() {
      return this.f_58857_ == null || this.f_58857_.m_5776_();
   }

   public void onReady() {
      super.onReady();
      this.updatePatterns();
   }

   public void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      this.patternInventory.writeToNBT(tag, "Patterns");
      this.returnInventory.writeToChildTag(tag, "ReturnInventory");
      ListTag pendingTag = new ListTag();

      for (GenericStack stack : this.pendingDispatch) {
         pendingTag.add(GenericStack.writeTag(stack));
      }

      if (pendingTag.isEmpty()) {
         tag.m_128473_("PendingDispatch");
         tag.m_128473_("PendingDirection");
      } else {
         tag.m_128365_("PendingDispatch", pendingTag);
         if (this.pendingDirection != null) {
            tag.m_128344_("PendingDirection", (byte)this.pendingDirection.m_122411_());
         } else {
            tag.m_128473_("PendingDirection");
         }
      }
   }

   public void loadTag(CompoundTag tag) {
      super.loadTag(tag);
      this.patternInventory.readFromNBT(tag, "Patterns");
      this.returnInventory.readFromTag(tag.m_128437_("ReturnInventory", 10));
      this.pendingDispatch.clear();
      ListTag pendingTag = tag.m_128437_("PendingDispatch", 10);

      for (int i = 0; i < pendingTag.size(); i++) {
         GenericStack stack = GenericStack.readTag(pendingTag.m_128728_(i));
         if (stack != null && stack.amount() > 0L) {
            this.pendingDispatch.add(stack);
         }
      }

      if (this.pendingDispatch.isEmpty()) {
         this.pendingDirection = null;
      } else if (tag.m_128425_("PendingDirection", 1)) {
         this.pendingDirection = Direction.m_122376_(tag.m_128445_("PendingDirection"));
      } else {
         this.pendingDirection = null;
      }
   }

   public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
      super.addAdditionalDrops(level, pos, drops);

      for (ItemStack stack : this.patternInventory) {
         if (!stack.m_41619_()) {
            drops.add(stack);
         }
      }

      this.returnInventory.addDrops(drops, level, pos);

      for (GenericStack stackx : this.pendingDispatch) {
         stackx.what().addDrops(stackx.amount(), drops, level, pos);
      }
   }

   public void m_6211_() {
      super.m_6211_();
      this.patternInventory.clear();
      this.returnInventory.clear();
      this.pendingDispatch.clear();
      this.pendingDirection = null;
   }

   public AECableType getCableConnectionType(Direction direction) {
      return AECableType.SMART;
   }

   public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
      Direction direction = ((PushDirection)this.m_58900_().m_61143_(PigmeePatternProviderBlock.PUSH_DIRECTION)).getDirection();
      return direction == null ? EnumSet.allOf(Direction.class) : EnumSet.complementOf(EnumSet.of(direction));
   }

   public void m_155250_(BlockState state) {
      super.m_155250_(state);
      this.onGridConnectableSidesChanged();
   }

   public Component m_5446_() {
      return Component.m_237115_("block.ae2lt.pigmee_pattern_provider");
   }

   public void openMenu(Player player, MenuLocator locator) {
      MenuOpener.open(PigmeePatternProviderMenu.TYPE, player, locator);
   }

   protected Item getItemFromBlockEntity() {
      return ((PigmeePatternProviderBlock)ModBlocks.PIGMEE_PATTERN_PROVIDER.get()).m_5456_();
   }
}
