package com.moakiee.ae2lt.logic.craft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;

public final class MatrixAutoBuildPlan {
   private static final BlockPos DEFAULT_PORT_LOCAL = new BlockPos(6, 5, 3);
   private final List<MatrixAutoBuildPlan.Placement> placements;
   private final List<BlockPos> blocked;
   private final int missingPatternStorages;

   private MatrixAutoBuildPlan(List<MatrixAutoBuildPlan.Placement> placements, List<BlockPos> blocked, int missingPatternStorages) {
      this.placements = List.copyOf(placements);
      this.blocked = List.copyOf(blocked);
      this.missingPatternStorages = missingPatternStorages;
   }

   public static MatrixAutoBuildPlan create(MatrixAutoBuildPlan.ComponentResolver resolver, int patternStorageBudget) {
      Objects.requireNonNull(resolver);
      ArrayList<MatrixAutoBuildPlan.Placement> placements = new ArrayList<>();
      ArrayList<BlockPos> blocked = new ArrayList<>();
      int remainingPatternStorages = Math.max(0, patternStorageBudget);
      BlockPos portTarget = selectPortTarget(resolver);
      boolean hasPatternStorage = hasExistingPatternStorage(resolver);
      boolean willPlacePatternStorage = false;

      for (MatrixMultiblockTemplate.Entry entry : MatrixMultiblockTemplate.entries()) {
         MatrixMultiblockRole role = entry.role();
         if (shouldAutoBuild(role)) {
            BlockPos local = entry.localPos();
            MatrixMultiblockComponent component = normalize(resolver.componentAt(local));
            if (role == MatrixMultiblockRole.PATTERN_BAY) {
               if (!component.isPatternStorage()) {
                  if (component != MatrixMultiblockComponent.AIR) {
                     blocked.add(local);
                  } else if (remainingPatternStorages > 0) {
                     placements.add(new MatrixAutoBuildPlan.Placement(local, MatrixAutoBuildPlan.Target.PATTERN_STORAGE));
                     remainingPatternStorages--;
                     willPlacePatternStorage = true;
                  }
               }
            } else {
               MatrixAutoBuildPlan.Target target = targetFor(role, local, portTarget);
               if (target != null && !matchesTarget(component, target)) {
                  if (component != MatrixMultiblockComponent.AIR) {
                     blocked.add(local);
                  } else {
                     placements.add(new MatrixAutoBuildPlan.Placement(local, target));
                  }
               }
            }
         }
      }

      placements.sort(
         Comparator.<MatrixAutoBuildPlan.Placement>comparingInt(placement -> placement.localPos().m_123342_())
            .thenComparingInt(placement -> horizontalDistanceFromController(placement.localPos()))
            .thenComparingInt(placement -> Math.abs(placement.localPos().m_123343_() - MatrixMultiblockTemplate.CONTROLLER_LOCAL.m_123343_()))
            .thenComparingInt(placement -> placement.localPos().m_123341_())
            .thenComparingInt(placement -> placement.localPos().m_123343_())
      );
      int missingPatternStorages = !hasPatternStorage && !willPlacePatternStorage ? 1 : 0;
      return new MatrixAutoBuildPlan(placements, blocked, missingPatternStorages);
   }

   private static int horizontalDistanceFromController(BlockPos pos) {
      return Math.abs(pos.m_123341_() - MatrixMultiblockTemplate.CONTROLLER_LOCAL.m_123341_())
         + Math.abs(pos.m_123343_() - MatrixMultiblockTemplate.CONTROLLER_LOCAL.m_123343_());
   }

   public List<MatrixAutoBuildPlan.Placement> placements() {
      return this.placements;
   }

   public List<BlockPos> blocked() {
      return this.blocked;
   }

   public int missingPatternStorages() {
      return this.missingPatternStorages;
   }

   private static BlockPos selectPortTarget(MatrixAutoBuildPlan.ComponentResolver resolver) {
      BlockPos firstExistingPort = null;

      for (MatrixMultiblockTemplate.Entry entry : MatrixMultiblockTemplate.entries()) {
         if (entry.role() == MatrixMultiblockRole.PORT_CANDIDATE && normalize(resolver.componentAt(entry.localPos())) == MatrixMultiblockComponent.MATRIX_PORT) {
            if (entry.localPos().equals(DEFAULT_PORT_LOCAL)) {
               return DEFAULT_PORT_LOCAL;
            }

            if (firstExistingPort == null) {
               firstExistingPort = entry.localPos();
            }
         }
      }

      return firstExistingPort != null ? firstExistingPort : DEFAULT_PORT_LOCAL;
   }

   private static boolean hasExistingPatternStorage(MatrixAutoBuildPlan.ComponentResolver resolver) {
      for (MatrixMultiblockTemplate.Entry entry : MatrixMultiblockTemplate.entries()) {
         if (entry.role() == MatrixMultiblockRole.PATTERN_BAY && normalize(resolver.componentAt(entry.localPos())).isPatternStorage()) {
            return true;
         }
      }

      return false;
   }

   private static MatrixAutoBuildPlan.Target targetFor(MatrixMultiblockRole role, BlockPos local, BlockPos portTarget) {
      return switch (role) {
         case CASING -> MatrixAutoBuildPlan.Target.CASING;
         case CONSTRAINT_FRAME -> MatrixAutoBuildPlan.Target.CONSTRAINT_FRAME;
         case GLASS -> MatrixAutoBuildPlan.Target.GLASS;
         case PORT_CANDIDATE -> local.equals(portTarget) ? MatrixAutoBuildPlan.Target.PORT : MatrixAutoBuildPlan.Target.CONSTRAINT_FRAME;
         default -> null;
      };
   }

   private static boolean matchesTarget(MatrixMultiblockComponent component, MatrixAutoBuildPlan.Target target) {
      return switch (target) {
         case CASING -> component == MatrixMultiblockComponent.MATRIX_CASING;
         case CONSTRAINT_FRAME -> component == MatrixMultiblockComponent.MATRIX_CONSTRAINT_FRAME;
         case GLASS -> component == MatrixMultiblockComponent.MATRIX_GLASS;
         case PORT -> component == MatrixMultiblockComponent.MATRIX_PORT;
         case PATTERN_STORAGE -> component.isPatternStorage();
      };
   }

   private static boolean shouldAutoBuild(MatrixMultiblockRole role) {
      return role != MatrixMultiblockRole.EMPTY && role != MatrixMultiblockRole.CONTROLLER && role != MatrixMultiblockRole.CRAFTING_BAY;
   }

   private static MatrixMultiblockComponent normalize(MatrixMultiblockComponent component) {
      return component == null ? MatrixMultiblockComponent.OTHER : component;
   }

   @FunctionalInterface
   public interface ComponentResolver {
      MatrixMultiblockComponent componentAt(BlockPos var1);
   }

   public static record Placement(BlockPos localPos, MatrixAutoBuildPlan.Target target) {
   }

   public static enum Target {
      CASING,
      CONSTRAINT_FRAME,
      GLASS,
      PORT,
      PATTERN_STORAGE;
   }
}
