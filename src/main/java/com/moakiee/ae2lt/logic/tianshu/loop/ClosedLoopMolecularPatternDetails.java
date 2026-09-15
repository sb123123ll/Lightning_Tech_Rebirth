package com.moakiee.ae2lt.logic.tianshu.loop;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern.CraftingGridAccessor;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

class ClosedLoopMolecularPatternDetails extends ClosedLoopExpandedPatternDetails implements IMolecularAssemblerSupportedPattern {
   private final IMolecularAssemblerSupportedPattern molecular;

   ClosedLoopMolecularPatternDetails(
      IMolecularAssemblerSupportedPattern molecular,
      Map<AEKey, Long> seedAmounts,
      Set<AEKey> cycleKeys,
      UUID seedGroupId,
      boolean singleSeedInputPerMember,
      Map<Integer, AEKey> plannedSeedInputSlots,
      AEItemKey persistenceDefinition,
      int dispatchOrder
   ) {
      super(molecular, seedAmounts, cycleKeys, seedGroupId, singleSeedInputPerMember, plannedSeedInputSlots, persistenceDefinition, dispatchOrder);
      this.molecular = molecular;
   }

   public ItemStack assemble(Container input, Level level) {
      return this.molecular.assemble(input, level);
   }

   public boolean isItemValid(int slotIndex, AEItemKey key, Level level) {
      return this.molecular.isItemValid(slotIndex, key, level);
   }

   public boolean isSlotEnabled(int slot) {
      return this.molecular.isSlotEnabled(slot);
   }

   public void fillCraftingGrid(KeyCounter[] inputHolder, CraftingGridAccessor accessor) {
      this.molecular.fillCraftingGrid(inputHolder, accessor);
   }

   public NonNullList<ItemStack> getRemainingItems(CraftingContainer input) {
      return this.molecular.getRemainingItems(input);
   }
}
