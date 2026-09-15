package com.moakiee.ae2lt.logic.craft;

import com.moakiee.ae2lt.block.MatrixMultiblockComponentBlock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class MatrixMultiblockScanner {
   private MatrixMultiblockScanner() {
   }

   public static Optional<MatrixMultiblockScanResult> find(BlockPos controllerPos, Direction orientation, MatrixMultiblockScanner.ComponentResolver resolver) {
      Objects.requireNonNull(controllerPos);
      Objects.requireNonNull(orientation);
      Objects.requireNonNull(resolver);
      MatrixMultiblockScanAttempt attempt = scan(controllerPos, orientation, resolver);
      return attempt.formed() ? Optional.of(attempt.result()) : Optional.empty();
   }

   public static Optional<MatrixMultiblockScanResult> findInLevel(BlockGetter level, BlockPos controllerPos, Direction orientation) {
      Objects.requireNonNull(level);
      if (level instanceof Level realLevel && !areRequiredChunksLoaded(realLevel, controllerPos, orientation)) {
         return Optional.empty();
      }

      return find(controllerPos, orientation, pos -> componentAt(level, pos));
   }

   public static MatrixMultiblockScanAttempt scan(Level level, BlockPos controllerPos, Direction orientation) {
      return !areRequiredChunksLoaded(level, controllerPos, orientation)
         ? new MatrixMultiblockScanAttempt(orientation, List.of(MatrixMultiblockScanIssue.CHUNKS_UNLOADED), null)
         : scan(controllerPos, orientation, pos -> componentAt(level, pos));
   }

   public static boolean areRequiredChunksLoaded(Level level, BlockPos controllerPos, Direction orientation) {
      return areRequiredChunksLoaded(controllerPos, orientation, level::m_46749_);
   }

   static boolean areRequiredChunksLoaded(BlockPos controllerPos, Direction orientation, Predicate<BlockPos> loaded) {
      Objects.requireNonNull(controllerPos);
      Objects.requireNonNull(orientation);
      Objects.requireNonNull(loaded);
      return loaded.test(worldPos(controllerPos, new BlockPos(0, 0, 0), orientation))
         && loaded.test(worldPos(controllerPos, new BlockPos(6, 0, 0), orientation))
         && loaded.test(worldPos(controllerPos, new BlockPos(0, 0, 6), orientation))
         && loaded.test(worldPos(controllerPos, new BlockPos(6, 0, 6), orientation));
   }

   public static MatrixMultiblockScanAttempt scan(BlockPos controllerPos, Direction orientation, MatrixMultiblockScanner.ComponentResolver resolver) {
      Objects.requireNonNull(controllerPos);
      Objects.requireNonNull(orientation);
      Objects.requireNonNull(resolver);
      if (orientation.m_122434_() == Axis.Y) {
         return new MatrixMultiblockScanAttempt(orientation, List.of(MatrixMultiblockScanIssue.UNEXPECTED_COMPONENT), null);
      } else {
         ArrayList<MatrixMultiblockScanIssue> issues = new ArrayList<>();
         ArrayList<MatrixMultiblockMember> members = new ArrayList<>();
         ArrayList<MatrixMultiblockMember> craftingMembers = new ArrayList<>();
         ArrayList<MatrixMultiblockMember> patternMembers = new ArrayList<>();
         BlockPos portPos = null;
         int portCount = 0;
         int amplifierUnitCount = 0;
         int dispatchCount = 0;
         int nonBlankUnitCount = 0;
         boolean hasMainCoreAtCenter = false;
         MatrixMultiblockComponent mainCoreAtCenter = null;
         boolean hasMainCoreOutsideCenter = false;
         int minX = Integer.MAX_VALUE;
         int minY = Integer.MAX_VALUE;
         int minZ = Integer.MAX_VALUE;
         int maxX = Integer.MIN_VALUE;
         int maxY = Integer.MIN_VALUE;
         int maxZ = Integer.MIN_VALUE;

         for (MatrixMultiblockTemplate.Entry entry : MatrixMultiblockTemplate.entries()) {
            BlockPos localPos = entry.localPos();
            MatrixMultiblockRole role = entry.role();
            BlockPos worldPos = worldPos(controllerPos, localPos, orientation);
            MatrixMultiblockComponent component = normalize(resolver.componentAt(worldPos));
            minX = Math.min(minX, worldPos.m_123341_());
            minY = Math.min(minY, worldPos.m_123342_());
            minZ = Math.min(minZ, worldPos.m_123343_());
            maxX = Math.max(maxX, worldPos.m_123341_());
            maxY = Math.max(maxY, worldPos.m_123342_());
            maxZ = Math.max(maxZ, worldPos.m_123343_());
            if (!accepts(role, component, localPos)) {
               addIssue(issues, MatrixMultiblockScanIssue.UNEXPECTED_COMPONENT);
            }

            if (role == MatrixMultiblockRole.PORT_CANDIDATE && component == MatrixMultiblockComponent.MATRIX_PORT) {
               portCount++;
               portPos = worldPos;
            }

            if (role == MatrixMultiblockRole.CRAFTING_BAY) {
               if (localPos.equals(MatrixMultiblockTemplate.CRAFTING_CENTER_LOCAL)) {
                  hasMainCoreAtCenter = component.isMainCore();
                  if (hasMainCoreAtCenter) {
                     mainCoreAtCenter = component;
                  }
               } else if (component.isMainCore()) {
                  hasMainCoreOutsideCenter = true;
               }

               if (component.isAmplifierUnit()) {
                  amplifierUnitCount++;
               }

               if (!localPos.equals(MatrixMultiblockTemplate.CRAFTING_CENTER_LOCAL)) {
                  if (component == MatrixMultiblockComponent.THREAD_UNIT_T1 || component == MatrixMultiblockComponent.THREAD_UNIT_T2) {
                     dispatchCount++;
                  }

                  if (component != MatrixMultiblockComponent.BLANK_UNIT) {
                     nonBlankUnitCount++;
                  }
               }
            }

            if (component != MatrixMultiblockComponent.AIR && role != MatrixMultiblockRole.EMPTY) {
               MatrixMultiblockMember member = new MatrixMultiblockMember(worldPos, localPos, role, component);
               members.add(member);
               if (role == MatrixMultiblockRole.CRAFTING_BAY) {
                  craftingMembers.add(member);
               } else if (role == MatrixMultiblockRole.PATTERN_BAY) {
                  patternMembers.add(member);
               }
            }
         }

         if (portCount == 0) {
            addIssue(issues, MatrixMultiblockScanIssue.MISSING_PORT);
         } else if (portCount > 1) {
            addIssue(issues, MatrixMultiblockScanIssue.MULTIPLE_PORTS);
         }

         if (!hasMainCoreAtCenter) {
            addIssue(issues, MatrixMultiblockScanIssue.MISSING_MAIN_CORE);
         }

         if (patternMembers.isEmpty()) {
            addIssue(issues, MatrixMultiblockScanIssue.MISSING_PATTERN_STORAGE);
         }

         if (hasMainCoreOutsideCenter) {
            addIssue(issues, MatrixMultiblockScanIssue.MAIN_CORE_OUTSIDE_CENTER);
         }

         if (mainCoreAtCenter != MatrixMultiblockComponent.MULTIDIMENSIONAL_MAIN_CORE && amplifierUnitCount > 15) {
            addIssue(issues, MatrixMultiblockScanIssue.AMPLIFIER_LIMIT_EXCEEDED);
         }

         if (mainCoreAtCenter != null && mainCoreAtCenter != MatrixMultiblockComponent.MULTIDIMENSIONAL_MAIN_CORE && dispatchCount == 0) {
            addIssue(issues, MatrixMultiblockScanIssue.MISSING_DISPATCH_UNIT);
         }

         if (mainCoreAtCenter == MatrixMultiblockComponent.STABLE_MAIN_CORE && amplifierUnitCount > 0) {
            addIssue(issues, MatrixMultiblockScanIssue.AMPLIFIER_NOT_SUPPORTED);
         }

         if (mainCoreAtCenter == MatrixMultiblockComponent.MULTIDIMENSIONAL_MAIN_CORE && nonBlankUnitCount > 0) {
            addIssue(issues, MatrixMultiblockScanIssue.MULTIDIMENSIONAL_UNIT_NOT_SUPPORTED);
         }

         MatrixMultiblockScanResult result = null;
         if (issues.isEmpty() && portPos != null) {
            result = new MatrixMultiblockScanResult(
               controllerPos, orientation, new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ), portPos, members, craftingMembers, patternMembers
            );
         }

         return new MatrixMultiblockScanAttempt(orientation, issues, result);
      }
   }

   public static BlockPos worldPos(BlockPos controllerPos, BlockPos localPos, Direction orientation) {
      int dx = localPos.m_123341_() - MatrixMultiblockTemplate.CONTROLLER_LOCAL.m_123341_();
      int dy = localPos.m_123342_() - MatrixMultiblockTemplate.CONTROLLER_LOCAL.m_123342_();
      int dz = localPos.m_123343_() - MatrixMultiblockTemplate.CONTROLLER_LOCAL.m_123343_();

      return switch (orientation) {
         case EAST -> controllerPos.m_7918_(dx, dy, dz);
         case SOUTH -> controllerPos.m_7918_(-dz, dy, dx);
         case WEST -> controllerPos.m_7918_(-dx, dy, -dz);
         case NORTH -> controllerPos.m_7918_(dz, dy, -dx);
         default -> throw new IllegalArgumentException("Matrix orientation must be horizontal: " + orientation);
      };
   }

   public static Set<BlockPos> candidateControllerPositions(BlockPos changedPos) {
      Objects.requireNonNull(changedPos);
      LinkedHashSet<BlockPos> candidates = new LinkedHashSet<>();

      for (Direction orientation : Plane.HORIZONTAL) {
         for (MatrixMultiblockTemplate.Entry entry : MatrixMultiblockTemplate.entries()) {
            candidates.add(controllerPosFor(changedPos, entry.localPos(), orientation));
         }
      }

      return Set.copyOf(candidates);
   }

   public static BlockPos controllerPosFor(BlockPos worldPos, BlockPos localPos, Direction orientation) {
      Objects.requireNonNull(worldPos);
      Objects.requireNonNull(localPos);
      Objects.requireNonNull(orientation);
      int dx = localPos.m_123341_() - MatrixMultiblockTemplate.CONTROLLER_LOCAL.m_123341_();
      int dy = localPos.m_123342_() - MatrixMultiblockTemplate.CONTROLLER_LOCAL.m_123342_();
      int dz = localPos.m_123343_() - MatrixMultiblockTemplate.CONTROLLER_LOCAL.m_123343_();

      return switch (orientation) {
         case EAST -> worldPos.m_7918_(-dx, -dy, -dz);
         case SOUTH -> worldPos.m_7918_(dz, -dy, -dx);
         case WEST -> worldPos.m_7918_(dx, -dy, dz);
         case NORTH -> worldPos.m_7918_(-dz, -dy, dx);
         default -> throw new IllegalArgumentException("Matrix orientation must be horizontal: " + orientation);
      };
   }

   public static MatrixMultiblockComponent componentAt(BlockGetter level, BlockPos pos) {
      BlockState state = level.m_8055_(pos);
      if (state.m_60795_()) {
         return MatrixMultiblockComponent.AIR;
      } else {
         return state.m_60734_() instanceof MatrixMultiblockComponentBlock componentBlock
            ? componentBlock.matrixComponent(state)
            : MatrixMultiblockComponent.OTHER;
      }
   }

   private static boolean accepts(MatrixMultiblockRole role, MatrixMultiblockComponent component, BlockPos localPos) {
      return switch (role) {
         case EMPTY -> true;
         case CASING -> component == MatrixMultiblockComponent.MATRIX_CASING;
         case CONSTRAINT_FRAME -> component == MatrixMultiblockComponent.MATRIX_CONSTRAINT_FRAME;
         case GLASS -> component == MatrixMultiblockComponent.MATRIX_GLASS;
         case CONTROLLER -> component == MatrixMultiblockComponent.MATRIX_CONTROLLER;
         case PORT_CANDIDATE -> component == MatrixMultiblockComponent.MATRIX_PORT || component == MatrixMultiblockComponent.MATRIX_CONSTRAINT_FRAME;
         case PATTERN_BAY -> component == MatrixMultiblockComponent.AIR || component.isPatternStorage();
         case CRAFTING_BAY -> acceptsCraftingBay(component, localPos);
      };
   }

   private static boolean acceptsCraftingBay(MatrixMultiblockComponent component, BlockPos localPos) {
      return localPos.equals(MatrixMultiblockTemplate.CRAFTING_CENTER_LOCAL) ? component.isMainCore() : component.isCraftingUnit();
   }

   private static MatrixMultiblockComponent normalize(MatrixMultiblockComponent component) {
      return component == null ? MatrixMultiblockComponent.OTHER : component;
   }

   private static void addIssue(List<MatrixMultiblockScanIssue> issues, MatrixMultiblockScanIssue issue) {
      if (!issues.contains(issue)) {
         issues.add(issue);
      }
   }

   @FunctionalInterface
   public interface ComponentResolver {
      MatrixMultiblockComponent componentAt(BlockPos var1);
   }
}
