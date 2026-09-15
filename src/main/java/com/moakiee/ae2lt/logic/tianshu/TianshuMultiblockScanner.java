package com.moakiee.ae2lt.logic.tianshu;

import com.moakiee.ae2lt.block.TianshuSupercomputingUnitBlock;
import com.moakiee.ae2lt.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class TianshuMultiblockScanner {
   public static TianshuMultiblockScanAttempt scan(Level level, BlockPos controllerPos, Direction orientation) {
      return !areRequiredChunksLoaded(level, controllerPos, orientation)
         ? new TianshuMultiblockScanAttempt(null, List.of(TianshuMultiblockScanIssue.CHUNKS_UNLOADED))
         : scan(controllerPos, orientation, pos -> componentAt(level, pos));
   }

   public static boolean areRequiredChunksLoaded(Level level, BlockPos controllerPos, Direction orientation) {
      return areRequiredChunksLoaded(controllerPos, orientation, level::m_46749_);
   }

   static boolean areRequiredChunksLoaded(BlockPos controllerPos, Direction orientation, Predicate<BlockPos> loaded) {
      return loaded.test(worldPos(controllerPos, new BlockPos(0, 0, 0), orientation))
         && loaded.test(worldPos(controllerPos, new BlockPos(6, 0, 0), orientation))
         && loaded.test(worldPos(controllerPos, new BlockPos(0, 0, 6), orientation))
         && loaded.test(worldPos(controllerPos, new BlockPos(6, 0, 6), orientation));
   }

   public static TianshuMultiblockScanAttempt scan(BlockPos controllerPos, Direction orientation, Function<BlockPos, TianshuMultiblockComponent> resolver) {
      ArrayList<TianshuMultiblockScanIssue> issues = new ArrayList<>();
      ArrayList<BlockPos> members = new ArrayList<>();
      ArrayList<BlockPos> cores = new ArrayList<>(27);
      ArrayList<BlockPos> ports = new ArrayList<>(2);
      ArrayList<BlockPos> patternStorages = new ArrayList<>();
      ArrayList<BlockPos> seedStorages = new ArrayList<>();
      CpuMainCoreTier mainCore = null;
      int storageUnits = 0;
      int parallelUnits = 0;
      int amplifierUnits = 0;
      BlockPos min = null;
      BlockPos max = null;

      for (int x = 0; x < 7; x++) {
         for (int y = 0; y < 7; y++) {
            for (int z = 0; z < 7; z++) {
               BlockPos local = new BlockPos(x, y, z);
               TianshuMultiblockRole role = TianshuMultiblockTemplate.roleAt(local);
               BlockPos world = worldPos(controllerPos, local, orientation);
               min = min == null
                  ? world
                  : new BlockPos(
                     Math.min(min.m_123341_(), world.m_123341_()), Math.min(min.m_123342_(), world.m_123342_()), Math.min(min.m_123343_(), world.m_123343_())
                  );
               max = max == null
                  ? world
                  : new BlockPos(
                     Math.max(max.m_123341_(), world.m_123341_()), Math.max(max.m_123342_(), world.m_123342_()), Math.max(max.m_123343_(), world.m_123343_())
                  );
               TianshuMultiblockComponent component = resolver.apply(world);
               switch (role) {
                  case CONTROLLER:
                     if (component != TianshuMultiblockComponent.CONTROLLER) {
                        addOnce(issues, TianshuMultiblockScanIssue.INVALID_CONTROLLER);
                     } else {
                        members.add(world.m_7949_());
                     }
                     break;
                  case CASING:
                     if (component != TianshuMultiblockComponent.CASING) {
                        addOnce(issues, TianshuMultiblockScanIssue.MISSING_CASING);
                     } else {
                        members.add(world.m_7949_());
                     }
                     break;
                  case COOLING:
                     if (!component.fillsCoolingPosition()) {
                        addOnce(issues, TianshuMultiblockScanIssue.MISSING_COOLING);
                     } else {
                        trackClosedLoopStorage(component, world, patternStorages, seedStorages);
                        members.add(world.m_7949_());
                     }
                     break;
                  case GLASS:
                     if (component != TianshuMultiblockComponent.GLASS) {
                        addOnce(issues, TianshuMultiblockScanIssue.MISSING_GLASS);
                     } else {
                        members.add(world.m_7949_());
                     }
                     break;
                  case PORT_CANDIDATE:
                     if (component == TianshuMultiblockComponent.PORT) {
                        ports.add(world.m_7949_());
                        members.add(world.m_7949_());
                     } else if (component.fillsCoolingPosition()) {
                        trackClosedLoopStorage(component, world, patternStorages, seedStorages);
                        members.add(world.m_7949_());
                     } else {
                        addOnce(issues, TianshuMultiblockScanIssue.MISSING_COOLING);
                     }
                     break;
                  case CORE_RESERVED:
                     cores.add(world.m_7949_());
                     boolean center = local.equals(new BlockPos(3, 3, 3));
                     CpuMainCoreTier tier = mainTier(component);
                     if (center) {
                        if (tier == null) {
                           addOnce(issues, TianshuMultiblockScanIssue.MISSING_MAIN_CORE);
                        } else {
                           mainCore = tier;
                           members.add(world.m_7949_());
                        }
                     } else if (component == TianshuMultiblockComponent.BLANK_UNIT) {
                        members.add(world.m_7949_());
                     } else if (component == TianshuMultiblockComponent.STORAGE_UNIT) {
                        storageUnits++;
                        members.add(world.m_7949_());
                     } else if (component == TianshuMultiblockComponent.PARALLEL_UNIT) {
                        parallelUnits++;
                        members.add(world.m_7949_());
                     } else if (component == TianshuMultiblockComponent.AMPLIFIER_UNIT) {
                        amplifierUnits++;
                        members.add(world.m_7949_());
                     } else {
                        if (tier != null) {
                           addOnce(issues, TianshuMultiblockScanIssue.MAIN_CORE_OUTSIDE_CENTER);
                        }

                        addOnce(issues, TianshuMultiblockScanIssue.INVALID_PERIPHERAL_UNIT);
                     }
                  case IGNORED:
               }
            }
         }
      }

      if (ports.isEmpty()) {
         addOnce(issues, TianshuMultiblockScanIssue.MISSING_PORT);
      } else if (ports.size() > 1) {
         addOnce(issues, TianshuMultiblockScanIssue.MULTIPLE_PORTS);
      }

      if (mainCore != CpuMainCoreTier.MULTIDIMENSIONAL && parallelUnits == 0) {
         addOnce(issues, TianshuMultiblockScanIssue.MISSING_PARALLEL_UNIT);
      }

      if (mainCore == CpuMainCoreTier.MULTIDIMENSIONAL && storageUnits + parallelUnits + amplifierUnits > 0) {
         addOnce(issues, TianshuMultiblockScanIssue.INVALID_PERIPHERAL_UNIT);
      }

      if (mainCore != null && amplifierUnits > mainCore.computeTier().maxAmplifierUnits()) {
         addOnce(
            issues,
            mainCore.computeTier().maxAmplifierUnits() == 0
               ? TianshuMultiblockScanIssue.AMPLIFIER_UNIT_NOT_SUPPORTED
               : TianshuMultiblockScanIssue.TOO_MANY_AMPLIFIER_UNITS
         );
      }

      if (!issues.isEmpty()) {
         return new TianshuMultiblockScanAttempt(null, List.copyOf(issues));
      } else {
         CpuInternalCoreProfile profile = CpuInternalCoreCalculator.calculate(mainCore, storageUnits, parallelUnits, amplifierUnits);
         TianshuFunctionProfile functionProfile = new TianshuFunctionProfile(patternStorages.size(), seedStorages.size());
         return new TianshuMultiblockScanAttempt(
            new TianshuMultiblockScanResult(
               controllerPos.m_7949_(),
               orientation,
               min,
               max,
               ports.get(0),
               List.copyOf(members),
               List.copyOf(cores),
               List.copyOf(patternStorages),
               List.copyOf(seedStorages),
               profile,
               functionProfile
            ),
            List.of()
         );
      }
   }

   public static TianshuMultiblockComponent componentAt(Level level, BlockPos pos) {
      BlockState state = level.m_8055_(pos);
      if (state.m_60795_()) {
         return TianshuMultiblockComponent.AIR;
      } else if (state.m_60713_((Block)ModBlocks.TIANSHU_SUPERCOMPUTER_CASING.get())) {
         return TianshuMultiblockComponent.CASING;
      } else if (state.m_60713_((Block)ModBlocks.PHASE_CHANGE_COOLING_UNIT.get())) {
         return TianshuMultiblockComponent.COOLING;
      } else if (state.m_60713_((Block)ModBlocks.TIANSHU_SUPERCOMPUTER_GLASS.get())) {
         return TianshuMultiblockComponent.GLASS;
      } else if (state.m_60713_((Block)ModBlocks.TIANSHU_SUPERCOMPUTER_CONTROLLER.get())) {
         return TianshuMultiblockComponent.CONTROLLER;
      } else if (state.m_60713_((Block)ModBlocks.TIANSHU_SUPERCOMPUTER_PORT.get())) {
         return TianshuMultiblockComponent.PORT;
      } else {
         return state.m_60734_() instanceof TianshuSupercomputingUnitBlock unit ? unit.component() : TianshuMultiblockComponent.OTHER;
      }
   }

   private static CpuMainCoreTier mainTier(TianshuMultiblockComponent component) {
      return switch (component) {
         case MAIN_BASELINE -> CpuMainCoreTier.BASELINE;
         case MAIN_QUANTUM -> CpuMainCoreTier.QUANTUM;
         case MAIN_OVERLOAD -> CpuMainCoreTier.OVERLOAD;
         case MAIN_MULTIDIMENSIONAL -> CpuMainCoreTier.MULTIDIMENSIONAL;
         default -> null;
      };
   }

   private static void trackClosedLoopStorage(TianshuMultiblockComponent component, BlockPos world, List<BlockPos> patternStorages, List<BlockPos> seedStorages) {
      if (component == TianshuMultiblockComponent.CLOSED_LOOP_PATTERN_STORAGE) {
         patternStorages.add(world.m_7949_());
      } else if (component == TianshuMultiblockComponent.CLOSED_LOOP_SEED_STORAGE) {
         seedStorages.add(world.m_7949_());
      }
   }

   public static BlockPos worldPos(BlockPos controllerPos, BlockPos local, Direction orientation) {
      int dx = local.m_123341_() - TianshuMultiblockTemplate.CONTROLLER.m_123341_();
      int dy = local.m_123342_() - TianshuMultiblockTemplate.CONTROLLER.m_123342_();
      int dz = local.m_123343_() - TianshuMultiblockTemplate.CONTROLLER.m_123343_();

      return switch (orientation) {
         case WEST -> controllerPos.m_7918_(dx, dy, dz);
         case NORTH -> controllerPos.m_7918_(-dz, dy, dx);
         case EAST -> controllerPos.m_7918_(-dx, dy, -dz);
         case SOUTH -> controllerPos.m_7918_(dz, dy, -dx);
         default -> throw new IllegalArgumentException("Tianshu controller must face horizontally");
      };
   }

   private static void addOnce(List<TianshuMultiblockScanIssue> issues, TianshuMultiblockScanIssue issue) {
      if (!issues.contains(issue)) {
         issues.add(issue);
      }
   }

   private TianshuMultiblockScanner() {
   }
}
