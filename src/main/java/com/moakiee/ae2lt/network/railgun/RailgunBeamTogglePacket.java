package com.moakiee.ae2lt.network.railgun;

import com.moakiee.ae2lt.item.railgun.ElectromagneticRailgunItem;
import com.moakiee.ae2lt.logic.railgun.RailgunBeamService;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;

public record RailgunBeamTogglePacket(boolean firing, InteractionHand hand) {
   public void write(FriendlyByteBuf buf) {
      buf.writeBoolean(this.firing);
      buf.m_130068_(this.hand);
   }

   public static RailgunBeamTogglePacket decode(FriendlyByteBuf buf) {
      return new RailgunBeamTogglePacket(buf.readBoolean(), (InteractionHand)buf.m_130066_(InteractionHand.class));
   }

   public static void handle(RailgunBeamTogglePacket pkt, Supplier<Context> ctxSup) {
      Context ctx = ctxSup.get();
      ctx.enqueueWork(() -> {
         ServerPlayer p = ctx.getSender();
         if (p != null) {
            ItemStack stack = p.m_21120_(pkt.hand());
            if (!pkt.firing() || stack.m_41720_() instanceof ElectromagneticRailgunItem) {
               RailgunBeamService.setFiring(p, pkt.hand(), pkt.firing());
            }
         }
      });
      ctx.setPacketHandled(true);
   }
}
