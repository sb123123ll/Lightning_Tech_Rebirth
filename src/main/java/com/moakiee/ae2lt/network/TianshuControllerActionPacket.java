package com.moakiee.ae2lt.network;

import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.moakiee.ae2lt.blockentity.TianshuSupercomputerControllerBlockEntity;
import com.moakiee.ae2lt.blockentity.TianshuSupercomputerPortBlockEntity;
import com.moakiee.ae2lt.menu.TianshuSupercomputerControllerMenu;
import com.moakiee.thunderbolt.core.crafting.algorithm.menu.CraftingAlgorithmProviderMenu;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record TianshuControllerActionPacket(int token, BlockPos pos, TianshuControllerActionPacket.Action action) {
   public static void encode(TianshuControllerActionPacket packet, FriendlyByteBuf buf) {
      buf.m_130130_(packet.token);
      buf.m_130064_(packet.pos);
      buf.m_130068_(packet.action);
   }

   public static TianshuControllerActionPacket decode(FriendlyByteBuf buf) {
      return new TianshuControllerActionPacket(
         buf.m_130242_(), buf.m_130135_(), (TianshuControllerActionPacket.Action)buf.m_130066_(TianshuControllerActionPacket.Action.class)
      );
   }

   public static void handle(TianshuControllerActionPacket packet, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            packet.handleOnServer(player);
         }
      });
      ctx.setPacketHandled(true);
   }

   private void handleOnServer(ServerPlayer player) {
      if (player.f_36096_ instanceof TianshuSupercomputerControllerMenu menu
         && menu.token() == this.token
         && menu.getBlockPos().equals(this.pos)
         && menu.m_6875_(player)
         && player.m_9236_().m_7702_(this.pos) instanceof TianshuSupercomputerControllerBlockEntity controller) {
         switch (this.action) {
            case AUTO_BUILD:
               controller.autoBuild(player);
               break;
            case OPEN_ALGORITHM_SELECTION:
               BlockPos portPos = controller.getPortPos();
               if (portPos != null
                  && player.m_9236_().m_7702_(portPos) instanceof TianshuSupercomputerPortBlockEntity port
                  && port.getController() == controller) {
                  MenuOpener.open(CraftingAlgorithmProviderMenu.TYPE, player, MenuLocators.forBlockEntity(port));
               } else {
                  player.m_5661_(Component.m_237115_("ae2lt.gui.error.rejected").m_130940_(ChatFormatting.RED), true);
               }
         }

         return;
      }

      player.m_5661_(Component.m_237115_("ae2lt.gui.error.rejected").m_130940_(ChatFormatting.RED), true);
   }

   public static enum Action {
      AUTO_BUILD,
      OPEN_ALGORITHM_SELECTION;
   }
}
