package com.moakiee.ae2lt.event;

import appeng.api.implementations.parts.ICablePart;
import appeng.api.parts.IPartItem;
import appeng.parts.PartPlacement;
import appeng.parts.PartPlacement.Placement;
import appeng.util.InteractionUtil;
import com.moakiee.ae2lt.grid.wirelesslink.WirelessLinkRegistry;
import com.moakiee.ae2lt.item.OverloadedFrequencyCardItem;
import com.moakiee.ae2lt.item.TerminalCardAccess;
import com.moakiee.ae2lt.network.FrequencyCardUsePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt"
)
public final class FrequencyCardAutoConnectHandler {
   private FrequencyCardAutoConnectHandler() {
   }

   @SubscribeEvent
   public static void onBlockPlaced(EntityPlaceEvent event) {
      if (event.getLevel() instanceof ServerLevelAccessor levelAccessor) {
         WirelessLinkRegistry registry = WirelessLinkRegistry.get();
         if (registry != null) {
            ServerLevel level = levelAccessor.m_6018_();
            if (registry.isPotentialLinkTarget(level, event.getPos())) {
               registry.queueClusterTopologyChange(level, event.getPos());
            }

            if (event.getEntity() instanceof ServerPlayer player) {
               registry.queueAutoConnect(player, player.m_9236_().m_46472_(), event.getPos(), null, 2);
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onBlockBreak(BreakEvent event) {
      if (!event.isCanceled() && event.getLevel() instanceof ServerLevelAccessor levelAccessor) {
         WirelessLinkRegistry registry = WirelessLinkRegistry.get();
         if (registry != null) {
            registry.onBlockChanged(levelAccessor.m_6018_(), event.getPos());
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onLeftClickBlock(LeftClickBlock event) {
      if (event.getEntity() instanceof ServerPlayer player && player.m_9236_() instanceof ServerLevel level) {
         WirelessLinkRegistry registry = WirelessLinkRegistry.get();
         if (registry != null && registry.isPotentialLinkTarget(level, event.getPos())) {
            registry.prepareClusterTopologyChange(level, event.getPos());
         }

         return;
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onWrenchDisassemble(RightClickBlock event) {
      if (event.getEntity() instanceof ServerPlayer player
         && player.m_9236_() instanceof ServerLevel level
         && InteractionUtil.isInAlternateUseMode(player)
         && InteractionUtil.canWrenchDisassemble(event.getItemStack())) {
         WirelessLinkRegistry registry = WirelessLinkRegistry.get();
         if (registry != null && registry.isPotentialLinkTarget(level, event.getPos())) {
            registry.prepareClusterTopologyChange(level, event.getPos());
         }

         return;
      }
   }

   @SubscribeEvent
   public static void onTerminalCardRightClick(RightClickBlock event) {
      if (event.getEntity() instanceof ServerPlayer player && !player.m_6144_()) {
         ItemStack held = event.getItemStack();
         if (held.m_41720_() instanceof OverloadedFrequencyCardItem) {
            return;
         }

         ItemStack card = TerminalCardAccess.findCard(held);
         if (!card.m_41619_() && OverloadedFrequencyCardItem.getData(card).isBound()) {
            if (player.m_9236_() instanceof ServerLevel level) {
               BlockPos var9 = event.getPos();
               WirelessLinkRegistry registry = WirelessLinkRegistry.get(level.m_7654_());
               if (registry != null && registry.isPotentialLinkTarget(level, var9)) {
                  event.setCanceled(true);
                  event.setCancellationResult(InteractionResult.SUCCESS);
                  Direction face = event.getFace();
                  if (face == null) {
                     return;
                  }

                  FrequencyCardUsePacket.tryLinkWithCard(player, card, level, var9, face, event.getHitVec().m_82450_());
                  return;
               }

               return;
            }

            return;
         }

         return;
      }
   }

   @SubscribeEvent
   public static void onRightClickBlock(RightClickBlock event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         if (event.getItemStack().m_41720_() instanceof IPartItem<?> partItem) {
            Direction clickedFace = event.getFace();
            if (clickedFace != null) {
               Placement placement = PartPlacement.getPartPlacement(
                  player, player.m_9236_(), event.getItemStack(), event.getPos(), clickedFace, event.getHitVec().m_82450_()
               );
               if (placement != null) {
                  FrequencyCardAutoConnectHandler.PartAutoConnectTarget target = toAutoConnectTarget(event.getPos(), clickedFace, placement);
                  WirelessLinkRegistry registry = WirelessLinkRegistry.get();
                  if (registry != null) {
                     if (player.m_9236_() instanceof ServerLevel level) {
                        registry.queueClusterTopologyChange(level, target.pos());
                     }

                     ResourceLocation partId = IPartItem.getId(partItem);
                     String expectedPartSideName = FrequencyCardAutoConnectTarget.placedPartStorageSideName(
                        target.side().m_122433_(), ICablePart.class.isAssignableFrom(partItem.getPartClass())
                     );
                     registry.queuePartAutoConnect(player, player.m_9236_().m_46472_(), target.pos(), target.side(), partId.toString(), expectedPartSideName, 2);
                  }
               }
            }
         }
      }
   }

   static FrequencyCardAutoConnectHandler.PartAutoConnectTarget toAutoConnectTarget(BlockPos clickedPos, Direction clickedSide, Placement placement) {
      FrequencyCardAutoConnectTarget target = FrequencyCardAutoConnectTarget.fromPartPlacement(
         new FrequencyCardAutoConnectTarget.GridPos(clickedPos.m_123341_(), clickedPos.m_123342_(), clickedPos.m_123343_()),
         clickedSide.m_122433_(),
         new FrequencyCardAutoConnectTarget.GridPos(placement.pos().m_123341_(), placement.pos().m_123342_(), placement.pos().m_123343_()),
         placement.side().m_122433_()
      );
      return new FrequencyCardAutoConnectHandler.PartAutoConnectTarget(
         new BlockPos(target.pos().x(), target.pos().y(), target.pos().z()), Direction.m_122402_(target.sideName())
      );
   }

   static record PartAutoConnectTarget(BlockPos pos, Direction side) {
   }
}
