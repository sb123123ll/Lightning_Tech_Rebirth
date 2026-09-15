package com.moakiee.ae2lt.menu;

import appeng.api.orientation.RelativeSide;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import com.moakiee.ae2lt.blockentity.CrystalCatalyzerBlockEntity;
import com.moakiee.ae2lt.machine.crystalcatalyzer.CrystalCatalyzerInventory;
import com.moakiee.ae2lt.machine.crystalcatalyzer.recipe.CrystalCatalyzerLockedRecipe;
import com.moakiee.ae2lt.machine.crystalcatalyzer.recipe.Mode;
import com.moakiee.ae2lt.mixin.AEBaseMenuAccessor;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

public class CrystalCatalyzerMenu extends AEBaseMenu implements FrequencyBindingMenu {
   public static final MenuType<CrystalCatalyzerMenu> TYPE = Ae2ltMenuBuilder.buildUnregistered(
      MenuTypeBuilder.create(CrystalCatalyzerMenu::new, CrystalCatalyzerBlockEntity.class)
         .withMenuTitle(host -> Component.m_237115_("block.ae2lt.crystal_catalyzer")),
      new ResourceLocation("ae2lt", "crystal_catalyzer")
   );
   @GuiSync(20)
   public long storedEnergy;
   @GuiSync(21)
   public long consumedEnergy;
   @GuiSync(22)
   public long totalEnergy;
   @GuiSync(23)
   public boolean working;
   @GuiSync(24)
   public int fluidId = -1;
   @GuiSync(25)
   public int fluidAmount;
   @GuiSync(26)
   public boolean autoExport;
   @GuiSync(27)
   public int outputSideMask;
   @GuiSync(28)
   public int modeOrdinal;
   @GuiSync(29)
   public long highVoltageAvailable;
   @GuiSync(30)
   public long extremeHighVoltageAvailable;
   private static final RelativeSide[] OUTPUT_SIDES = RelativeSide.values();
   private final CrystalCatalyzerBlockEntity host;
   private final Slot catalystSlot;
   private final Slot matrixSlot;
   private final Slot outputSlot;

   public CrystalCatalyzerMenu(int id, Inventory playerInventory, CrystalCatalyzerBlockEntity host) {
      super(TYPE, id, playerInventory, host);
      this.host = host;
      CrystalCatalyzerInventory inventory = host.getInventory();
      this.catalystSlot = this.addSlot(new LargeStackAppEngSlot(inventory, 0), Ae2ltSlotSemantics.CRYSTAL_CATALYZER_CATALYST);
      this.matrixSlot = this.addSlot(new LargeStackAppEngSlot(inventory, 1), Ae2ltSlotSemantics.CRYSTAL_CATALYZER_MATRIX);
      Ae2ltSlotBackgrounds.withBackground(this.matrixSlot, Ae2ltSlotBackgrounds.LIGHTNING_COLLAPSE_MATRIX);
      this.outputSlot = this.addSlot(new LargeStackAppEngSlot(inventory, 2), SlotSemantics.MACHINE_OUTPUT);
      this.createPlayerInventorySlots(playerInventory);
      this.registerClientAction("toggleAutoExport", this::toggleAutoExport);
      this.registerClientAction("toggleOutputSide", Integer.class, this::toggleOutputSide);
      this.registerClientAction("clearOutputSides", this::clearOutputSides);
      this.registerClientAction("insertFluid", this::insertFluidFromCarried);
      this.registerClientAction("extractFluid", this::extractFluidToCarried);
      this.registerClientAction("clearFluidTank", this::clearFluidTank);
      this.registerClientAction("cycleMode", this::cycleMode);
   }

   public void m_38946_() {
      if (this.isServerSide()) {
         this.storedEnergy = this.host.getEnergyStorage().getStoredEnergyLong();
         this.consumedEnergy = this.host.getConsumedEnergy();
         this.totalEnergy = this.host.getLockedRecipe().map(CrystalCatalyzerMenu::lockedRecipeEnergy).orElse(0L);
         this.working = this.host.isWorking();
         FluidStack fluid = this.host.getFluid();
         this.fluidId = fluid.isEmpty() ? -1 : BuiltInRegistries.f_257020_.m_7447_(fluid.getFluid());
         this.fluidAmount = fluid.getAmount();
         this.autoExport = this.host.isAutoExportEnabled();
         this.outputSideMask = toOutputSideMask(this.host.getAllowedOutputs());
         this.modeOrdinal = this.host.getMode().ordinal();
         this.highVoltageAvailable = this.host.getAvailableHighVoltage();
         this.extremeHighVoltageAvailable = this.host.getAvailableExtremeHighVoltage();
      }

      super.m_38946_();
   }

   private static long lockedRecipeEnergy(CrystalCatalyzerLockedRecipe recipe) {
      return recipe.totalEnergy();
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

   public CrystalCatalyzerBlockEntity getHost() {
      return this.host;
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

   public int getFluidCapacity() {
      return 16000;
   }

   public boolean isWorking() {
      return this.working;
   }

   public double getProgress() {
      return this.totalEnergy <= 0L ? 0.0 : Math.min(1.0, (double)this.consumedEnergy / (double)this.totalEnergy);
   }

   public FluidStack getFluid() {
      if (this.fluidId >= 0 && this.fluidAmount > 0) {
         Fluid fluid = (Fluid)BuiltInRegistries.f_257020_.m_7942_(this.fluidId);
         return fluid != null && fluid != Fluids.f_76191_ ? new FluidStack(fluid, this.fluidAmount) : FluidStack.EMPTY;
      } else {
         return FluidStack.EMPTY;
      }
   }

   public boolean isAutoExportEnabled() {
      return this.autoExport;
   }

   public boolean isOutputSideEnabled(RelativeSide side) {
      return (this.outputSideMask & 1 << side.ordinal()) != 0;
   }

   public Mode getMode() {
      Mode[] values = Mode.values();
      return this.modeOrdinal >= 0 && this.modeOrdinal < values.length ? values[this.modeOrdinal] : Mode.CRYSTAL;
   }

   public long getHighVoltageAvailable() {
      return this.highVoltageAvailable;
   }

   public long getExtremeHighVoltageAvailable() {
      return this.extremeHighVoltageAvailable;
   }

   public void clientCycleMode() {
      this.sendClientAction("cycleMode");
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

   public void clientInsertFluid() {
      this.sendClientAction("insertFluid");
   }

   public void clientExtractFluid() {
      this.sendClientAction("extractFluid");
   }

   public void clientClearFluidTank() {
      this.sendClientAction("clearFluidTank");
   }

   private void insertFluidFromCarried() {
      if (this.isServerSide()) {
         this.host.tryInsertFluidFromCarried(this.getPlayer());
         this.m_38946_();
      }
   }

   private void extractFluidToCarried() {
      if (this.isServerSide()) {
         this.host.tryExtractFluidToCarried(this.getPlayer());
         this.m_38946_();
      }
   }

   private void clearFluidTank() {
      if (this.isServerSide()) {
         this.host.clearFluidTank();
         this.m_38946_();
      }
   }

   private void toggleAutoExport() {
      if (this.isServerSide()) {
         this.host.setAutoExportEnabled(!this.host.isAutoExportEnabled());
      }
   }

   private void cycleMode() {
      if (this.isServerSide()) {
         this.host.cycleMode();
         this.m_38946_();
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

   private static int toOutputSideMask(EnumSet<RelativeSide> sides) {
      int mask = 0;

      for (RelativeSide side : sides) {
         mask |= 1 << side.ordinal();
      }

      return mask;
   }

   private ItemStack moveFromPlayerInventory(ItemStack stack) {
      return moveIntoSlots(stack, List.of(this.matrixSlot, this.catalystSlot));
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
