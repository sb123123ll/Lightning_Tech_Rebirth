package com.moakiee.ae2lt.blockentity;

import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.util.DimensionalBlockPos;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import com.moakiee.ae2lt.block.OverloadDeviceWorkbenchBlock;
import com.moakiee.ae2lt.blockentity.workbench.DeviceWorkbenchAdapter;
import com.moakiee.ae2lt.blockentity.workbench.DeviceWorkbenchAdapters;
import com.moakiee.ae2lt.blockentity.workbench.StructuralSlotSpec;
import com.moakiee.ae2lt.device.DeviceItem;
import com.moakiee.ae2lt.menu.OverloadDeviceWorkbenchMenu;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import org.jetbrains.annotations.Nullable;

public class OverloadDeviceWorkbenchBlockEntity extends AENetworkBlockEntity implements InternalInventoryHost, IWirelessAccessPoint {
   private static final String TAG_DEVICE_INV = "DeviceInv";
   private final AppEngInternalInventory deviceInventory = new AppEngInternalInventory(this, 1, 1) {
      public boolean isItemValid(int slot, ItemStack stack) {
         return stack.m_41720_() instanceof DeviceItem;
      }
   };

   public OverloadDeviceWorkbenchBlockEntity(BlockPos pos, BlockState blockState) {
      super((BlockEntityType)ModBlockEntities.OVERLOAD_DEVICE_WORKBENCH.get(), pos, blockState);
      this.getMainNode().setIdlePowerUsage(0.0);
   }

   public AppEngInternalInventory getDeviceInventory() {
      return this.deviceInventory;
   }

   public ItemStack getInstalledDevice() {
      return this.deviceInventory.getStackInSlot(0);
   }

   public boolean hasInstalledDevice() {
      return !this.getInstalledDevice().m_41619_() && this.currentAdapter() != null;
   }

   @Nullable
   public DeviceWorkbenchAdapter currentAdapter() {
      return DeviceWorkbenchAdapters.get(this.getInstalledDevice()).orElse(null);
   }

   public List<StructuralSlotSpec> getStructuralSlots() {
      DeviceWorkbenchAdapter adapter = this.currentAdapter();
      return adapter == null ? List.of() : adapter.structuralSlots();
   }

   public ItemStack getStructuralSlot(Provider registries, StructuralSlotSpec spec) {
      DeviceWorkbenchAdapter adapter = this.currentAdapter();
      return adapter == null ? ItemStack.f_41583_ : adapter.getStructuralSlot(this.getInstalledDevice(), registries, spec);
   }

   public boolean canPlaceStructural(Provider registries, StructuralSlotSpec spec, ItemStack stack) {
      DeviceWorkbenchAdapter adapter = this.currentAdapter();
      return adapter != null && adapter.canPlaceStructural(this.getInstalledDevice(), registries, spec, stack);
   }

   public void setStructuralSlot(Provider registries, StructuralSlotSpec spec, ItemStack stack) {
      DeviceWorkbenchAdapter adapter = this.currentAdapter();
      if (adapter != null) {
         adapter.setStructuralSlot(this.getInstalledDevice(), registries, spec, stack);
         adapter.onModulesChanged(this.getInstalledDevice(), registries, this.dist());
         this.saveChanges();
      }
   }

   public ItemStack removeStructuralSlot(Provider registries, StructuralSlotSpec spec, int amount) {
      DeviceWorkbenchAdapter adapter = this.currentAdapter();
      if (adapter != null && amount > 0) {
         ItemStack removed = adapter.removeStructuralSlot(this.getInstalledDevice(), registries, spec, amount);
         if (!removed.m_41619_()) {
            adapter.onModulesChanged(this.getInstalledDevice(), registries, this.dist());
            this.saveChanges();
         }

         return removed;
      } else {
         return ItemStack.f_41583_;
      }
   }

   public boolean mayPickupStructural(Provider registries, StructuralSlotSpec spec, Player player, ItemStack carried) {
      DeviceWorkbenchAdapter adapter = this.currentAdapter();
      return adapter != null && adapter.mayPickupStructural(this.getInstalledDevice(), registries, spec, player, carried);
   }

   public List<ItemStack> getModuleList(Provider registries) {
      DeviceWorkbenchAdapter adapter = this.currentAdapter();
      return adapter == null ? List.of() : adapter.listModuleEntries(this.getInstalledDevice(), registries);
   }

   public boolean canInstallOneModule(Provider registries, ItemStack candidate) {
      DeviceWorkbenchAdapter adapter = this.currentAdapter();
      return adapter != null && adapter.canInstallOne(this.getInstalledDevice(), registries, candidate);
   }

   public boolean installOneModule(Provider registries, ItemStack candidate) {
      DeviceWorkbenchAdapter adapter = this.currentAdapter();
      if (adapter != null && candidate != null && !candidate.m_41619_()) {
         ItemStack device = this.getInstalledDevice();
         if (!adapter.installOne(device, registries, candidate)) {
            return false;
         } else {
            adapter.onModulesChanged(device, registries, this.dist());
            this.saveChanges();
            return true;
         }
      } else {
         return false;
      }
   }

   public ItemStack uninstallOneModule(Provider registries, String typeId) {
      DeviceWorkbenchAdapter adapter = this.currentAdapter();
      if (adapter == null) {
         return ItemStack.f_41583_;
      } else {
         ItemStack device = this.getInstalledDevice();
         ItemStack detached = adapter.uninstallOne(device, registries, typeId);
         if (detached.m_41619_()) {
            return ItemStack.f_41583_;
         } else {
            adapter.onModulesChanged(device, registries, this.dist());
            this.saveChanges();
            return detached;
         }
      }
   }

   public ItemStack uninstallAllOfModule(Provider registries, String typeId) {
      DeviceWorkbenchAdapter adapter = this.currentAdapter();
      if (adapter == null) {
         return ItemStack.f_41583_;
      } else {
         ItemStack device = this.getInstalledDevice();
         ItemStack detached = adapter.uninstallAll(device, registries, typeId);
         if (detached.m_41619_()) {
            return ItemStack.f_41583_;
         } else {
            adapter.onModulesChanged(device, registries, this.dist());
            this.saveChanges();
            return detached;
         }
      }
   }

   public String moduleTypeId(ItemStack stack) {
      DeviceWorkbenchAdapter adapter = this.currentAdapter();
      return adapter == null ? "" : adapter.moduleTypeId(stack);
   }

   public int moduleMaxInstallAmount(ItemStack stack) {
      DeviceWorkbenchAdapter adapter = this.currentAdapter();
      return adapter == null ? 0 : adapter.maxInstallAmount(stack);
   }

   public void openMenu(Player player, MenuLocator locator) {
      MenuOpener.open(OverloadDeviceWorkbenchMenu.TYPE, player, locator);
   }

   public void saveChangedInventory(AppEngInternalInventory inv) {
      this.saveChanges();
      this.markForUpdate();
   }

   public void onChangeInventory(InternalInventory inv, int slot) {
      if (inv == this.deviceInventory) {
         this.bindInsertedDevice();
         this.saveChanges();
      }
   }

   public boolean isClientSide() {
      return this.f_58857_ != null && this.f_58857_.m_5776_();
   }

   public void m_183515_(CompoundTag data) {
      super.m_183515_(data);
      this.deviceInventory.writeToNBT(data, "DeviceInv");
   }

   public void loadTag(CompoundTag data) {
      super.loadTag(data);
      this.deviceInventory.readFromNBT(data, "DeviceInv");
      this.bindInsertedDevice();
   }

   public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
      super.addAdditionalDrops(level, pos, drops);
      ItemStack device = this.getInstalledDevice();
      if (!device.m_41619_()) {
         drops.add(device.m_41777_());
      }
   }

   public void m_6211_() {
      super.m_6211_();
      this.deviceInventory.clear();
   }

   protected Item getItemFromBlockEntity() {
      return ((OverloadDeviceWorkbenchBlock)ModBlocks.OVERLOAD_DEVICE_WORKBENCH.get()).m_5456_();
   }

   public DimensionalBlockPos getLocation() {
      return new DimensionalBlockPos(this.f_58857_, this.f_58858_);
   }

   public double getRange() {
      return Double.MAX_VALUE;
   }

   public boolean isActive() {
      return this.getMainNode().isActive() && this.getMainNode().getGrid() != null;
   }

   public IGrid getGrid() {
      return this.getMainNode().getGrid();
   }

   public IGridNode getActionableNode() {
      return this.getMainNode().getNode();
   }

   private void bindInsertedDevice() {
      if (this.f_58857_ != null && !this.f_58857_.m_5776_()) {
         ItemStack device = this.getInstalledDevice();
         DeviceWorkbenchAdapter adapter = this.currentAdapter();
         if (!device.m_41619_() && adapter != null) {
            adapter.onDeviceInserted(device);
            adapter.networkBinding().bind(device, GlobalPos.m_122643_(this.f_58857_.m_46472_(), this.f_58858_));
         }
      }
   }

   private Dist dist() {
      return this.isClientSide() ? Dist.CLIENT : Dist.DEDICATED_SERVER;
   }
}
