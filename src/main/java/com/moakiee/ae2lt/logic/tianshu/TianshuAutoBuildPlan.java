package com.moakiee.ae2lt.logic.tianshu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;

public final class TianshuAutoBuildPlan {
   private final List<TianshuAutoBuildPlan.Placement> placements;
   private final List<BlockPos> blocked;

   private TianshuAutoBuildPlan(List<TianshuAutoBuildPlan.Placement> placements, List<BlockPos> blocked) {
      this.placements = List.copyOf(placements);
      this.blocked = List.copyOf(blocked);
   }

   public static TianshuAutoBuildPlan create(TianshuAutoBuildPlan.ComponentResolver resolver) {
      Objects.requireNonNull(resolver);
      ArrayList<TianshuAutoBuildPlan.Placement> placements = new ArrayList<>();
      ArrayList<BlockPos> blocked = new ArrayList<>();
      BlockPos portTarget = selectPortTarget(resolver);

      for (int x = 0; x < 7; x++) {
         for (int y = 0; y < 7; y++) {
            for (int z = 0; z < 7; z++) {
               BlockPos local = new BlockPos(x, y, z);
               TianshuAutoBuildPlan.Target target = targetFor(TianshuMultiblockTemplate.roleAt(local), local, portTarget);
               if (target != null) {
                  TianshuMultiblockComponent component = normalize(resolver.componentAt(local));
                  if (!matchesTarget(component, target)) {
                     if (component != TianshuMultiblockComponent.AIR) {
                        blocked.add(local);
                     } else {
                        placements.add(new TianshuAutoBuildPlan.Placement(local, target));
                     }
                  }
               }
            }
         }
      }

      placements.sort(
         Comparator.<TianshuAutoBuildPlan.Placement>comparingInt(placement -> placement.localPos().m_123342_())
            .thenComparingInt(placement -> horizontalDistanceFromController(placement.localPos()))
            .thenComparingInt(placement -> Math.abs(placement.localPos().m_123343_() - TianshuMultiblockTemplate.CONTROLLER.m_123343_()))
            .thenComparingInt(placement -> placement.localPos().m_123341_())
            .thenComparingInt(placement -> placement.localPos().m_123343_())
      );
      return new TianshuAutoBuildPlan(placements, blocked);
   }

   public List<TianshuAutoBuildPlan.Placement> placements() {
      return this.placements;
   }

   public List<BlockPos> blocked() {
      return this.blocked;
   }

   private static BlockPos selectPortTarget(TianshuAutoBuildPlan.ComponentResolver resolver) {
      TianshuMultiblockComponent lower = normalize(resolver.componentAt(TianshuMultiblockTemplate.LOWER_PORT));
      if (lower == TianshuMultiblockComponent.PORT) {
         return TianshuMultiblockTemplate.LOWER_PORT;
      } else {
         TianshuMultiblockComponent upper = normalize(resolver.componentAt(TianshuMultiblockTemplate.UPPER_PORT));
         if (upper == TianshuMultiblockComponent.PORT) {
            return TianshuMultiblockTemplate.UPPER_PORT;
         } else if (lower.isClosedLoopStorage() && !upper.isClosedLoopStorage()) {
            return TianshuMultiblockTemplate.UPPER_PORT;
         } else if (upper.isClosedLoopStorage() && !lower.isClosedLoopStorage()) {
            return TianshuMultiblockTemplate.LOWER_PORT;
         } else {
            return lower.fillsCoolingPosition() && upper == TianshuMultiblockComponent.AIR
               ? TianshuMultiblockTemplate.UPPER_PORT
               : TianshuMultiblockTemplate.LOWER_PORT;
         }
      }
   }

   private static TianshuAutoBuildPlan.Target targetFor(TianshuMultiblockRole role, BlockPos local, BlockPos portTarget) {
      return switch (role) {
         case CASING -> TianshuAutoBuildPlan.Target.CASING;
         case COOLING -> TianshuAutoBuildPlan.Target.COOLING;
         case GLASS -> TianshuAutoBuildPlan.Target.GLASS;
         case PORT_CANDIDATE -> local.equals(portTarget) ? TianshuAutoBuildPlan.Target.PORT : TianshuAutoBuildPlan.Target.COOLING;
         case CONTROLLER, CORE_RESERVED, IGNORED -> null;
      };
   }

   private static boolean matchesTarget(TianshuMultiblockComponent component, TianshuAutoBuildPlan.Target target) {
      return switch (target) {
         case CASING -> component == TianshuMultiblockComponent.CASING;
         case COOLING -> component.fillsCoolingPosition();
         case GLASS -> component == TianshuMultiblockComponent.GLASS;
         case PORT -> component == TianshuMultiblockComponent.PORT;
      };
   }

   private static int horizontalDistanceFromController(BlockPos pos) {
      return Math.abs(pos.m_123341_() - TianshuMultiblockTemplate.CONTROLLER.m_123341_())
         + Math.abs(pos.m_123343_() - TianshuMultiblockTemplate.CONTROLLER.m_123343_());
   }

   private static TianshuMultiblockComponent normalize(TianshuMultiblockComponent component) {
      return component == null ? TianshuMultiblockComponent.OTHER : component;
   }

   @FunctionalInterface
   public interface ComponentResolver {
      TianshuMultiblockComponent componentAt(BlockPos var1);
   }

   public static record Placement(BlockPos localPos, TianshuAutoBuildPlan.Target target) {
   }

   public static enum Target {
      CASING,
      COOLING,
      GLASS,
      PORT;
   }
}
