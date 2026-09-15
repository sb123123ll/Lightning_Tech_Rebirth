package com.moakiee.ae2lt.menu;

import appeng.api.inventories.InternalInventory;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.AppEngSlot;
import com.moakiee.ae2lt.blockentity.OverloadDeviceWorkbenchBlockEntity;
import com.moakiee.ae2lt.blockentity.workbench.DeviceWorkbenchAdapter;
import com.moakiee.ae2lt.blockentity.workbench.StructuralSlotSpec;
import com.moakiee.ae2lt.device.DeviceItem;
import com.moakiee.ae2lt.device.DeviceKind;
import com.moakiee.ae2lt.device.DeviceSlotType;
import com.moakiee.ae2lt.menu.hub.DeviceHubDisplayRules;
import com.moakiee.ae2lt.util.SlotPositionAccess;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class OverloadDeviceWorkbenchMenu extends AEBaseMenu {
   public static final MenuType<OverloadDeviceWorkbenchMenu> TYPE = MenuTypeBuilder.create(
         OverloadDeviceWorkbenchMenu::new, OverloadDeviceWorkbenchBlockEntity.class
      )
      .withMenuTitle(host -> Component.m_237115_("block.ae2lt.overload_device_workbench"))
      .build("overload_device_workbench");
   public static final int DEVICE_X = 13;
   public static final int DEVICE_Y = 20;
   public static final int STRUCTURAL_X = 13;
   public static final int STRUCTURAL_Y = 48;
   public static final int STRUCTURAL_SPACING = 20;
   public static final int MAX_STRUCTURAL_SLOTS = 1;
   public static final int INPUT_X = 13;
   public static final int INPUT_Y = 74;
   public static final int INVENTORY_X = 8;
   public static final int INVENTORY_Y = 161;
   public static final int HOTBAR_Y = 219;
   @GuiSync(0)
   public int devicePresent;
   @GuiSync(1)
   public int moduleTypeCount;
   @GuiSync(2)
   public long energyCapacity;
   @GuiSync(3)
   public int coreInstalled;
   @GuiSync(4)
   public long energyStored;
   @GuiSync(5)
   public int installProgress;
   @GuiSync(6)
   public int gridConnected;
   @GuiSync(7)
   public int railgunDevice;
   @GuiSync(8)
   public int moduleUnitCount;
   public static final int INSTALL_TICKS = 20;
   private static final List<SlotSemantic> STRUCTURAL_SEMANTICS = List.of(Ae2ltSlotSemantics.OVERLOAD_DEVICE_WORKBENCH_CORE);
   private final OverloadDeviceWorkbenchBlockEntity host;
   private final Slot deviceSlot;
   private final List<Slot> structuralSlots = new ArrayList<>(1);
   private final Slot inputSlot;
   private final SimpleContainer inputContainer = new SimpleContainer(1);

   public OverloadDeviceWorkbenchMenu(int id, Inventory playerInventory, OverloadDeviceWorkbenchBlockEntity host) {
      super(TYPE, id, playerInventory, host);
      this.host = host;
      OverloadDeviceWorkbenchMenu.StructuralSlotContainer structuralContainer = new OverloadDeviceWorkbenchMenu.StructuralSlotContainer();
      this.deviceSlot = this.addSlot(
         new OverloadDeviceWorkbenchMenu.WorkbenchDeviceSlot(host.getDeviceInventory(), 0, 13, 20), Ae2ltSlotSemantics.OVERLOAD_DEVICE_WORKBENCH_DEVICE
      );

      for (int i = 0; i < 1; i++) {
         Slot slot = this.addSlot(new OverloadDeviceWorkbenchMenu.StructuralSlot(structuralContainer, i, 13, 48 + i * 20), STRUCTURAL_SEMANTICS.get(i));
         this.structuralSlots.add(slot);
      }

      this.inputSlot = this.addSlot(
         new OverloadDeviceWorkbenchMenu.ModuleInputSlot(this.inputContainer, 0, 13, 74), Ae2ltSlotSemantics.OVERLOAD_DEVICE_WORKBENCH_MODULE
      );
      this.addPlayerInventorySlots(playerInventory);
      this.registerClientAction("uninstallModuleAtIndex", Integer.class, this::handleUninstallAt);
      this.registerClientAction("uninstallAllOfIndex", Integer.class, this::handleUninstallAllAt);
      this.updateSnapshot();
   }

   public void m_38946_() {
      if (this.isServerSide()) {
         this.tickInstallProgress();
         this.updateSnapshot();
      }

      super.m_38946_();
   }

   public void m_6877_(Player player) {
      if (this.isServerSide()) {
         ItemStack residual = this.inputContainer.m_8016_(0);
         if (!residual.m_41619_() && !player.m_150109_().m_36054_(residual)) {
            player.m_36176_(residual, false);
         }
      }

      super.m_6877_(player);
   }

   public ItemStack m_7648_(Player player, int index) {
      if (!this.isClientSide() && index >= 0 && index < this.f_38839_.size()) {
         Slot sourceSlot = this.m_38853_(index);
         if (sourceSlot.m_6657_() && sourceSlot.m_8010_(player)) {
            ItemStack sourceStack = sourceSlot.m_7993_();
            ItemStack original = sourceStack.m_41777_();
            ItemStack remainder;
            if (this.isPlayerSideSlot(sourceSlot)) {
               List<Slot> destinations = this.getWorkbenchDestinationSlots(sourceStack);
               if (destinations.isEmpty()) {
                  return ItemStack.f_41583_;
               }

               remainder = moveIntoSlots(sourceStack.m_41777_(), destinations);
            } else {
               remainder = moveIntoSlots(sourceStack.m_41777_(), this.getPlayerDestinationSlots());
            }

            int moved = original.m_41613_() - remainder.m_41613_();
            if (moved <= 0) {
               return ItemStack.f_41583_;
            } else {
               sourceSlot.m_6201_(moved);
               sourceSlot.m_6654_();
               return original;
            }
         } else {
            return ItemStack.f_41583_;
         }
      } else {
         return ItemStack.f_41583_;
      }
   }

   public boolean m_6875_(Player player) {
      return !this.host.m_58901_() && this.host.m_58904_() != null
         ? this.host.m_58904_().m_7702_(this.host.m_58899_()) == this.host
            && player.m_9236_() == this.host.m_58904_()
            && player.m_20275_(
                  (double)this.host.m_58899_().m_123341_() + 0.5,
                  (double)this.host.m_58899_().m_123342_() + 0.5,
                  (double)this.host.m_58899_().m_123343_() + 0.5
               )
               <= 64.0
         : false;
   }

   public boolean hasDeviceInserted() {
      return this.devicePresent != 0;
   }

   public boolean hasCoreInstalled() {
      return this.coreInstalled != 0;
   }

   public boolean isRailgunDevice() {
      return this.railgunDevice != 0;
   }

   public List<StructuralSlotSpec> getStructuralSlotSpecs() {
      return this.host.getStructuralSlots();
   }

   public int getModuleMaxInstallAmount(ItemStack stack) {
      return this.host.moduleMaxInstallAmount(stack);
   }

   public Component getStatusText() {
      if (!this.hasDeviceInserted()) {
         return Component.m_237115_("ae2lt.overload_device_workbench.status.no_device");
      } else {
         return !this.hasCoreInstalled()
            ? Component.m_237115_("ae2lt.celestweave.status.missing_core")
            : Component.m_237115_("ae2lt.overload_device_workbench.status.ready");
      }
   }

   public List<ItemStack> getInstalledModuleList() {
      return this.host.getModuleList(this.registryAccess());
   }

   public void requestUninstall(int index, boolean all) {
      this.sendClientAction(all ? "uninstallAllOfIndex" : "uninstallModuleAtIndex", index);
   }

   private void handleUninstallAt(int index) {
      if (!this.isClientSide()) {
         List<ItemStack> list = this.host.getModuleList(this.registryAccess());
         if (index >= 0 && index < list.size()) {
            ItemStack stack = list.get(index);
            if (!stack.m_41619_()) {
               String id = this.host.moduleTypeId(stack);
               if (!id.isBlank()) {
                  ItemStack detached = this.host.uninstallOneModule(this.registryAccess(), id);
                  if (!detached.m_41619_()) {
                     this.giveToPlayer(detached);
                  }
               }
            }
         }
      }
   }

   private void handleUninstallAllAt(int index) {
      if (!this.isClientSide()) {
         List<ItemStack> list = this.host.getModuleList(this.registryAccess());
         if (index >= 0 && index < list.size()) {
            ItemStack stack = list.get(index);
            if (!stack.m_41619_()) {
               String id = this.host.moduleTypeId(stack);
               if (!id.isBlank()) {
                  ItemStack detached = this.host.uninstallAllOfModule(this.registryAccess(), id);
                  if (!detached.m_41619_()) {
                     int max = Math.max(1, detached.m_41741_());

                     while (!detached.m_41619_()) {
                        ItemStack chunk = detached.m_41620_(Math.min(max, detached.m_41613_()));
                        this.giveToPlayer(chunk);
                     }
                  }
               }
            }
         }
      }
   }

   private RegistryAccess registryAccess() {
      return this.getPlayer().m_9236_().m_9598_();
   }

   private boolean isPlayerSideSlot(Slot slot) {
      return this.getSlots(SlotSemantics.PLAYER_INVENTORY).contains(slot) || this.getSlots(SlotSemantics.PLAYER_HOTBAR).contains(slot);
   }

   private void giveToPlayer(ItemStack stack) {
      if (!stack.m_41619_()) {
         Player player = this.getPlayer();
         if (player != null) {
            if (!player.m_150109_().m_36054_(stack)) {
               player.m_36176_(stack, false);
            }
         }
      }
   }

   private void tickInstallProgress() {
      if (!this.host.hasInstalledDevice()) {
         this.installProgress = 0;
      } else {
         ItemStack stack = this.inputContainer.m_8020_(0);
         if (!stack.m_41619_() && this.host.canInstallOneModule(this.registryAccess(), stack)) {
            this.installProgress++;
            if (this.installProgress >= 20) {
               this.installProgress = 0;
               ItemStack unit = stack.m_255036_(1);
               if (this.host.installOneModule(this.registryAccess(), unit)) {
                  stack.m_41774_(1);
                  this.inputContainer.m_6836_(0, stack.m_41619_() ? ItemStack.f_41583_ : stack);
               }
            }
         } else {
            this.installProgress = 0;
         }
      }
   }

   private void updateSnapshot() {
      this.gridConnected = this.host.isActive() ? 1 : 0;
      ItemStack device = this.host.getInstalledDevice();
      DeviceWorkbenchAdapter adapter = this.host.currentAdapter();
      this.devicePresent = adapter == null ? 0 : 1;
      if (adapter != null && !device.m_41619_()) {
         this.railgunDevice = adapter.deviceKind() == DeviceKind.RAILGUN ? 1 : 0;
         List<ItemStack> modules = this.host.getModuleList(this.registryAccess());
         this.moduleTypeCount = modules.size();
         this.moduleUnitCount = DeviceHubDisplayRules.countModuleUnits(modules.stream().<Integer>map(ItemStack::m_41613_).toList());
         this.energyCapacity = adapter.energyBuffer().capacity(device);
         this.energyStored = adapter.energyBuffer().stored(device);
         this.coreInstalled = this.structuralInstalled(DeviceSlotType.CORE) ? 1 : 0;
      } else {
         this.moduleTypeCount = 0;
         this.energyCapacity = 0L;
         this.coreInstalled = 0;
         this.energyStored = 0L;
         this.railgunDevice = 0;
         this.moduleUnitCount = 0;
      }
   }

   private boolean structuralInstalled(DeviceSlotType type) {
      for (StructuralSlotSpec spec : this.host.getStructuralSlots()) {
         if (spec.slotType() == type && !this.host.getStructuralSlot(this.registryAccess(), spec).m_41619_()) {
            return true;
         }
      }

      return false;
   }

   private void addPlayerInventorySlots(Inventory playerInventory) {
      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < 9; column++) {
            int slotIndex = column + row * 9 + 9;
            this.addSlot(new Slot(playerInventory, slotIndex, 8 + column * 18, 161 + row * 18), SlotSemantics.PLAYER_INVENTORY);
         }
      }

      for (int column = 0; column < 9; column++) {
         this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 219), SlotSemantics.PLAYER_HOTBAR);
      }
   }

   private List<Slot> getWorkbenchDestinationSlots(ItemStack stack) {
      if (stack.m_41720_() instanceof DeviceItem) {
         return List.of(this.deviceSlot);
      } else if (!this.host.hasInstalledDevice()) {
         return List.of();
      } else {
         List<Slot> structural = this.structuralSlots.stream().filter(slot -> slot.m_5857_(stack)).toList();
         if (!structural.isEmpty()) {
            return structural;
         } else {
            return this.host.canInstallOneModule(this.registryAccess(), stack) ? List.of(this.inputSlot) : List.of();
         }
      }
   }

   private List<Slot> getPlayerDestinationSlots() {
      ArrayList<Slot> result = new ArrayList<>(this.getSlots(SlotSemantics.PLAYER_INVENTORY));
      result.addAll(this.getSlots(SlotSemantics.PLAYER_HOTBAR));
      return result;
   }

   private static ItemStack moveIntoSlots(ItemStack stack, List<Slot> destinations) {
      ItemStack remainder = stack;

      for (Slot slot : destinations) {
         if (slot.m_6657_()) {
            remainder = slot.m_150659_(remainder);
            if (remainder.m_41619_()) {
               return ItemStack.f_41583_;
            }
         }
      }

      for (Slot slotx : destinations) {
         if (!slotx.m_6657_()) {
            remainder = slotx.m_150659_(remainder);
            if (remainder.m_41619_()) {
               return ItemStack.f_41583_;
            }
         }
      }

      return remainder;
   }

   private StructuralSlotSpec specForSlot(int slot) {
      List<StructuralSlotSpec> specs = this.host.getStructuralSlots();
      return slot >= 0 && slot < specs.size() ? specs.get(slot) : null;
   }

   private final class ModuleInputSlot extends Slot {
      private ModuleInputSlot(SimpleContainer container, int slot, int x, int y) {
         super(container, slot, x, y);
      }

      public boolean m_5857_(ItemStack stack) {
         return OverloadDeviceWorkbenchMenu.this.host.hasInstalledDevice()
            && !stack.m_41619_()
            && OverloadDeviceWorkbenchMenu.this.host.canInstallOneModule(OverloadDeviceWorkbenchMenu.this.registryAccess(), stack);
      }

      public int m_6641_() {
         return 64;
      }
   }

   private final class StructuralSlot extends Slot {
      private StructuralSlot(OverloadDeviceWorkbenchMenu.StructuralSlotContainer container, int slot, int x, int y) {
         super(container, slot, x, y);
      }

      public boolean m_5857_(ItemStack stack) {
         StructuralSlotSpec spec = OverloadDeviceWorkbenchMenu.this.specForSlot(this.m_150661_());
         return spec != null && OverloadDeviceWorkbenchMenu.this.host.canPlaceStructural(OverloadDeviceWorkbenchMenu.this.registryAccess(), spec, stack);
      }

      public boolean m_8010_(Player player) {
         StructuralSlotSpec spec = OverloadDeviceWorkbenchMenu.this.specForSlot(this.m_150661_());
         return spec != null
            && OverloadDeviceWorkbenchMenu.this.host
               .mayPickupStructural(OverloadDeviceWorkbenchMenu.this.registryAccess(), spec, player, OverloadDeviceWorkbenchMenu.this.m_142621_())
            && super.m_8010_(player);
      }

      public int m_6641_() {
         return 1;
      }
   }

   private final class StructuralSlotContainer extends SimpleContainer {
      private StructuralSlotContainer() {
         super(1);
      }

      public ItemStack m_8020_(int slot) {
         StructuralSlotSpec spec = OverloadDeviceWorkbenchMenu.this.specForSlot(slot);
         return spec == null
            ? ItemStack.f_41583_
            : OverloadDeviceWorkbenchMenu.this.host.getStructuralSlot(OverloadDeviceWorkbenchMenu.this.registryAccess(), spec);
      }

      public ItemStack m_7407_(int slot, int amount) {
         StructuralSlotSpec spec = OverloadDeviceWorkbenchMenu.this.specForSlot(slot);
         return spec == null
            ? ItemStack.f_41583_
            : OverloadDeviceWorkbenchMenu.this.host.removeStructuralSlot(OverloadDeviceWorkbenchMenu.this.registryAccess(), spec, amount);
      }

      public ItemStack m_8016_(int slot) {
         StructuralSlotSpec spec = OverloadDeviceWorkbenchMenu.this.specForSlot(slot);
         return spec == null
            ? ItemStack.f_41583_
            : OverloadDeviceWorkbenchMenu.this.host.removeStructuralSlot(OverloadDeviceWorkbenchMenu.this.registryAccess(), spec, Integer.MAX_VALUE);
      }

      public void m_6836_(int slot, ItemStack stack) {
         StructuralSlotSpec spec = OverloadDeviceWorkbenchMenu.this.specForSlot(slot);
         if (spec != null) {
            OverloadDeviceWorkbenchMenu.this.host.setStructuralSlot(OverloadDeviceWorkbenchMenu.this.registryAccess(), spec, stack);
         }
      }

      public boolean m_6542_(Player player) {
         return OverloadDeviceWorkbenchMenu.this.m_6875_(player);
      }
   }

   private static final class WorkbenchDeviceSlot extends AppEngSlot {
      private WorkbenchDeviceSlot(InternalInventory inventory, int invSlot, int x, int y) {
         super(inventory, invSlot);
         SlotPositionAccess.set(this, x, y);
      }

      public int m_6641_() {
         return 1;
      }
   }
}
