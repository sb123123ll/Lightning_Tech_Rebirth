package com.moakiee.ae2lt.integration.ae2wtlib;

import appeng.api.networking.IGridNode;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import com.moakiee.ae2lt.grid.WirelessFrequencyManager;
import com.moakiee.ae2lt.item.OverloadedFrequencyCardData;
import com.moakiee.ae2lt.item.OverloadedFrequencyCardItem;
import de.mari_023.ae2wtlib.wut.WUTHandler;
import java.util.Objects;
import java.util.function.IntFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class WirelessTerminalFrequencyLink {
   private static final WirelessTerminalFrequencyLink.Resolution NO_FREQUENCY = new WirelessTerminalFrequencyLink.Resolution(
      WirelessTerminalFrequencyLink.RouteKind.NO_FREQUENCY, null
   );
   private static final WirelessTerminalFrequencyLink.Resolution UNAVAILABLE = new WirelessTerminalFrequencyLink.Resolution(
      WirelessTerminalFrequencyLink.RouteKind.UNAVAILABLE, null
   );

   private WirelessTerminalFrequencyLink() {
   }

   @Nullable
   public static IGridNode resolve(Player player, ItemStack terminalStack) {
      return resolveRoute(player, terminalStack).node();
   }

   public static WirelessTerminalFrequencyLink.Resolution resolveRoute(Player player, ItemStack terminalStack) {
      return terminalStack.m_41619_() ? NO_FREQUENCY : resolveRoute(player, UpgradeInventories.forItem(terminalStack, WUTHandler.getUpgradeCardCount()));
   }

   @Nullable
   public static IGridNode resolve(Player player, IUpgradeInventory upgrades) {
      return resolveRoute(player, upgrades).node();
   }

   public static WirelessTerminalFrequencyLink.Resolution resolveRoute(Player player, IUpgradeInventory upgrades) {
      return player.m_9236_() instanceof ServerLevel serverLevel ? resolveRoute(findBoundFrequency(upgrades), frequencyId -> {
         WirelessFrequencyManager manager = WirelessFrequencyManager.get();
         return manager == null ? null : manager.resolveAdvancedNode(frequencyId, serverLevel.m_7654_());
      }) : NO_FREQUENCY;
   }

   static WirelessTerminalFrequencyLink.Resolution resolveRoute(int frequencyId, IntFunction<IGridNode> nodeResolver) {
      if (frequencyId <= 0) {
         return NO_FREQUENCY;
      } else {
         IGridNode node = nodeResolver.apply(frequencyId);
         return node == null ? UNAVAILABLE : new WirelessTerminalFrequencyLink.Resolution(WirelessTerminalFrequencyLink.RouteKind.RESOLVED, node);
      }
   }

   public static boolean isNetworkPowered(@Nullable IGridNode node) {
      return node != null && node.isPowered();
   }

   private static int findBoundFrequency(IUpgradeInventory upgrades) {
      for (int slot = 0; slot < upgrades.size(); slot++) {
         ItemStack stack = upgrades.getStackInSlot(slot);
         if (stack.m_41720_() instanceof OverloadedFrequencyCardItem) {
            OverloadedFrequencyCardData data = OverloadedFrequencyCardItem.getData(stack);
            if (data.isBound()) {
               return data.frequencyId();
            }
         }
      }

      return -1;
   }

   public static record Resolution(WirelessTerminalFrequencyLink.RouteKind kind, @Nullable IGridNode node) {
      public Resolution(WirelessTerminalFrequencyLink.RouteKind kind, @Nullable IGridNode node) {
         Objects.requireNonNull(kind, "kind");
         if (kind == WirelessTerminalFrequencyLink.RouteKind.RESOLVED != (node != null)) {
            throw new IllegalArgumentException("Only a resolved frequency route has a node");
         } else {
            this.kind = kind;
            this.node = node;
         }
      }

      public boolean usesFrequencyRoute() {
         return this.kind == WirelessTerminalFrequencyLink.RouteKind.RESOLVED;
      }

      public boolean isNetworkPowered() {
         return WirelessTerminalFrequencyLink.isNetworkPowered(this.node);
      }
   }

   public static enum RouteKind {
      NO_FREQUENCY,
      UNAVAILABLE,
      RESOLVED;
   }
}
