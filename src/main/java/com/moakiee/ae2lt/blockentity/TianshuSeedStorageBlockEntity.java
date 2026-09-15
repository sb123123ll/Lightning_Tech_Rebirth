package com.moakiee.ae2lt.blockentity;

import appeng.api.config.Actionable;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.StorageCell;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import com.moakiee.ae2lt.menu.TianshuSeedStorageMenu;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.util.NativeStackDropHelper;
import it.unimi.dsi.fastutil.objects.Object2LongMap.Entry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class TianshuSeedStorageBlockEntity extends AEBaseBlockEntity implements InternalInventoryHost {
   public static final int CELL_SLOTS = 10;
   private static final String TAG_CELLS = "Cells";
   private static final String TAG_PORT_POS = "PortPos";
   private final AppEngInternalInventory cells = new AppEngInternalInventory(this, 10, 1) {
      public boolean isItemValid(int slot, ItemStack stack) {
         return stack.m_41613_() == 1 && StorageCells.isCellHandled(stack);
      }
   };
   private BlockPos portPos;

   public TianshuSeedStorageBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.TIANSHU_SEED_STORAGE.get(), pos, state);
   }

   public AppEngInternalInventory getCellInventory() {
      return this.cells;
   }

   public BlockPos getPortPos() {
      return this.portPos;
   }

   public void bindToPort(BlockPos newPortPos) {
      this.portPos = newPortPos == null ? null : newPortPos.m_7949_();
      this.saveChanges();
   }

   public long extract(AEKey key, long amount, Actionable mode, IActionSource source) {
      long remaining = Math.max(0L, amount);
      long total = 0L;

      for (int slot = 0; slot < this.cells.size() && remaining > 0L; slot++) {
         StorageCell cell = this.cell(slot);
         if (cell != null) {
            long moved = cell.extract(key, remaining, mode, source);
            if (moved > 0L && mode == Actionable.MODULATE) {
               cell.persist();
            }

            total += moved;
            remaining -= moved;
         }
      }

      return total;
   }

   public long insert(AEKey key, long amount, Actionable mode, IActionSource source) {
      long remaining = Math.max(0L, amount);
      long total = 0L;

      for (int slot = 0; slot < this.cells.size() && remaining > 0L; slot++) {
         StorageCell cell = this.cell(slot);
         if (cell != null) {
            long moved = cell.insert(key, remaining, mode, source);
            if (moved > 0L && mode == Actionable.MODULATE) {
               cell.persist();
            }

            total += moved;
            remaining -= moved;
         }
      }

      return total;
   }

   public long amount(AEKey key, IActionSource source) {
      return this.extract(key, Long.MAX_VALUE, Actionable.SIMULATE, source);
   }

   public void getAvailableStacks(KeyCounter out) {
      for (int slot = 0; slot < this.cells.size(); slot++) {
         StorageCell cell = this.cell(slot);
         if (cell != null) {
            KeyCounter snapshot = new KeyCounter();
            cell.getAvailableStacks(snapshot);

            for (Entry<AEKey> entry : snapshot) {
               long amount = entry.getLongValue();
               if (amount > 0L) {
                  long current = Math.max(0L, out.get((AEKey)entry.getKey()));
                  out.set((AEKey)entry.getKey(), current > Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount);
               }
            }
         }
      }
   }

   private StorageCell cell(int slot) {
      ItemStack stack = this.cells.getStackInSlot(slot);
      return stack.m_41619_() ? null : StorageCells.getCellInventory(stack, this::saveChanges);
   }

   public void openMenu(Player player) {
      MenuOpener.open(TianshuSeedStorageMenu.TYPE, player, MenuLocators.forBlockEntity(this));
   }

   public void dropCells() {
      if (this.f_58857_ != null) {
         ArrayList<ItemStack> drops = new ArrayList<>();

         for (int slot = 0; slot < this.cells.size(); slot++) {
            StorageCell cell = this.cell(slot);
            if (cell != null) {
               cell.persist();
            }

            ItemStack stack = this.cells.getStackInSlot(slot);
            if (!stack.m_41619_()) {
               NativeStackDropHelper.addDrops(drops, stack);
            }
         }

         this.cells.clear();

         for (ItemStack stack : drops) {
            NativeStackDropHelper.popResource(this.f_58857_, this.f_58858_, stack);
         }
      }
   }

   public void saveChangedInventory(AppEngInternalInventory inventory) {
      this.saveChanges();
      this.notifyPort();
   }

   public void onChangeInventory(InternalInventory inventory, int slot) {
      this.notifyPort();
   }

   private void notifyPort() {
      if (this.f_58857_ != null && this.portPos != null && this.f_58857_.m_7702_(this.portPos) instanceof TianshuSupercomputerPortBlockEntity port) {
         port.seedDrivesChanged();
      }
   }

   public boolean isClientSide() {
      return this.f_58857_ != null && this.f_58857_.f_46443_;
   }

   public void m_183515_(CompoundTag tag) {
      for (int slot = 0; slot < this.cells.size(); slot++) {
         StorageCell cell = this.cell(slot);
         if (cell != null) {
            cell.persist();
         }
      }

      super.m_183515_(tag);
      this.cells.writeToNBT(tag, "Cells");
      if (this.portPos != null) {
         tag.m_128356_("PortPos", this.portPos.m_121878_());
      }
   }

   public void loadTag(CompoundTag tag) {
      super.loadTag(tag);
      this.cells.readFromNBT(tag, "Cells");
      this.portPos = tag.m_128425_("PortPos", 4) ? BlockPos.m_122022_(tag.m_128454_("PortPos")) : null;
   }

   public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
      super.addAdditionalDrops(level, pos, drops);

      for (int slot = 0; slot < this.cells.size(); slot++) {
         ItemStack stack = this.cells.getStackInSlot(slot);
         if (!stack.m_41619_()) {
            NativeStackDropHelper.addDrops(drops, stack);
         }
      }
   }

   public void m_6211_() {
      this.cells.clear();
   }
}
