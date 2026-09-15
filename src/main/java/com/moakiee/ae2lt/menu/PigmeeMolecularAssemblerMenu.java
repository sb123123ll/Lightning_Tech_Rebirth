package com.moakiee.ae2lt.menu;

import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.client.Point;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.interfaces.IProgressProvider;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.IOptionalSlot;
import appeng.menu.slot.OutputSlot;
import appeng.menu.slot.RestrictedInputSlot;
import appeng.menu.slot.RestrictedInputSlot.PlacableItemType;
import com.moakiee.ae2lt.blockentity.PigmeeMolecularAssemblerBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class PigmeeMolecularAssemblerMenu extends AEBaseMenu implements IProgressProvider {
   public static final MenuType<PigmeeMolecularAssemblerMenu> TYPE = MenuTypeBuilder.create(
         PigmeeMolecularAssemblerMenu::new, PigmeeMolecularAssemblerBlockEntity.class
      )
      .withMenuTitle(host -> Component.m_237115_("block.ae2lt.pigmee_molecular_assembler"))
      .build("pigmee_molecular_assembler");
   private static final int MAX_CRAFT_PROGRESS = 100;
   private final PigmeeMolecularAssemblerBlockEntity molecularAssembler;
   private Slot encodedPatternSlot;
   @GuiSync(4)
   public int craftProgress;

   public PigmeeMolecularAssemblerMenu(int id, Inventory playerInventory, PigmeeMolecularAssemblerBlockEntity blockEntity) {
      super(TYPE, id, playerInventory, blockEntity);
      this.molecularAssembler = blockEntity;
      this.setupMachineSlots();
      this.createPlayerInventorySlots(playerInventory);
   }

   private void setupMachineSlots() {
      InternalInventory inventory = this.molecularAssembler.getSubInventory(PigmeeMolecularAssemblerBlockEntity.INV_MAIN);

      for (int slot = 0; slot < 9; slot++) {
         this.addSlot(new PigmeeMolecularAssemblerMenu.CraftingGridSlot(inventory, slot), SlotSemantics.MACHINE_CRAFTING_GRID);
      }

      RestrictedInputSlot patternSlot = new RestrictedInputSlot(PlacableItemType.MOLECULAR_ASSEMBLER_PATTERN, inventory, 10);
      patternSlot.setIcon(null);
      this.encodedPatternSlot = this.addSlot(patternSlot, SlotSemantics.ENCODED_PATTERN);
      this.addSlot(new OutputSlot(inventory, 9, null), SlotSemantics.MACHINE_OUTPUT);
   }

   public boolean isValidItemForSlot(int slotIndex, ItemStack stack) {
      IMolecularAssemblerSupportedPattern pattern = this.molecularAssembler.getCurrentPattern();
      return pattern != null && pattern.isItemValid(slotIndex, AEItemKey.of(stack), this.molecularAssembler.m_58904_());
   }

   public void m_38946_() {
      this.craftProgress = this.molecularAssembler.getCraftingProgress();
      super.m_38946_();
   }

   public int getCurrentProgress() {
      return this.craftProgress;
   }

   public int getMaxProgress() {
      return 100;
   }

   public void onSlotChange(Slot changedSlot) {
      if (changedSlot == this.encodedPatternSlot) {
         for (Slot slot : this.f_38839_) {
            if (slot != changedSlot && slot instanceof AppEngSlot appEngSlot) {
               appEngSlot.resetCachedValidation();
            }
         }
      }
   }

   private final class CraftingGridSlot extends AppEngSlot implements IOptionalSlot {
      private CraftingGridSlot(InternalInventory inventory, int slot) {
         super(inventory, slot);
      }

      public boolean m_5857_(ItemStack stack) {
         return super.m_5857_(stack) && PigmeeMolecularAssemblerMenu.this.isValidItemForSlot(this.getSlotIndex(), stack);
      }

      protected boolean getCurrentValidationState() {
         ItemStack stack = this.m_7993_();
         return stack.m_41619_() || this.m_5857_(stack);
      }

      public boolean isRenderDisabled() {
         return false;
      }

      public boolean isSlotEnabled() {
         int slotIndex = this.getSlotIndex();
         if (!this.getInventory().getStackInSlot(slotIndex).m_41619_()) {
            return true;
         } else {
            IMolecularAssemblerSupportedPattern pattern = PigmeeMolecularAssemblerMenu.this.molecularAssembler.getCurrentPattern();
            return slotIndex >= 0 && slotIndex < 9 && pattern != null && pattern.isSlotEnabled(slotIndex);
         }
      }

      public Point getBackgroundPos() {
         return new Point(this.f_40220_ - 1, this.f_40221_ - 1);
      }
   }
}
