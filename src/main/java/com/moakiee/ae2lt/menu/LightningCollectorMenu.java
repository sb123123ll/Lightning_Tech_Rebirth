package com.moakiee.ae2lt.menu;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.interfaces.IProgressProvider;
import com.moakiee.ae2lt.blockentity.LightningCollectorBlockEntity;
import com.moakiee.ae2lt.item.ElectroChimeCrystalItem;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.mixin.AEBaseMenuAccessor;
import com.moakiee.ae2lt.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class LightningCollectorMenu extends AEBaseMenu implements IProgressProvider, FrequencyBindingMenu {
   public static final MenuType<LightningCollectorMenu> TYPE = Ae2ltMenuBuilder.buildUnregistered(
      MenuTypeBuilder.create(LightningCollectorMenu::new, LightningCollectorBlockEntity.class)
         .withMenuTitle(host -> Component.m_237115_("block.ae2lt.lightning_collector")),
      new ResourceLocation("ae2lt", "lightning_collector")
   );
   @GuiSync(30)
   public int catalysisValue;
   @GuiSync(31)
   public int previewHighMin;
   @GuiSync(32)
   public int previewHighMax;
   @GuiSync(33)
   public int previewExtremeMin;
   @GuiSync(34)
   public int previewExtremeMax;
   private final LightningCollectorBlockEntity host;
   private final Slot crystalSlot;

   public LightningCollectorMenu(int id, Inventory playerInventory, LightningCollectorBlockEntity host) {
      super(TYPE, id, playerInventory, host);
      this.host = host;
      this.crystalSlot = this.addSlot(new LargeStackAppEngSlot(host.getInventory(), 0), Ae2ltSlotSemantics.LIGHTNING_COLLECTOR_CRYSTAL);
      Ae2ltSlotBackgrounds.withBackground(this.crystalSlot, Ae2ltSlotBackgrounds.ELECTRO_CHIME_CRYSTAL);
      this.createPlayerInventorySlots(playerInventory);
   }

   public void m_38946_() {
      if (this.isServerSide()) {
         this.catalysisValue = this.host.getCatalysisValue();
         LightningCollectorBlockEntity.OutputPreview highPreview = this.host.getPreview(LightningKey.Tier.HIGH_VOLTAGE);
         LightningCollectorBlockEntity.OutputPreview extremePreview = this.host.getPreview(LightningKey.Tier.EXTREME_HIGH_VOLTAGE);
         this.previewHighMin = highPreview.min();
         this.previewHighMax = highPreview.max();
         this.previewExtremeMin = extremePreview.min();
         this.previewExtremeMax = extremePreview.max();
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
               remainder = moveIntoSlots(sourceStack.m_41777_(), List.of(this.crystalSlot));
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

   public LightningCollectorBlockEntity getHost() {
      return this.host;
   }

   public int getCurrentProgress() {
      ItemStack crystal = this.host.getInstalledCrystal();
      return crystal.m_150930_((Item)ModItems.PERFECT_ELECTRO_CHIME_CRYSTAL.get()) ? this.getMaxProgress() : this.catalysisValue;
   }

   public int getMaxProgress() {
      return Math.max(1, ElectroChimeCrystalItem.getMaxCatalysis());
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
