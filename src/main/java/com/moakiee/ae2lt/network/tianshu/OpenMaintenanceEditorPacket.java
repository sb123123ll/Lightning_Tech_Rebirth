package com.moakiee.ae2lt.network.tianshu;

import appeng.api.stacks.AEKey;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record OpenMaintenanceEditorPacket(int containerId, int selectionRevision, AEKey key) {
   public void write(FriendlyByteBuf buf) {
      buf.m_130130_(this.containerId);
      buf.m_130130_(this.selectionRevision);
      AEKey.writeKey(buf, this.key);
   }

   public static OpenMaintenanceEditorPacket decode(FriendlyByteBuf buf) {
      return new OpenMaintenanceEditorPacket(buf.m_130242_(), buf.m_130242_(), AEKey.readKey(buf));
   }

   public static void handle(OpenMaintenanceEditorPacket packet, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null && player.f_36096_ instanceof TianshuPatternEncodingTermMenu menu && menu.f_38840_ == packet.containerId()) {
            menu.openMaintenanceEditor(packet.selectionRevision(), packet.key());
         }
      });
      ctx.setPacketHandled(true);
   }
}
