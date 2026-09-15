package com.moakiee.ae2lt.logic.energy;

import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class WirelessEnergyAPI {
   private WirelessEnergyAPI() {
   }

   @Nullable
   public static ServerLevel resolveLevel(MinecraftServer server, WirelessEnergyAPI.Target target) {
      ServerLevel level = server.m_129880_(target.dimension());
      return level != null && level.m_46749_(target.pos()) ? level : null;
   }

   @Nullable
   public static Object resolveCapCache(ServerLevel providerLevel, WirelessEnergyAPI.Target target, Supplier<IGrid> gridSupplier) {
      ServerLevel targetLevel = resolveLevel(providerLevel.m_7654_(), target);
      return targetLevel != null ? AppFluxBridge.createCapCache(targetLevel, target.virtualHostPos(), gridSupplier) : null;
   }

   @Nullable
   public static TargetAccess resolveEnergyTarget(@Nullable Object capCache, Direction targetFace) {
      return AppFluxBridge.resolveEnergyTarget(capCache, targetFace.m_122424_());
   }

   public static long simulateTarget(@Nullable TargetAccess target, long maxFe) {
      return AppFluxBridge.simulateTarget(target, maxFe);
   }

   public static long sendToTarget(@Nullable TargetAccess target, IStorageService storage, IActionSource source, long maxFe) {
      return AppFluxBridge.sendToTarget(target, storage, source, maxFe);
   }

   public static long sendToTargetKnownDemand(@Nullable TargetAccess target, IStorageService storage, IActionSource source, long requested) {
      return AppFluxBridge.sendToTargetKnownDemand(target, storage, source, requested);
   }

   public static long sendToTargetRepeatedOptimistic(@Nullable TargetAccess target, BufferedMEStorage buffer, IActionSource source, long maxFe, int maxCalls) {
      return AppFluxBridge.sendToTargetRepeatedOptimistic(target, buffer, source, maxFe, maxCalls);
   }

   public static long distributeBatch(
      ServerLevel providerLevel,
      List<WirelessEnergyAPI.Target> targets,
      BufferedMEStorage buffered,
      BufferedStorageService proxy,
      Supplier<IGrid> gridSupplier,
      IActionSource source
   ) {
      if (AppFluxBridge.canUseEnergyHandler() && AppFluxBridge.FE_KEY != null && !targets.isEmpty()) {
         List<WirelessEnergyAPI.ResolvedTarget> liveTargets = new ArrayList<>(targets.size());

         for (WirelessEnergyAPI.Target target : targets) {
            ServerLevel level = resolveLevel(providerLevel.m_7654_(), target);
            if (level != null) {
               liveTargets.add(new WirelessEnergyAPI.ResolvedTarget(level, target));
            }
         }

         if (liveTargets.isEmpty()) {
            return 0L;
         } else {
            buffered.setCostMultiplier(1);
            long totalPushed = 0L;

            for (WirelessEnergyAPI.ResolvedTarget entry : liveTargets) {
               Object capCache = AppFluxBridge.createCapCache(entry.level(), entry.target().virtualHostPos(), gridSupplier);
               TargetAccess targetx = resolveEnergyTarget(capCache, entry.target().face());
               totalPushed += sendToTarget(targetx, proxy, source, AppFluxBridge.TRANSFER_RATE);
            }

            return totalPushed;
         }
      } else {
         return 0L;
      }
   }

   private static record ResolvedTarget(ServerLevel level, WirelessEnergyAPI.Target target) {
   }

   public static record Target(ResourceKey<Level> dimension, BlockPos pos, Direction face) {
      public Target(ResourceKey<Level> dimension, BlockPos pos, Direction face) {
         pos = pos.m_7949_();
         this.dimension = dimension;
         this.pos = pos;
         this.face = face;
      }

      public BlockPos virtualHostPos() {
         return this.pos.m_121945_(this.face);
      }

      public Direction hostSide() {
         return this.face.m_122424_();
      }
   }
}
