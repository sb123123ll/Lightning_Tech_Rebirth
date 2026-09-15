package com.moakiee.ae2lt.menu;

import appeng.api.orientation.RelativeSide;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.ToolboxMenu;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import com.moakiee.ae2lt.blockentity.LightningAssemblyChamberBlockEntity;
import com.moakiee.ae2lt.mixin.AEBaseMenuAccessor;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class LightningAssemblyChamberMenu extends AEBaseMenu implements FrequencyBindingMenu {
   public static final MenuType<LightningAssemblyChamberMenu> TYPE = Ae2ltMenuBuilder.buildUnregistered(
      MenuTypeBuilder.create(LightningAssemblyChamberMenu::new, LightningAssemblyChamberBlockEntity.class)
         .withMenuTitle(host -> Component.m_237115_("block.ae2lt.lightning_assembly_chamber")),
      new ResourceLocation("ae2lt", "lightning_assembly_chamber")
   );
   private static final RelativeSide[] OUTPUT_SIDES = RelativeSide.values();
   @GuiSync(20)
   public long storedEnergy;
   @GuiSync(21)
   public long consumedEnergy;
   @GuiSync(22)
   public long totalEnergy;
   @GuiSync(23)
   public boolean working;
   @GuiSync(24)
   public boolean autoExport;
   @GuiSync(25)
   public int outputSideMask;
   @GuiSync(26)
   public long highVoltageAvailable;
   @GuiSync(27)
   public long extremeHighVoltageAvailable;
   private final LightningAssemblyChamberBlockEntity host;
   private final List<Slot> machineInputSlots = new ArrayList<>(9);
   private final Slot catalystSlot;
   private final ToolboxMenu toolbox;

   public LightningAssemblyChamberMenu(int id, Inventory playerInventory, LightningAssemblyChamberBlockEntity host) {
      super(TYPE, id, playerInventory, host);
      this.host = host;
      this.toolbox = new ToolboxMenu(this);
      this.addMachineSlots();
      this.catalystSlot = this.addSlot(new LargeStackAppEngSlot(host.getInventory(), 9), Ae2ltSlotSemantics.LIGHTNING_ASSEMBLY_CATALYST);
      Ae2ltSlotBackgrounds.withBackground(this.catalystSlot, Ae2ltSlotBackgrounds.LIGHTNING_COLLAPSE_MATRIX);
      this.addSlot(new LargeStackAppEngSlot(host.getInventory(), 10), SlotSemantics.MACHINE_OUTPUT);
      this.setupUpgrades(host.getUpgrades());
      this.createPlayerInventorySlots(playerInventory);
      this.registerClientAction("toggleAutoExport", this::toggleAutoExport);
      this.registerClientAction("toggleOutputSide", Integer.class, this::toggleOutputSide);
      this.registerClientAction("clearOutputSides", this::clearOutputSides);
   }

   private void addMachineSlots() {
      this.machineInputSlots.add(this.addSlot(new LargeStackAppEngSlot(this.host.getInventory(), 0), Ae2ltSlotSemantics.LIGHTNING_ASSEMBLY_INPUT_0));
      this.machineInputSlots.add(this.addSlot(new LargeStackAppEngSlot(this.host.getInventory(), 1), Ae2ltSlotSemantics.LIGHTNING_ASSEMBLY_INPUT_1));
      this.machineInputSlots.add(this.addSlot(new LargeStackAppEngSlot(this.host.getInventory(), 2), Ae2ltSlotSemantics.LIGHTNING_ASSEMBLY_INPUT_2));
      this.machineInputSlots.add(this.addSlot(new LargeStackAppEngSlot(this.host.getInventory(), 3), Ae2ltSlotSemantics.LIGHTNING_ASSEMBLY_INPUT_3));
      this.machineInputSlots.add(this.addSlot(new LargeStackAppEngSlot(this.host.getInventory(), 4), Ae2ltSlotSemantics.LIGHTNING_ASSEMBLY_INPUT_4));
      this.machineInputSlots.add(this.addSlot(new LargeStackAppEngSlot(this.host.getInventory(), 5), Ae2ltSlotSemantics.LIGHTNING_ASSEMBLY_INPUT_5));
      this.machineInputSlots.add(this.addSlot(new LargeStackAppEngSlot(this.host.getInventory(), 6), Ae2ltSlotSemantics.LIGHTNING_ASSEMBLY_INPUT_6));
      this.machineInputSlots.add(this.addSlot(new LargeStackAppEngSlot(this.host.getInventory(), 7), Ae2ltSlotSemantics.LIGHTNING_ASSEMBLY_INPUT_7));
      this.machineInputSlots.add(this.addSlot(new LargeStackAppEngSlot(this.host.getInventory(), 8), Ae2ltSlotSemantics.LIGHTNING_ASSEMBLY_INPUT_8));
   }

   public void m_38946_() {
      if (this.isServerSide()) {
         this.toolbox.tick();
         this.storedEnergy = this.host.getEnergyStorage().getStoredEnergyLong();
         this.consumedEnergy = this.host.getConsumedEnergy();
         this.totalEnergy = this.host.getLockedRecipe().map(lockedRecipe -> lockedRecipe.totalEnergy()).orElse(0L);
         this.working = this.host.isWorking();
         this.autoExport = this.host.isAutoExportEnabled();
         this.outputSideMask = toOutputSideMask(this.host.getAllowedOutputs());
         this.highVoltageAvailable = this.host.getAvailableHighVoltage();
         this.extremeHighVoltageAvailable = this.host.getAvailableExtremeHighVoltage();
      }

      super.m_38946_();
   }

   public ItemStack m_7648_(Player player, int idx) {
      if (!this.isClientSide() && idx >= 0 && idx < this.f_38839_.size()) {
         Slot sourceSlot = this.m_38853_(idx);
         if (sourceSlot.m_6657_() && sourceSlot.m_8010_(player)) {
            ItemStack sourceStack = sourceSlot.m_7993_();
            ItemStack original = sourceStack.m_41777_();
            ItemStack remainder;
            if (((AEBaseMenuAccessor)this).ae2lt$isPlayerSideSlot(sourceSlot)) {
               remainder = this.moveFromPlayerInventory(sourceStack.m_41777_());
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

   public long getStoredEnergy() {
      return this.storedEnergy;
   }

   public long getConsumedEnergy() {
      return this.consumedEnergy;
   }

   public long getTotalEnergy() {
      return this.totalEnergy;
   }

   public long getEnergyCapacity() {
      return 1000000L;
   }

   public boolean isWorking() {
      return this.working;
   }

   public boolean isAutoExportEnabled() {
      return this.autoExport;
   }

   public boolean isOutputSideEnabled(RelativeSide side) {
      return (this.outputSideMask & 1 << side.ordinal()) != 0;
   }

   public long getHighVoltageAvailable() {
      return this.highVoltageAvailable;
   }

   public long getExtremeHighVoltageAvailable() {
      return this.extremeHighVoltageAvailable;
   }

   public Component getHighVoltageMessage() {
      return Component.m_237110_("ae2lt.gui.lightning_status.high_voltage", new Object[]{this.highVoltageAvailable});
   }

   public Component getExtremeHighVoltageMessage() {
      return Component.m_237110_("ae2lt.gui.lightning_status.extreme_high_voltage", new Object[]{this.extremeHighVoltageAvailable});
   }

   public double getProgress() {
      return this.totalEnergy <= 0L ? 0.0 : Math.min(1.0, (double)this.consumedEnergy / (double)this.totalEnergy);
   }

   public void clientToggleAutoExport() {
      this.sendClientAction("toggleAutoExport");
   }

   public void clientToggleOutputSide(RelativeSide side) {
      this.sendClientAction("toggleOutputSide", side.ordinal());
   }

   public void clientClearOutputSides() {
      this.sendClientAction("clearOutputSides");
   }

   public LightningAssemblyChamberBlockEntity getHost() {
      return this.host;
   }

   public ToolboxMenu getToolbox() {
      return this.toolbox;
   }

   private void toggleAutoExport() {
      if (this.isServerSide()) {
         this.host.setAutoExportEnabled(!this.host.isAutoExportEnabled());
      }
   }

   private void toggleOutputSide(Integer ordinal) {
      if (this.isServerSide() && ordinal != null && ordinal >= 0 && ordinal < OUTPUT_SIDES.length) {
         EnumSet<RelativeSide> updated = this.host.getAllowedOutputs();
         RelativeSide side = OUTPUT_SIDES[ordinal];
         if (!updated.add(side)) {
            updated.remove(side);
         }

         this.host.updateOutputSides(updated);
      }
   }

   private void clearOutputSides() {
      if (this.isServerSide()) {
         this.host.updateOutputSides(EnumSet.noneOf(RelativeSide.class));
      }
   }

   private ItemStack moveFromPlayerInventory(ItemStack stack) {
      List<Slot> upgradeSlots = this.getUpgradeDestinationSlots(stack);
      return !upgradeSlots.isEmpty() ? moveIntoSlots(stack, upgradeSlots) : moveIntoSlots(stack, this.machineInputSlots);
   }

   private List<Slot> getUpgradeDestinationSlots(ItemStack stack) {
      ArrayList<Slot> result = new ArrayList<>();

      for (Slot slot : this.getSlots(SlotSemantics.UPGRADE)) {
         if (slot.m_5857_(stack)) {
            result.add(slot);
         }
      }

      return result;
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

   private static int toOutputSideMask(EnumSet<RelativeSide> sides) {
      int mask = 0;

      for (RelativeSide side : sides) {
         mask |= 1 << side.ordinal();
      }

      return mask;
   }
}
