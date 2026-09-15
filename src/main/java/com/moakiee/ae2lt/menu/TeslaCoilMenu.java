package com.moakiee.ae2lt.menu;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import com.moakiee.ae2lt.blockentity.TeslaCoilBlockEntity;
import com.moakiee.ae2lt.machine.teslacoil.TeslaCoilMode;
import com.moakiee.ae2lt.machine.teslacoil.TeslaCoilStatus;
import com.moakiee.ae2lt.mixin.AEBaseMenuAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TeslaCoilMenu extends AEBaseMenu implements FrequencyBindingMenu {
   public static final MenuType<TeslaCoilMenu> TYPE = Ae2ltMenuBuilder.buildUnregistered(
      MenuTypeBuilder.create(TeslaCoilMenu::new, TeslaCoilBlockEntity.class).withMenuTitle(host -> Component.m_237115_("block.ae2lt.tesla_coil")),
      new ResourceLocation("ae2lt", "tesla_coil")
   );
   @GuiSync(40)
   public long storedEnergy;
   @GuiSync(41)
   public long consumedEnergy;
   @GuiSync(42)
   public long totalEnergy;
   @GuiSync(43)
   public boolean working;
   @GuiSync(44)
   public int modeOrdinal;
   @GuiSync(45)
   public int statusOrdinal;
   @GuiSync(46)
   public long highVoltageAvailable;
   @GuiSync(47)
   public long extremeHighVoltageAvailable;
   @GuiSync(48)
   public boolean matrixInstalled;
   private final TeslaCoilBlockEntity host;
   private final Slot dustSlot;
   private final Slot matrixSlot;

   public TeslaCoilMenu(int id, Inventory playerInventory, TeslaCoilBlockEntity host) {
      super(TYPE, id, playerInventory, host);
      this.host = host;
      this.dustSlot = this.addSlot(new LargeStackAppEngSlot(host.getInventory(), 0), Ae2ltSlotSemantics.TESLA_COIL_DUST);
      this.matrixSlot = this.addSlot(new LargeStackAppEngSlot(host.getInventory(), 1), Ae2ltSlotSemantics.TESLA_COIL_MATRIX);
      Ae2ltSlotBackgrounds.withBackground(this.matrixSlot, Ae2ltSlotBackgrounds.LIGHTNING_COLLAPSE_MATRIX);
      this.createPlayerInventorySlots(playerInventory);
      this.registerClientAction("cycleMode", this::cycleMode);
   }

   public void m_38946_() {
      if (this.isServerSide()) {
         this.storedEnergy = this.host.getEnergyStorage().getStoredEnergyLong();
         this.consumedEnergy = this.host.getConsumedEnergy();
         this.totalEnergy = this.host.hasLockedMode() ? this.host.getCurrentTotalEnergy() : 0L;
         this.working = this.host.isWorking();
         this.modeOrdinal = this.host.getSelectedMode().ordinal();
         this.statusOrdinal = this.host.getStatus().ordinal();
         this.highVoltageAvailable = this.host.getAvailableHighVoltage();
         this.extremeHighVoltageAvailable = this.host.getAvailableExtremeHighVoltage();
         this.matrixInstalled = this.host.isMatrixInstalled();
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
      return 16000000L;
   }

   public double getProgress() {
      return this.totalEnergy <= 0L ? 0.0 : Math.min(1.0, (double)this.consumedEnergy / (double)this.totalEnergy);
   }

   public TeslaCoilMode getMode() {
      return TeslaCoilMode.fromOrdinal(this.modeOrdinal);
   }

   public TeslaCoilStatus getStatus() {
      return TeslaCoilStatus.fromOrdinal(this.statusOrdinal);
   }

   public boolean isMatrixInstalled() {
      return this.matrixInstalled;
   }

   public Component getModeButtonMessage() {
      return Component.m_237110_("ae2lt.gui.tesla_coil.mode.button", new Object[]{Component.m_237115_(this.getMode().translationKey())});
   }

   public Component getStatusMessage() {
      return Component.m_237110_("ae2lt.gui.tesla_coil.status.label", new Object[]{Component.m_237115_(this.getStatus().translationKey())});
   }

   public Component getMatrixMessage() {
      return Component.m_237110_(
         "ae2lt.gui.tesla_coil.matrix.label",
         new Object[]{Component.m_237115_(this.isMatrixInstalled() ? "ae2lt.gui.tesla_coil.matrix.installed" : "ae2lt.gui.tesla_coil.matrix.missing")}
      );
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

   public void clientCycleMode() {
      this.sendClientAction("cycleMode");
   }

   public TeslaCoilBlockEntity getHost() {
      return this.host;
   }

   private void cycleMode() {
      if (this.isServerSide()) {
         this.host.cycleMode();
      }
   }

   private ItemStack moveFromPlayerInventory(ItemStack stack) {
      if (this.host.getInventory().isLightningCollapseMatrix(stack)) {
         return moveIntoSlots(stack, List.of(this.matrixSlot));
      } else {
         return this.host.getInventory().isOverloadCrystalDust(stack) ? moveIntoSlots(stack, List.of(this.dustSlot)) : stack;
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
}
