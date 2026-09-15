package com.moakiee.ae2lt.logic.craft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class MatrixMultiblockScanResult {
   private final BlockPos controllerPos;
   private final Direction orientation;
   private final BlockPos minPos;
   private final BlockPos maxPos;
   private final BlockPos portPos;
   private final List<MatrixMultiblockMember> members;
   private final List<MatrixMultiblockMember> craftingMembers;
   private final List<MatrixMultiblockMember> patternMembers;

   MatrixMultiblockScanResult(
      BlockPos controllerPos,
      Direction orientation,
      BlockPos minPos,
      BlockPos maxPos,
      BlockPos portPos,
      List<MatrixMultiblockMember> members,
      List<MatrixMultiblockMember> craftingMembers,
      List<MatrixMultiblockMember> patternMembers
   ) {
      this.controllerPos = controllerPos.m_7949_();
      this.orientation = orientation;
      this.minPos = minPos.m_7949_();
      this.maxPos = maxPos.m_7949_();
      this.portPos = portPos.m_7949_();
      this.members = List.copyOf(members);
      this.craftingMembers = List.copyOf(craftingMembers);
      this.patternMembers = List.copyOf(patternMembers);
   }

   public BlockPos controllerPos() {
      return this.controllerPos;
   }

   public Direction orientation() {
      return this.orientation;
   }

   public BlockPos minPos() {
      return this.minPos;
   }

   public BlockPos maxPos() {
      return this.maxPos;
   }

   public BlockPos portPos() {
      return this.portPos;
   }

   public List<MatrixMultiblockMember> members() {
      return this.members;
   }

   public List<MatrixMultiblockMember> craftingMembers() {
      return this.craftingMembers;
   }

   public List<MatrixMultiblockMember> patternMembers() {
      return this.patternMembers;
   }

   public List<MatrixCraftingUnit> craftingUnits() {
      ArrayList<MatrixCraftingUnit> units = new ArrayList<>();

      for (MatrixMultiblockMember member : this.craftingMembers) {
         MatrixCraftingUnit unit = member.component().toCraftingUnit(distanceToCraftingCenter(member.localPos()));
         if (unit != null) {
            units.add(unit);
         }
      }

      return List.copyOf(units);
   }

   public MatrixCraftingProfile craftingProfile() {
      return MatrixCraftingProfile.fromUnits(this.craftingUnits());
   }

   public List<MatrixPatternStorageTier> patternStorageTiers() {
      ArrayList<MatrixPatternStorageTier> tiers = new ArrayList<>();

      for (MatrixMultiblockMember member : this.patternMembers) {
         MatrixPatternStorageTier tier = member.component().patternStorageTier();
         if (tier != null) {
            tiers.add(tier);
         }
      }

      return List.copyOf(tiers);
   }

   public MatrixPatternRepository createEmptyPatternRepository() {
      ArrayList<MatrixPatternStorageUnit> units = new ArrayList<>();

      for (MatrixPatternStorageTier tier : this.patternStorageTiers()) {
         units.add(tier == MatrixPatternStorageTier.T2 ? MatrixPatternStorageUnit.t2() : MatrixPatternStorageUnit.t1());
      }

      return new MatrixPatternRepository(units);
   }

   private static int distanceToCraftingCenter(BlockPos localPos) {
      return Math.abs(localPos.m_123341_() - MatrixMultiblockTemplate.CRAFTING_CENTER_LOCAL.m_123341_())
         + Math.abs(localPos.m_123342_() - MatrixMultiblockTemplate.CRAFTING_CENTER_LOCAL.m_123342_())
         + Math.abs(localPos.m_123343_() - MatrixMultiblockTemplate.CRAFTING_CENTER_LOCAL.m_123343_());
   }
}
