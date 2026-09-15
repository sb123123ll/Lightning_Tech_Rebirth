package com.moakiee.ae2lt.network;

import com.moakiee.ae2lt.blockentity.MatrixControllerBlockEntity;
import com.moakiee.ae2lt.menu.MatrixControllerMenu;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record MatrixControllerActionPacket(int token, BlockPos pos, MatrixControllerActionPacket.Action action) {
   public static void encode(MatrixControllerActionPacket packet, FriendlyByteBuf buf) {
      buf.m_130130_(packet.token);
      buf.m_130064_(packet.pos);
      buf.m_130068_(packet.action);
   }

   public static MatrixControllerActionPacket decode(FriendlyByteBuf buf) {
      return new MatrixControllerActionPacket(
         buf.m_130242_(), buf.m_130135_(), (MatrixControllerActionPacket.Action)buf.m_130066_(MatrixControllerActionPacket.Action.class)
      );
   }

   public static void handle(MatrixControllerActionPacket packet, Supplier<Context> context) {
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
      if (!(player.f_36096_ instanceof MatrixControllerMenu menu)
         || menu.token() != this.token
         || !menu.getBlockPos().equals(this.pos)
         || !menu.m_6875_(player)) {
         player.m_5661_(Component.m_237115_("ae2lt.gui.error.rejected").m_130940_(ChatFormatting.RED), true);
         return;
      }

      if (player.m_9236_().m_7702_(this.pos) instanceof MatrixControllerBlockEntity controller) {
         controller.performAction(this.action, player);
         return;
      }

      player.m_5661_(Component.m_237115_("ae2lt.gui.error.rejected").m_130940_(ChatFormatting.RED), true);
   }

   public static enum Action {
      AUTO_BUILD,
      UPGRADE_PATTERN_STORAGE;
   }
}
