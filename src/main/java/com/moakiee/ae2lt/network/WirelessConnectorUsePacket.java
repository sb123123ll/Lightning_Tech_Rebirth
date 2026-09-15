package com.moakiee.ae2lt.network;

import com.moakiee.ae2lt.api.patternprovider.WirelessPatternProviderHost;
import com.moakiee.ae2lt.block.OverloadedInterfaceBlock;
import com.moakiee.ae2lt.block.OverloadedPowerSupplyBlock;
import com.moakiee.ae2lt.blockentity.OverloadedInterfaceBlockEntity;
import com.moakiee.ae2lt.blockentity.OverloadedPowerSupplyBlockEntity;
import com.moakiee.ae2lt.item.OverloadedWirelessConnectorItem;
import com.moakiee.ae2lt.logic.WirelessConnectionBatchEdit;
import com.moakiee.ae2lt.logic.WirelessConnectionRange;
import com.moakiee.ae2lt.logic.WirelessConnectorTargetHelper;
import com.moakiee.ae2lt.logic.wireless.support.WirelessConnectionRef;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.network.NetworkEvent.Context;

public record WirelessConnectorUsePacket(InteractionHand hand, BlockPos pos, Direction face, boolean contiguous) {
   public static WirelessConnectorUsePacket decode(FriendlyByteBuf buf) {
      return new WirelessConnectorUsePacket(
         (InteractionHand)buf.m_130066_(InteractionHand.class), buf.m_130135_(), (Direction)buf.m_130066_(Direction.class), buf.readBoolean()
      );
   }

   public static void encode(WirelessConnectorUsePacket payload, FriendlyByteBuf buf) {
      buf.m_130068_(payload.hand);
      buf.m_130064_(payload.pos);
      buf.m_130068_(payload.face);
      buf.writeBoolean(payload.contiguous);
   }

   public static void handle(WirelessConnectorUsePacket payload, Supplier<Context> ctxSupplier) {
      Context ctx = ctxSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            payload.handleOnServer(player);
         }
      });
      ctx.setPacketHandled(true);
   }

   private void handleOnServer(ServerPlayer player) {
      Level level = player.m_9236_();
      if (level.m_46749_(this.pos)) {
         ItemStack stack = player.m_21120_(this.hand);
         if (stack.m_41720_() instanceof OverloadedWirelessConnectorItem) {
            double reach = player.m_21133_((Attribute)ForgeMod.BLOCK_REACH.get()) + 1.0;
            if (!(player.m_20275_((double)this.pos.m_123341_() + 0.5, (double)this.pos.m_123342_() + 0.5, (double)this.pos.m_123343_() + 0.5) > reach * reach)) {
               BlockState state = level.m_8055_(this.pos);
               BlockEntity targetBe = level.m_7702_(this.pos);
               boolean isProvider = targetBe instanceof WirelessPatternProviderHost;
               boolean isInterface = state.m_60734_() instanceof OverloadedInterfaceBlock;
               boolean isPowerSupply = state.m_60734_() instanceof OverloadedPowerSupplyBlock;
               boolean isHost = isProvider || isInterface || isPowerSupply;
               boolean isMachine = targetBe != null;
               if (isHost || isMachine) {
                  if (isProvider) {
                     if (targetBe instanceof WirelessPatternProviderHost provider && !provider.isWirelessProvider()) {
                        player.m_5661_(Component.m_237115_("ae2lt.connector.need_wireless").m_130940_(ChatFormatting.GREEN), true);
                        return;
                     }

                     OverloadedWirelessConnectorItem.selectHost(stack, level, this.pos, "provider");
                     player.m_5661_(
                        Component.m_237110_("ae2lt.connector.selected", new Object[]{this.pos.m_123341_(), this.pos.m_123342_(), this.pos.m_123343_()})
                           .m_130940_(ChatFormatting.GREEN),
                        true
                     );
                  } else if (isInterface) {
                     if (targetBe instanceof OverloadedInterfaceBlockEntity iface
                        && iface.getInterfaceMode() != OverloadedInterfaceBlockEntity.InterfaceMode.WIRELESS) {
                        player.m_5661_(Component.m_237115_("ae2lt.connector.need_wireless").m_130940_(ChatFormatting.GREEN), true);
                        return;
                     }

                     OverloadedWirelessConnectorItem.selectHost(stack, level, this.pos, "interface");
                     player.m_5661_(
                        Component.m_237110_(
                              "ae2lt.connector.selected_interface", new Object[]{this.pos.m_123341_(), this.pos.m_123342_(), this.pos.m_123343_()}
                           )
                           .m_130940_(ChatFormatting.GREEN),
                        true
                     );
                  } else if (isPowerSupply) {
                     OverloadedWirelessConnectorItem.selectHost(stack, level, this.pos, "power_supply");
                     player.m_5661_(
                        Component.m_237110_(
                              "ae2lt.connector.selected_power_supply", new Object[]{this.pos.m_123341_(), this.pos.m_123342_(), this.pos.m_123343_()}
                           )
                           .m_130940_(ChatFormatting.GREEN),
                        true
                     );
                  } else if (OverloadedWirelessConnectorItem.hasSelection(stack)) {
                     String hostType = OverloadedWirelessConnectorItem.getSelectedHostType(stack);
                     if (!OverloadedWirelessConnectorItem.isSelectionInCurrentDimension(level, stack)) {
                        player.m_5661_(Component.m_237115_("ae2lt.connector.dimension_mismatch").m_130940_(ChatFormatting.RED), true);
                     } else {
                        if ("provider".equals(hostType)) {
                           this.handleProviderConnection(player, level, stack);
                        } else if ("interface".equals(hostType)) {
                           this.handleInterfaceConnection(player, level, stack);
                        } else if ("power_supply".equals(hostType)) {
                           this.handlePowerSupplyConnection(player, level, stack);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void handleProviderConnection(ServerPlayer player, Level level, ItemStack stack) {
      WirelessPatternProviderHost provider = OverloadedWirelessConnectorItem.getSelectedProvider(level, stack);
      if (provider == null) {
         player.m_5661_(Component.m_237115_("ae2lt.connector.provider_lost").m_130940_(ChatFormatting.GREEN), true);
         OverloadedWirelessConnectorItem.clearSelection(stack);
      } else if (level.m_7702_(this.pos) instanceof WirelessPatternProviderHost) {
         player.m_5661_(Component.m_237115_("ae2lt.connector.cannot_bind_provider").m_130940_(ChatFormatting.RED), true);
      } else {
         Set<BlockPos> targets = WirelessConnectorTargetHelper.collectTargets(level, this.pos, this.contiguous);
         if (targets.isEmpty()) {
            player.m_5661_(Component.m_237115_("ae2lt.connector.not_machine").m_130940_(ChatFormatting.GREEN), true);
         } else {
            ResourceKey<Level> targetDim = level.m_46472_();
            ArrayList<BlockPos> disconnected = new ArrayList<>();
            ArrayList<BlockPos> updated = new ArrayList<>();
            ArrayList<BlockPos> connected = new ArrayList<>();
            int skippedDueToLimit = 0;
            int skippedOutOfRange = 0;
            WirelessConnectionBatchEdit.Plan<BlockPos> plan = WirelessConnectionBatchEdit.planSingleFacePerTarget(
               targets,
               targetDim,
               provider.getConnections(),
               this.face,
               WirelessConnectionRef::dimension,
               WirelessConnectionRef::pos,
               WirelessConnectionRef::boundFace
            );

            for (BlockPos targetPos : plan.disconnect()) {
               if (provider.removeConnection(targetDim, targetPos)) {
                  disconnected.add(targetPos.m_7949_());
               }
            }

            for (BlockPos targetPosx : plan.update()) {
               if (!WirelessConnectionRange.isConnectorLinkInRange(level, provider.getProviderPos(), targetPosx)) {
                  skippedOutOfRange++;
               } else if (provider.addOrUpdateConnection(targetDim, targetPosx, this.face)) {
                  updated.add(targetPosx.m_7949_());
               }
            }

            for (BlockPos targetPosxx : plan.connect()) {
               if (!WirelessConnectionRange.isConnectorLinkInRange(level, provider.getProviderPos(), targetPosxx)) {
                  skippedOutOfRange++;
               } else if (provider.addOrUpdateConnection(targetDim, targetPosxx, this.face)) {
                  connected.add(targetPosxx.m_7949_());
               } else {
                  skippedDueToLimit++;
               }
            }

            this.sendProviderConnectionFeedback(
               player, disconnected, updated, connected, skippedDueToLimit, skippedOutOfRange, provider.getMaxWirelessConnections()
            );
         }
      }
   }

   private void sendProviderConnectionFeedback(
      ServerPlayer player,
      ArrayList<BlockPos> disconnected,
      ArrayList<BlockPos> updated,
      ArrayList<BlockPos> connected,
      int skippedDueToLimit,
      int skippedOutOfRange,
      int maxConnections
   ) {
      if (skippedOutOfRange > 0 && skippedDueToLimit > 0) {
         int changed = disconnected.size() + updated.size() + connected.size();
         if (changed > 0) {
            player.m_5661_(
               Component.m_237110_(
                     "ae2lt.connector.partial_with_range_and_limit",
                     new Object[]{changed, skippedOutOfRange, WirelessConnectionRange.maxConnectorDistance(), skippedDueToLimit, maxConnections}
                  )
                  .m_130940_(ChatFormatting.GREEN),
               true
            );
         } else {
            player.m_5661_(
               Component.m_237110_(
                     "ae2lt.connector.skipped_range_and_limit",
                     new Object[]{skippedOutOfRange, WirelessConnectionRange.maxConnectorDistance(), skippedDueToLimit, maxConnections}
                  )
                  .m_130940_(ChatFormatting.RED),
               true
            );
         }
      } else if (skippedOutOfRange > 0) {
         int changed = disconnected.size() + updated.size() + connected.size();
         if (changed > 0) {
            player.m_5661_(
               Component.m_237110_(
                     "ae2lt.connector.out_of_range_partial", new Object[]{changed, skippedOutOfRange, WirelessConnectionRange.maxConnectorDistance()}
                  )
                  .m_130940_(ChatFormatting.GREEN),
               true
            );
         } else {
            player.m_5661_(
               Component.m_237110_("ae2lt.connector.out_of_range", new Object[]{skippedOutOfRange, WirelessConnectionRange.maxConnectorDistance()})
                  .m_130940_(ChatFormatting.RED),
               true
            );
         }
      } else if (skippedDueToLimit > 0) {
         int changed = disconnected.size() + updated.size() + connected.size();
         if (changed > 0) {
            player.m_5661_(
               Component.m_237110_("ae2lt.connector.provider_partial", new Object[]{changed, skippedDueToLimit, maxConnections})
                  .m_130940_(ChatFormatting.GREEN),
               true
            );
         } else {
            player.m_5661_(
               Component.m_237110_("ae2lt.connector.provider_full", new Object[]{skippedDueToLimit, maxConnections}).m_130940_(ChatFormatting.RED), true
            );
         }
      } else {
         this.sendConnectionFeedback(player, disconnected, updated, connected);
      }
   }

   private void handleInterfaceConnection(ServerPlayer player, Level level, ItemStack stack) {
      OverloadedInterfaceBlockEntity iface = OverloadedWirelessConnectorItem.getSelectedInterface(level, stack);
      if (iface == null) {
         player.m_5661_(Component.m_237115_("ae2lt.connector.provider_lost").m_130940_(ChatFormatting.GREEN), true);
         OverloadedWirelessConnectorItem.clearSelection(stack);
      } else if (level.m_7702_(this.pos) instanceof OverloadedInterfaceBlockEntity) {
         player.m_5661_(Component.m_237115_("ae2lt.connector.cannot_bind_provider").m_130940_(ChatFormatting.RED), true);
      } else {
         Set<BlockPos> targets = WirelessConnectorTargetHelper.collectTargets(level, this.pos, this.contiguous);
         if (targets.isEmpty()) {
            player.m_5661_(Component.m_237115_("ae2lt.connector.not_machine").m_130940_(ChatFormatting.GREEN), true);
         } else {
            ResourceKey<Level> targetDim = level.m_46472_();
            ArrayList<BlockPos> disconnected = new ArrayList<>();
            ArrayList<BlockPos> updated = new ArrayList<>();
            ArrayList<BlockPos> connected = new ArrayList<>();
            int skippedDueToLimit = 0;
            int skippedOutOfRange = 0;
            WirelessConnectionBatchEdit.Plan<BlockPos> plan = WirelessConnectionBatchEdit.planMultiFacePerTarget(
               targets,
               targetDim,
               iface.getConnections(),
               this.face,
               OverloadedInterfaceBlockEntity.WirelessConnection::dimension,
               OverloadedInterfaceBlockEntity.WirelessConnection::pos,
               OverloadedInterfaceBlockEntity.WirelessConnection::boundFace
            );

            for (BlockPos targetPos : plan.disconnect()) {
               if (iface.removeConnection(targetDim, targetPos, this.face)) {
                  disconnected.add(targetPos.m_7949_());
               }
            }

            for (BlockPos targetPosx : plan.connect()) {
               if (!WirelessConnectionRange.isConnectorLinkInRange(level, iface.m_58899_(), targetPosx)) {
                  skippedOutOfRange++;
               } else if (iface.addOrUpdateConnection(new OverloadedInterfaceBlockEntity.WirelessConnection(targetDim, targetPosx, this.face))) {
                  connected.add(targetPosx.m_7949_());
               } else {
                  skippedDueToLimit++;
               }
            }

            this.sendConnectionFeedback(
               player,
               disconnected,
               updated,
               connected,
               skippedDueToLimit,
               skippedOutOfRange,
               "ae2lt.connector.interface_partial",
               "ae2lt.connector.interface_full",
               1024
            );
         }
      }
   }

   private void handlePowerSupplyConnection(ServerPlayer player, Level level, ItemStack stack) {
      OverloadedPowerSupplyBlockEntity powerSupply = OverloadedWirelessConnectorItem.getSelectedPowerSupply(level, stack);
      if (powerSupply == null) {
         player.m_5661_(Component.m_237115_("ae2lt.connector.power_supply_lost").m_130940_(ChatFormatting.GREEN), true);
         OverloadedWirelessConnectorItem.clearSelection(stack);
      } else if (level.m_7702_(this.pos) instanceof OverloadedPowerSupplyBlockEntity) {
         player.m_5661_(Component.m_237115_("ae2lt.connector.cannot_bind_power_supply").m_130940_(ChatFormatting.RED), true);
      } else {
         Set<BlockPos> targets = WirelessConnectorTargetHelper.collectTargets(level, this.pos, this.contiguous);
         if (targets.isEmpty()) {
            player.m_5661_(Component.m_237115_("ae2lt.connector.not_machine").m_130940_(ChatFormatting.GREEN), true);
         } else {
            ResourceKey<Level> targetDim = level.m_46472_();
            WirelessConnectionBatchEdit.Plan<BlockPos> plan = WirelessConnectionBatchEdit.planSingleFacePerTarget(
               targets,
               targetDim,
               powerSupply.getConnections(),
               this.face,
               OverloadedPowerSupplyBlockEntity.WirelessConnection::dimension,
               OverloadedPowerSupplyBlockEntity.WirelessConnection::pos,
               OverloadedPowerSupplyBlockEntity.WirelessConnection::boundFace
            );
            ArrayList<BlockPos> disconnected = new ArrayList<>();
            ArrayList<BlockPos> updated = new ArrayList<>();
            ArrayList<BlockPos> connected = new ArrayList<>();
            int skippedDueToLimit = 0;
            int skippedOutOfRange = 0;

            for (BlockPos targetPos : plan.disconnect()) {
               if (powerSupply.removeConnection(targetDim, targetPos)) {
                  disconnected.add(targetPos.m_7949_());
               }
            }

            for (BlockPos targetPosx : plan.update()) {
               if (!WirelessConnectionRange.isConnectorLinkInRange(level, powerSupply.m_58899_(), targetPosx)) {
                  skippedOutOfRange++;
               } else if (powerSupply.addOrUpdateConnection(targetDim, targetPosx, this.face)) {
                  updated.add(targetPosx.m_7949_());
               }
            }

            for (BlockPos targetPosxx : plan.connect()) {
               if (!WirelessConnectionRange.isConnectorLinkInRange(level, powerSupply.m_58899_(), targetPosxx)) {
                  skippedOutOfRange++;
               } else if (powerSupply.addOrUpdateConnection(targetDim, targetPosxx, this.face)) {
                  connected.add(targetPosxx.m_7949_());
               } else {
                  skippedDueToLimit++;
               }
            }

            this.sendConnectionFeedback(
               player,
               disconnected,
               updated,
               connected,
               skippedDueToLimit,
               skippedOutOfRange,
               "ae2lt.connector.power_supply_partial",
               "ae2lt.connector.power_supply_full",
               64
            );
         }
      }
   }

   private void sendConnectionFeedback(ServerPlayer player, ArrayList<BlockPos> disconnected, ArrayList<BlockPos> updated, ArrayList<BlockPos> connected) {
      this.sendConnectionFeedback(player, disconnected, updated, connected, 0, 0);
   }

   private void sendConnectionFeedback(
      ServerPlayer player,
      ArrayList<BlockPos> disconnected,
      ArrayList<BlockPos> updated,
      ArrayList<BlockPos> connected,
      int skippedDueToLimit,
      int skippedOutOfRange
   ) {
      this.sendConnectionFeedback(
         player,
         disconnected,
         updated,
         connected,
         skippedDueToLimit,
         skippedOutOfRange,
         "ae2lt.connector.power_supply_partial",
         "ae2lt.connector.power_supply_full",
         64
      );
   }

   private void sendConnectionFeedback(
      ServerPlayer player,
      ArrayList<BlockPos> disconnected,
      ArrayList<BlockPos> updated,
      ArrayList<BlockPos> connected,
      int skippedDueToLimit,
      int skippedOutOfRange,
      String limitPartialKey,
      String limitFullKey,
      int maxConnections
   ) {
      if (skippedOutOfRange > 0 && skippedDueToLimit > 0) {
         int changed = disconnected.size() + updated.size() + connected.size();
         if (changed > 0) {
            player.m_5661_(
               Component.m_237110_(
                     "ae2lt.connector.partial_with_range_and_limit",
                     new Object[]{changed, skippedOutOfRange, WirelessConnectionRange.maxConnectorDistance(), skippedDueToLimit, maxConnections}
                  )
                  .m_130940_(ChatFormatting.GREEN),
               true
            );
         } else {
            player.m_5661_(
               Component.m_237110_(
                     "ae2lt.connector.skipped_range_and_limit",
                     new Object[]{skippedOutOfRange, WirelessConnectionRange.maxConnectorDistance(), skippedDueToLimit, maxConnections}
                  )
                  .m_130940_(ChatFormatting.RED),
               true
            );
         }
      } else if (skippedOutOfRange > 0) {
         int changed = disconnected.size() + updated.size() + connected.size();
         if (changed > 0) {
            player.m_5661_(
               Component.m_237110_(
                     "ae2lt.connector.out_of_range_partial", new Object[]{changed, skippedOutOfRange, WirelessConnectionRange.maxConnectorDistance()}
                  )
                  .m_130940_(ChatFormatting.GREEN),
               true
            );
         } else {
            player.m_5661_(
               Component.m_237110_("ae2lt.connector.out_of_range", new Object[]{skippedOutOfRange, WirelessConnectionRange.maxConnectorDistance()})
                  .m_130940_(ChatFormatting.RED),
               true
            );
         }
      } else if (skippedDueToLimit > 0) {
         int changed = disconnected.size() + updated.size() + connected.size();
         if (changed > 0) {
            player.m_5661_(Component.m_237110_(limitPartialKey, new Object[]{changed, skippedDueToLimit, maxConnections}).m_130940_(ChatFormatting.GREEN), true);
         } else {
            player.m_5661_(Component.m_237110_(limitFullKey, new Object[]{skippedDueToLimit, maxConnections}).m_130940_(ChatFormatting.RED), true);
         }
      } else {
         boolean many = disconnected.size() + updated.size() + connected.size() > 1;
         if (many) {
            if (!disconnected.isEmpty()) {
               player.m_5661_(
                  Component.m_237110_("ae2lt.connector.disconnected_many", new Object[]{disconnected.size(), this.face.m_122433_()})
                     .m_130940_(ChatFormatting.GREEN),
                  true
               );
            } else if (!updated.isEmpty()) {
               player.m_5661_(
                  Component.m_237110_("ae2lt.connector.updated_many", new Object[]{updated.size(), this.face.m_122433_()}).m_130940_(ChatFormatting.GREEN),
                  true
               );
            } else if (!connected.isEmpty()) {
               player.m_5661_(
                  Component.m_237110_("ae2lt.connector.connected_many", new Object[]{connected.size(), this.face.m_122433_()}).m_130940_(ChatFormatting.GREEN),
                  true
               );
            }
         } else {
            if (!disconnected.isEmpty()) {
               BlockPos p = disconnected.get(0);
               player.m_5661_(
                  Component.m_237110_("ae2lt.connector.disconnected", new Object[]{p.m_123341_(), p.m_123342_(), p.m_123343_()})
                     .m_130940_(ChatFormatting.GREEN),
                  true
               );
            } else if (!updated.isEmpty()) {
               BlockPos p = updated.get(0);
               player.m_5661_(
                  Component.m_237110_("ae2lt.connector.updated", new Object[]{p.m_123341_(), p.m_123342_(), p.m_123343_(), this.face.m_122433_()})
                     .m_130940_(ChatFormatting.GREEN),
                  true
               );
            } else if (!connected.isEmpty()) {
               BlockPos p = connected.get(0);
               player.m_5661_(
                  Component.m_237110_("ae2lt.connector.connected", new Object[]{p.m_123341_(), p.m_123342_(), p.m_123343_(), this.face.m_122433_()})
                     .m_130940_(ChatFormatting.GREEN),
                  true
               );
            }
         }
      }
   }
}
