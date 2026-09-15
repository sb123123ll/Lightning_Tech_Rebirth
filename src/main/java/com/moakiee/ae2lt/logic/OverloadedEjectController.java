package com.moakiee.ae2lt.logic;

import com.moakiee.ae2lt.blockentity.GhostOutputBlockEntity;
import com.moakiee.ae2lt.blockentity.OverloadedPatternProviderBlockEntity;
import java.lang.ref.WeakReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

final class OverloadedEjectController {
   private final OverloadedPatternProviderBlockEntity host;

   OverloadedEjectController(OverloadedPatternProviderBlockEntity host) {
      this.host = host;
   }

   void refresh() {
      Level level = this.host.m_58904_();
      if (level == null || !level.m_5776_()) {
         this.invalidate(EjectModeRegistry.unregisterAll(this.host, true));
         if (this.host.getReturnMode() == OverloadedPatternProviderBlockEntity.ReturnMode.EJECT
            && this.host.getProviderMode() == OverloadedPatternProviderBlockEntity.ProviderMode.WIRELESS
            && level instanceof ServerLevel providerLevel) {
            for (OverloadedPatternProviderBlockEntity.WirelessConnection connection : this.host.getConnections()) {
               if (connection.dimension().equals(providerLevel.m_46472_())) {
                  ServerLevel targetLevel = providerLevel.m_7654_().m_129880_(connection.dimension());
                  if (targetLevel != null) {
                     BlockPos adjacentPos = connection.pos().m_121945_(connection.boundFace());
                     Direction queryFace = connection.boundFace().m_122424_();
                     GhostOutputBlockEntity ghostBlockEntity = new GhostOutputBlockEntity(adjacentPos);
                     ghostBlockEntity.m_142339_(targetLevel);
                     EjectModeRegistry.register(
                        targetLevel.m_46472_(),
                        adjacentPos.m_121878_(),
                        queryFace,
                        new EjectModeRegistry.EjectEntry(new WeakReference<>(this.host), ghostBlockEntity, providerLevel.m_46472_(), this.host.m_58899_())
                     );
                  }
               }
            }
         }
      }
   }

   void clear() {
      this.invalidate(EjectModeRegistry.unregisterAll(this.host, true));
   }

   private void invalidate(Iterable<EjectModeRegistry.DimPos> positions) {
      if (this.host.m_58904_() instanceof ServerLevel providerLevel) {
         MinecraftServer server = providerLevel.m_7654_();

         for (EjectModeRegistry.DimPos position : positions) {
            ServerLevel targetLevel = server.m_129880_(position.dimension());
            if (targetLevel != null) {
            }
         }
      }
   }
}
