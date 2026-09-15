package com.moakiee.ae2lt.network;

import com.moakiee.ae2lt.celestweave.module.DashSubmodule;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.network.NetworkEvent.Context;

public record DashPacket() {
   public static DashPacket decode(FriendlyByteBuf buf) {
      return new DashPacket();
   }

   public void write(FriendlyByteBuf buf) {
   }

   public static void handle(DashPacket payload, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            DashSubmodule.applyDash(player, player.m_6844_(EquipmentSlot.FEET));
         }
      });
      ctx.setPacketHandled(true);
   }
}
