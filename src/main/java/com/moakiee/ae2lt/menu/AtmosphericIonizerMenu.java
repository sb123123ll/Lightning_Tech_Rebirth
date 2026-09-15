package com.moakiee.ae2lt.menu;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import com.moakiee.ae2lt.blockentity.AtmosphericIonizerBlockEntity;
import com.moakiee.ae2lt.item.WeatherCondensateItem;
import com.moakiee.ae2lt.machine.atmosphericionizer.AtmosphericIonizerStatus;
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

public class AtmosphericIonizerMenu extends AEBaseMenu implements FrequencyBindingMenu {
   public static final MenuType<AtmosphericIonizerMenu> TYPE = Ae2ltMenuBuilder.buildUnregistered(
      MenuTypeBuilder.create(AtmosphericIonizerMenu::new, AtmosphericIonizerBlockEntity.class)
         .withMenuTitle(host -> Component.m_237115_("block.ae2lt.atmospheric_ionizer")),
      new ResourceLocation("ae2lt", "atmospheric_ionizer")
   );
   @GuiSync(50)
   public long consumedEnergy;
   @GuiSync(51)
   public long totalEnergy;
   @GuiSync(52)
   public int statusOrdinal;
   @GuiSync(53)
   public int typeOrdinal;
   private final AtmosphericIonizerBlockEntity host;
   private final Slot condensateSlot;

   public AtmosphericIonizerMenu(int id, Inventory playerInventory, AtmosphericIonizerBlockEntity host) {
      super(TYPE, id, playerInventory, host);
      this.host = host;
      this.condensateSlot = this.addSlot(new LargeStackAppEngSlot(host.getInventory(), 0), Ae2ltSlotSemantics.ATMOSPHERIC_IONIZER_CONDENSATE);
      this.createPlayerInventorySlots(playerInventory);
   }

   public void m_38946_() {
      if (this.isServerSide()) {
         this.consumedEnergy = this.host.getConsumedEnergy();
         this.totalEnergy = this.host.getTotalEnergy();
         this.statusOrdinal = this.host.getStatus().ordinal();
         WeatherCondensateItem.Type selectedType = this.host.getSelectedType();
         this.typeOrdinal = selectedType == null ? -1 : selectedType.ordinal();
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
               remainder = moveIntoSlots(sourceStack.m_41777_(), List.of(this.condensateSlot));
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

   public long getConsumedEnergy() {
      return this.consumedEnergy;
   }

   public long getTotalEnergyRequired() {
      return this.totalEnergy;
   }

   public double getProgress() {
      return this.totalEnergy <= 0L ? 0.0 : Math.min(1.0, (double)this.consumedEnergy / (double)this.totalEnergy);
   }

   public AtmosphericIonizerStatus getStatus() {
      return AtmosphericIonizerStatus.fromOrdinal(this.statusOrdinal);
   }

   public WeatherCondensateItem.Type getSelectedCondensateType() {
      return this.typeOrdinal < 0 ? null : WeatherCondensateItem.Type.fromOrdinal(this.typeOrdinal);
   }

   public Component getStatusMessage() {
      return Component.m_237110_("ae2lt.gui.atmospheric_ionizer.status.label", new Object[]{Component.m_237115_(this.getStatus().translationKey())});
   }

   public Component getTargetWeatherMessage() {
      WeatherCondensateItem.Type type = this.getSelectedCondensateType();
      Component weatherName = (Component)(type == null ? Component.m_237115_("ae2lt.gui.atmospheric_ionizer.target.none") : type.getWeatherName());
      return Component.m_237110_("ae2lt.gui.atmospheric_ionizer.target_weather", new Object[]{weatherName});
   }

   public Component getEnergyDemandMessage() {
      WeatherCondensateItem.Type type = this.getSelectedCondensateType();
      return Component.m_237110_("ae2lt.gui.atmospheric_ionizer.energy_need", new Object[]{type == null ? 0L : type.totalEnergy()});
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
