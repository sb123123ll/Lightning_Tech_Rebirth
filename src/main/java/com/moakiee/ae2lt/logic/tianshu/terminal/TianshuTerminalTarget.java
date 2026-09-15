package com.moakiee.ae2lt.logic.tianshu.terminal;

import com.moakiee.ae2lt.blockentity.TianshuSupercomputerPortBlockEntity;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record TianshuTerminalTarget(UUID machineId, ResourceKey<Level> dimension, BlockPos controllerPos) {
   public TianshuTerminalTarget(UUID machineId, ResourceKey<Level> dimension, BlockPos controllerPos) {
      if (machineId != null && dimension != null && controllerPos != null) {
         controllerPos = controllerPos.m_7949_();
         this.machineId = machineId;
         this.dimension = dimension;
         this.controllerPos = controllerPos;
      } else {
         throw new IllegalArgumentException("A terminal target requires a complete machine identity");
      }
   }

   public static TianshuTerminalTarget from(TianshuSupercomputerPortBlockEntity port) {
      if (port != null && port.m_58904_() != null && port.getControllerPos() != null) {
         return new TianshuTerminalTarget(port.getTianshuId(), port.m_58904_().m_46472_(), port.getControllerPos());
      } else {
         throw new IllegalArgumentException("Cannot capture an unbound Tianshu port");
      }
   }

   public boolean matches(TianshuSupercomputerPortBlockEntity port) {
      return port != null
         && port.m_58904_() != null
         && this.machineId.equals(port.getTianshuId())
         && this.dimension.equals(port.m_58904_().m_46472_())
         && this.controllerPos.equals(port.getControllerPos());
   }
}
