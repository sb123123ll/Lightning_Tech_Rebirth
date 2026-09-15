package com.moakiee.ae2lt.menu;

import appeng.api.orientation.RelativeSide;
import appeng.api.upgrades.Upgrades;
import appeng.core.localization.GuiText;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.ToolboxMenu;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import com.moakiee.ae2lt.blockentity.OverloadProcessingFactoryBlockEntity;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingLockedRecipe;
import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipeCandidate;
import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipeService;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.mixin.AEBaseMenuAccessor;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
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

public class OverloadProcessingFactoryMenu extends AEBaseMenu implements FrequencyBindingMenu {
   public static final MenuType<OverloadProcessingFactoryMenu> TYPE = Ae2ltMenuBuilder.buildUnregistered(
      MenuTypeBuilder.create(OverloadProcessingFactoryMenu::new, OverloadProcessingFactoryBlockEntity.class)
         .withMenuTitle(host -> Component.m_237115_("block.ae2lt.overload_processing_factory")),
      new ResourceLocation("ae2lt", "overload_processing_factory")
   );
   private static final List<SlotSemantic> INPUT_SEMANTICS = List.of(
      Ae2ltSlotSemantics.OVERLOAD_FACTORY_INPUT_0,
      Ae2ltSlotSemantics.OVERLOAD_FACTORY_INPUT_1,
      Ae2ltSlotSemantics.OVERLOAD_FACTORY_INPUT_2,
      Ae2ltSlotSemantics.OVERLOAD_FACTORY_INPUT_3,
      Ae2ltSlotSemantics.OVERLOAD_FACTORY_INPUT_4,
      Ae2ltSlotSemantics.OVERLOAD_FACTORY_INPUT_5,
      Ae2ltSlotSemantics.OVERLOAD_FACTORY_INPUT_6,
      Ae2ltSlotSemantics.OVERLOAD_FACTORY_INPUT_7,
      Ae2ltSlotSemantics.OVERLOAD_FACTORY_INPUT_8
   );
   private static final List<SlotSemantic> OUTPUT_SEMANTICS = List.of(Ae2ltSlotSemantics.OVERLOAD_FACTORY_OUTPUT_0);
   private static final RelativeSide[] OUTPUT_SIDES = RelativeSide.values();
   @GuiSync(80)
   public long storedEnergy;
   @GuiSync(81)
   public long consumedEnergy;
   @GuiSync(82)
   public long totalEnergy;
   @GuiSync(83)
   public boolean working;
   @GuiSync(84)
   public int currentParallel;
   @GuiSync(85)
   public int parallelCapacity;
   @GuiSync(86)
   public long highVoltageAvailable;
   @GuiSync(87)
   public long extremeHighVoltageAvailable;
   @GuiSync(88)
   public int lightningTierOrdinal = -1;
   @GuiSync(89)
   public long lightningCost;
   @GuiSync(90)
   public int inputFluidId = -1;
   @GuiSync(91)
   public int inputFluidAmount;
   @GuiSync(92)
   public int outputFluidId = -1;
   @GuiSync(93)
   public int outputFluidAmount;
   @GuiSync(94)
   public boolean matrixSubstitutionActive;
   @GuiSync(95)
   public long equivalentHighVoltageCost;
   @GuiSync(96)
   public boolean autoExport;
   @GuiSync(97)
   public int outputSideMask;
   private final OverloadProcessingFactoryBlockEntity host;
   private final List<Slot> machineInputSlots = new ArrayList<>(9);
   private final Slot matrixSlot;
   private final ToolboxMenu toolbox;
   private int recipePreviewCooldown;
   private OverloadProcessingRecipeCandidate cachedProcessable;

   public OverloadProcessingFactoryMenu(int id, Inventory playerInventory, OverloadProcessingFactoryBlockEntity host) {
      super(TYPE, id, playerInventory, host);
      this.host = host;
      this.toolbox = new ToolboxMenu(this);
      this.addMachineSlots();
      this.matrixSlot = this.addSlot(new LargeStackAppEngSlot(host.getInventory(), 9), Ae2ltSlotSemantics.OVERLOAD_FACTORY_MATRIX);
      Ae2ltSlotBackgrounds.withBackground(this.matrixSlot, Ae2ltSlotBackgrounds.LIGHTNING_COLLAPSE_MATRIX);
      this.setupUpgrades(host.getUpgrades());
      this.createPlayerInventorySlots(playerInventory);
      this.registerClientAction("toggleAutoExport", this::toggleAutoExport);
      this.registerClientAction("toggleOutputSide", Integer.class, this::toggleOutputSide);
      this.registerClientAction("clearOutputSides", this::clearOutputSides);
      this.registerClientAction("insertFluid", Integer.class, this::insertFluidFromCarried);
      this.registerClientAction("extractFluid", Integer.class, this::extractFluidToCarried);
      this.registerClientAction("clearFluidTank", Integer.class, this::clearFluidTank);
   }

   private void addMachineSlots() {
      for (int index = 0; index < 9; index++) {
         this.machineInputSlots.add(this.addSlot(new LargeStackAppEngSlot(this.host.getInventory(), 0 + index), INPUT_SEMANTICS.get(index)));
      }

      for (int index = 0; index < 1; index++) {
         this.addSlot(new LargeStackAppEngSlot(this.host.getInventory(), 10 + index), OUTPUT_SEMANTICS.get(index));
      }
   }

   public void m_38946_() {
      if (this.isServerSide()) {
         this.toolbox.tick();
         this.storedEnergy = this.host.getEnergyStorage().getStoredEnergyLong();
         this.consumedEnergy = this.host.getConsumedEnergy();
         this.totalEnergy = this.host.getLockedRecipe().map(lockedRecipex -> lockedRecipex.totalEnergy()).orElse(0L);
         this.working = this.host.isWorking();
         this.parallelCapacity = this.host.getInstalledParallelCapacity();
         this.highVoltageAvailable = this.host.getAvailableHighVoltage();
         this.extremeHighVoltageAvailable = this.host.getAvailableExtremeHighVoltage();
         this.autoExport = this.host.isAutoExportEnabled();
         this.outputSideMask = toOutputSideMask(this.host.getAllowedOutputs());
         FluidStack inputFluid = this.host.getInputFluid();
         this.inputFluidId = inputFluid.isEmpty() ? -1 : BuiltInRegistries.f_257020_.m_7447_(inputFluid.getFluid());
         this.inputFluidAmount = inputFluid.getAmount();
         FluidStack outputFluid = this.host.getOutputFluid();
         this.outputFluidId = outputFluid.isEmpty() ? -1 : BuiltInRegistries.f_257020_.m_7447_(outputFluid.getFluid());
         this.outputFluidAmount = outputFluid.getAmount();
         OverloadProcessingLockedRecipe lockedRecipe = this.host.getLockedRecipe().orElse(null);
         OverloadProcessingRecipeCandidate processable;
         if (lockedRecipe == null) {
            if (this.recipePreviewCooldown <= 0) {
               this.cachedProcessable = this.host.findProcessableRecipe().orElse(null);
               this.recipePreviewCooldown = 10;
            } else {
               this.recipePreviewCooldown--;
            }

            processable = this.cachedProcessable;
         } else {
            processable = null;
            this.cachedProcessable = null;
            this.recipePreviewCooldown = 0;
         }

         if (lockedRecipe != null) {
            this.currentParallel = lockedRecipe.parallel();
            this.lightningTierOrdinal = lockedRecipe.lightningTier().ordinal();
            this.lightningCost = lockedRecipe.totalLightningCost();
         } else if (processable != null) {
            this.currentParallel = processable.parallel();
            this.lightningTierOrdinal = processable.recipe().lightningTier().ordinal();
            this.lightningCost = processable.totalLightningCost();
            this.totalEnergy = processable.totalEnergy();
         } else {
            this.currentParallel = 0;
            this.lightningTierOrdinal = -1;
            this.lightningCost = 0L;
         }

         if (this.lightningTierOrdinal >= 0 && this.lightningCost > 0L) {
            LightningKey.Tier tier = LightningKey.Tier.fromOrdinal(this.lightningTierOrdinal);
            Optional<OverloadProcessingRecipeService.LightningConsumptionPlan> plan = OverloadProcessingRecipeService.resolveLightningConsumption(
               this.host.getInventory(), tier, this.lightningCost, this.highVoltageAvailable, this.extremeHighVoltageAvailable
            );
            this.matrixSubstitutionActive = plan.map(OverloadProcessingRecipeService.LightningConsumptionPlan::matrixSubstitution).orElse(false);
            this.equivalentHighVoltageCost = OverloadProcessingRecipeService.getEquivalentHighVoltageCost(tier, this.lightningCost);
         } else {
            this.matrixSubstitutionActive = false;
            this.equivalentHighVoltageCost = 0L;
         }
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

   public OverloadProcessingFactoryBlockEntity getHost() {
      return this.host;
   }

   public ToolboxMenu getToolbox() {
      return this.toolbox;
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
      return AE2LTCommonConfig.overloadFactoryEnergyCapacity();
   }

   public int getInputTankCapacity() {
      return 1024000;
   }

   public int getOutputTankCapacity() {
      return 1024000;
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

   public double getProgress() {
      return this.totalEnergy <= 0L ? 0.0 : Math.min(1.0, (double)this.consumedEnergy / (double)this.totalEnergy);
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

   public FluidStack getInputFluid() {
      return this.getFluid(this.inputFluidId, this.inputFluidAmount);
   }

   public FluidStack getOutputFluid() {
      return this.getFluid(this.outputFluidId, this.outputFluidAmount);
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

   public void clientInsertFluid(int tankIndex) {
      this.sendClientAction("insertFluid", tankIndex);
   }

   public void clientExtractFluid(int tankIndex) {
      this.sendClientAction("extractFluid", tankIndex);
   }

   public void clientClearFluidTank(int tankIndex) {
      this.sendClientAction("clearFluidTank", tankIndex);
   }

   private void insertFluidFromCarried(Integer tankIndex) {
      if (this.isServerSide() && tankIndex != null) {
         this.host.tryInsertFluidFromCarried(this.getPlayer(), tankIndex);
         this.m_38946_();
      }
   }

   private void extractFluidToCarried(Integer tankIndex) {
      if (this.isServerSide() && tankIndex != null) {
         this.host.tryExtractFluidToCarried(this.getPlayer(), tankIndex);
         this.m_38946_();
      }
   }

   private void clearFluidTank(Integer tankIndex) {
      if (this.isServerSide() && tankIndex != null) {
         this.host.clearFluidTank(tankIndex);
         this.m_38946_();
      }
   }

   public List<Component> getCompatibleUpgradeLines() {
      ArrayList<Component> list = new ArrayList<>();
      list.add(GuiText.CompatibleUpgrades.text());
      list.addAll(Upgrades.getTooltipLinesForMachine(this.host.getUpgrades().getUpgradableItem()));
      return list;
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

   private FluidStack getFluid(int fluidId, int amount) {
      if (fluidId >= 0 && amount > 0) {
         Fluid fluid = (Fluid)BuiltInRegistries.f_257020_.m_7942_(fluidId);
         return fluid != null && fluid != Fluids.f_76191_ ? new FluidStack(fluid, amount) : FluidStack.EMPTY;
      } else {
         return FluidStack.EMPTY;
      }
   }

   private static int toOutputSideMask(EnumSet<RelativeSide> sides) {
      int mask = 0;

      for (RelativeSide side : sides) {
         mask |= 1 << side.ordinal();
      }

      return mask;
   }
}
