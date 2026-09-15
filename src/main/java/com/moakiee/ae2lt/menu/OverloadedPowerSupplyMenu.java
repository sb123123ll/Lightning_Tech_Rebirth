package com.moakiee.ae2lt.menu;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.AppEngSlot;
import com.moakiee.ae2lt.blockentity.OverloadedPowerSupplyBlockEntity;
import com.moakiee.ae2lt.logic.OverloadedPowerSupplyLogic;
import com.moakiee.ae2lt.logic.energy.AppFluxBridge;
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

public class OverloadedPowerSupplyMenu extends AEBaseMenu implements FrequencyBindingMenu {
   public static final MenuType<OverloadedPowerSupplyMenu> TYPE = Ae2ltMenuBuilder.buildUnregistered(
      MenuTypeBuilder.create(OverloadedPowerSupplyMenu::new, OverloadedPowerSupplyBlockEntity.class)
         .withMenuTitle(host -> Component.m_237115_("block.ae2lt.overloaded_power_supply")),
      new ResourceLocation("ae2lt", "overloaded_power_supply")
   );
   @GuiSync(0)
   public long bufferCapacity;
   @GuiSync(1)
   public long bufferedEnergy;
   @GuiSync(2)
   public int connectionCount;
   @GuiSync(3)
   public int modeOrdinal;
   @GuiSync(4)
   public int ticketCount;
   @GuiSync(5)
   public int statusOrdinal;
   @GuiSync(6)
   public long lastTransferAmount;
   private final OverloadedPowerSupplyBlockEntity host;
   private final Slot cellSlot;

   public OverloadedPowerSupplyMenu(int id, Inventory playerInventory, OverloadedPowerSupplyBlockEntity host) {
      super(TYPE, id, playerInventory, host);
      this.host = host;
      this.cellSlot = this.addSlot(new AppEngSlot(host.getCellInventory(), 0), Ae2ltSlotSemantics.OVERLOADED_POWER_SUPPLY_CELL);
      this.createPlayerInventorySlots(playerInventory);
      this.registerClientAction("cycleMode", this::cycleMode);
   }

   public void m_38946_() {
      if (this.isServerSide()) {
         this.bufferCapacity = this.host.getBufferCapacity();
         this.bufferedEnergy = this.host.getSupplyLogic().getBufferedEnergy();
         this.connectionCount = this.host.getConnections().size();
         this.modeOrdinal = this.host.getMode().ordinal();
         this.ticketCount = this.host.getSupplyLogic().getActiveTicketCount();
         this.statusOrdinal = this.host.getSupplyLogic().getLastStatus().ordinal();
         this.lastTransferAmount = this.host.getSupplyLogic().getLastTransferAmount();
      }

      super.m_38946_();
   }

   public ItemStack m_7648_(Player player, int index) {
      if (!this.isClientSide() && index >= 0 && index < this.f_38839_.size()) {
         Slot sourceSlot = this.m_38853_(index);
         if (sourceSlot.m_6657_() && sourceSlot.m_8010_(player)) {
            ItemStack sourceStack = sourceSlot.m_7993_();
            ItemStack original = sourceStack.m_41777_();
            ItemStack remainder;
            if (((AEBaseMenuAccessor)this).ae2lt$isPlayerSideSlot(sourceSlot)) {
               if (!AppFluxBridge.isFluxCell(sourceStack)) {
                  return ItemStack.f_41583_;
               }

               remainder = moveIntoSlots(sourceStack.m_41777_(), List.of(this.cellSlot));
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

   public void clientCycleMode() {
      this.sendClientAction("cycleMode");
   }

   public Component getModeButtonMessage() {
      return this.getMode() == OverloadedPowerSupplyBlockEntity.PowerMode.OVERLOAD
         ? Component.m_237115_("ae2lt.gui.overloaded_power_supply.mode.overload")
         : Component.m_237115_("ae2lt.gui.overloaded_power_supply.mode.normal");
   }

   public Component getConnectionsMessage() {
      return Component.m_237110_("ae2lt.gui.overloaded_power_supply.connections", new Object[]{this.connectionCount});
   }

   public Component getBufferMessage() {
      return Component.m_237110_(
         "ae2lt.gui.overloaded_power_supply.buffer", new Object[]{Long.toString(this.bufferedEnergy), Long.toString(this.bufferCapacity)}
      );
   }

   public Component getTicketsMessage() {
      return Component.m_237110_("ae2lt.gui.overloaded_power_supply.tickets", new Object[]{this.ticketCount});
   }

   public Component getCellMessage() {
      return this.bufferCapacity > 0L
         ? Component.m_237115_("ae2lt.gui.overloaded_power_supply.cell_present")
         : Component.m_237115_("ae2lt.gui.overloaded_power_supply.no_cell");
   }

   public Component getStatusMessage() {
      return switch (this.getStatus()) {
         case APPFLUX_UNAVAILABLE -> Component.m_237115_("ae2lt.gui.overloaded_power_supply.status.appflux_unavailable");
         case NO_CELL -> Component.m_237115_("ae2lt.gui.overloaded_power_supply.status.no_cell");
         case NO_GRID -> Component.m_237115_("ae2lt.gui.overloaded_power_supply.status.no_grid");
         case NO_CONNECTIONS -> Component.m_237115_("ae2lt.gui.overloaded_power_supply.status.no_connections");
         case NO_VALID_TARGETS -> Component.m_237115_("ae2lt.gui.overloaded_power_supply.status.no_valid_targets");
         case NO_NETWORK_FE -> Component.m_237115_("ae2lt.gui.overloaded_power_supply.status.no_network_fe");
         case TARGET_UNSUPPORTED -> Component.m_237115_("ae2lt.gui.overloaded_power_supply.status.target_unsupported");
         case TARGET_BLOCKED -> Component.m_237115_("ae2lt.gui.overloaded_power_supply.status.target_blocked");
         case ACTIVE -> Component.m_237110_("ae2lt.gui.overloaded_power_supply.status.active", new Object[]{Long.toString(this.lastTransferAmount)});
         case IDLE -> Component.m_237115_("ae2lt.gui.overloaded_power_supply.status.idle");
      };
   }

   public OverloadedPowerSupplyLogic.Status getStatus() {
      return this.statusOrdinal >= 0 && this.statusOrdinal < OverloadedPowerSupplyLogic.Status.values().length
         ? OverloadedPowerSupplyLogic.Status.values()[this.statusOrdinal]
         : OverloadedPowerSupplyLogic.Status.IDLE;
   }

   public OverloadedPowerSupplyBlockEntity.PowerMode getMode() {
      return this.modeOrdinal >= 0 && this.modeOrdinal < OverloadedPowerSupplyBlockEntity.PowerMode.values().length
         ? OverloadedPowerSupplyBlockEntity.PowerMode.values()[this.modeOrdinal]
         : OverloadedPowerSupplyBlockEntity.PowerMode.NORMAL;
   }

   private void cycleMode() {
      if (this.isServerSide()) {
         this.host.cycleMode();
         this.m_38946_();
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
