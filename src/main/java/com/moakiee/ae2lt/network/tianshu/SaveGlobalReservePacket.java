package com.moakiee.ae2lt.network.tianshu;

import appeng.api.stacks.AEKey;
import com.moakiee.ae2lt.logic.tianshu.maintenance.ReservedStockMatchMode;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record SaveGlobalReservePacket(int containerId, int selectionRevision, AEKey key, long amount, ReservedStockMatchMode mode) {
   public void write(FriendlyByteBuf buf) {
      buf.m_130130_(this.containerId);
      buf.m_130130_(this.selectionRevision);
      AEKey.writeKey(buf, this.key);
      buf.writeLong(this.amount);
      buf.m_130068_(this.mode);
   }

   public static SaveGlobalReservePacket decode(FriendlyByteBuf buf) {
      return new SaveGlobalReservePacket(
         buf.m_130242_(), buf.m_130242_(), AEKey.readKey(buf), buf.readLong(), (ReservedStockMatchMode)buf.m_130066_(ReservedStockMatchMode.class)
      );
   }

   public static void handle(SaveGlobalReservePacket packet, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null && player.f_36096_ instanceof TianshuPatternEncodingTermMenu menu && menu.f_38840_ == packet.containerId()) {
            menu.saveGlobalReserve(packet);
         }
      });
      ctx.setPacketHandled(true);
   }
}
