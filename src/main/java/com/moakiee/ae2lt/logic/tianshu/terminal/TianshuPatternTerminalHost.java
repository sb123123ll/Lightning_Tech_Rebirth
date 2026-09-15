package com.moakiee.ae2lt.logic.tianshu.terminal;

import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.helpers.IPatternTerminalMenuHost;
import com.moakiee.ae2lt.blockentity.TianshuSupercomputerPortBlockEntity;
import com.moakiee.ae2lt.me.GridNodeAccess;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public interface TianshuPatternTerminalHost extends IPatternTerminalMenuHost, IActionHost {
   TianshuEncodingMode getTianshuEncodingMode();

   void setTianshuEncodingMode(TianshuEncodingMode var1);

   default boolean isMaintainableView() {
      return false;
   }

   default void setMaintainableView(boolean enabled) {
   }

   default boolean isUniversalWirelessTerminal() {
      return false;
   }

   @Nullable
   default ClosedLoopTerminalDraft getClosedLoopTerminalDraft() {
      return null;
   }

   default void setClosedLoopTerminalDraft(@Nullable ClosedLoopTerminalDraft draft) {
   }

   @Nullable
   default ProcessingPatternTerminalDraft getProcessingPatternTerminalDraft() {
      return null;
   }

   default void setProcessingPatternTerminalDraft(@Nullable ProcessingPatternTerminalDraft draft) {
   }

   default List<TianshuSupercomputerPortBlockEntity> getAvailableTianshu() {
      IGrid grid = GridNodeAccess.getActiveGrid(this.getActionableNode());
      return grid == null
         ? List.of()
         : grid.getActiveMachines(TianshuSupercomputerPortBlockEntity.class)
            .stream()
            .filter(TianshuSupercomputerPortBlockEntity::isLinkActive)
            .sorted(
               Comparator.<TianshuSupercomputerPortBlockEntity, String>comparing(port -> port.m_58904_().m_46472_().m_135782_().toString())
                  .thenComparing(port -> port.getTianshuId().toString())
                  .thenComparingLong(port -> port.getControllerPos().m_121878_())
                  .thenComparingLong(port -> port.m_58899_().m_121878_())
            )
            .toList();
   }

   @Nullable
   default TianshuTerminalTarget selectTianshuTarget() {
      List<TianshuSupercomputerPortBlockEntity> available = this.getAvailableTianshu();
      return available.isEmpty() ? null : TianshuTerminalTarget.from(available.get(0));
   }

   @Nullable
   default TianshuSupercomputerPortBlockEntity resolveTianshuTarget(@Nullable TianshuTerminalTarget target) {
      if (target == null) {
         return null;
      } else {
         for (TianshuSupercomputerPortBlockEntity port : this.getAvailableTianshu()) {
            if (target.matches(port)) {
               return port;
            }
         }

         return null;
      }
   }
}
