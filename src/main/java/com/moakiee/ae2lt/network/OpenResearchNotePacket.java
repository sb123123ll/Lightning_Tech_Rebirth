package com.moakiee.ae2lt.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;

public record OpenResearchNotePacket(ItemStack book) {
   public void write(FriendlyByteBuf buf) {
      buf.m_130055_(this.book);
   }

   public static OpenResearchNotePacket decode(FriendlyByteBuf buf) {
      return new OpenResearchNotePacket(buf.m_130267_());
   }

   public static void handle(OpenResearchNotePacket packet, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> ResearchNoteClientBridge.open(packet.book()));
      ctx.setPacketHandled(true);
   }
}
