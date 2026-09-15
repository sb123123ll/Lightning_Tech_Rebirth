package com.moakiee.ae2lt.network.railgun;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent.Context;

public record RailgunFirePacket(
   UUID shooterId, Vec3 from, Vec3 firstHit, List<Vec3> chainPath, int tier, boolean isMax, boolean soundEnabled, float impactRadius
) {
   public void write(FriendlyByteBuf buf) {
      buf.m_130077_(this.shooterId);
      buf.writeDouble(this.from.f_82479_);
      buf.writeDouble(this.from.f_82480_);
      buf.writeDouble(this.from.f_82481_);
      buf.writeDouble(this.firstHit.f_82479_);
      buf.writeDouble(this.firstHit.f_82480_);
      buf.writeDouble(this.firstHit.f_82481_);
      buf.m_130130_(this.chainPath.size());

      for (Vec3 v : this.chainPath) {
         buf.writeDouble(v.f_82479_);
         buf.writeDouble(v.f_82480_);
         buf.writeDouble(v.f_82481_);
      }

      buf.m_130130_(this.tier);
      buf.writeBoolean(this.isMax);
      buf.writeBoolean(this.soundEnabled);
      buf.writeFloat(this.impactRadius);
   }

   public static RailgunFirePacket decode(FriendlyByteBuf buf) {
      UUID shooterId = buf.m_130259_();
      Vec3 from = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
      Vec3 first = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
      int n = buf.m_130242_();
      List<Vec3> path = new ArrayList<>(n);

      for (int i = 0; i < n; i++) {
         path.add(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
      }

      int tier = buf.m_130242_();
      boolean isMax = buf.readBoolean();
      boolean soundEnabled = buf.readBoolean();
      float impactRadius = buf.readFloat();
      return new RailgunFirePacket(shooterId, from, first, path, tier, isMax, soundEnabled, impactRadius);
   }

   public static void handle(RailgunFirePacket p, Supplier<Context> ctxSup) {
      Context ctx = ctxSup.get();
      ctx.enqueueWork(() -> RailgunClientBridge.fire(p));
      ctx.setPacketHandled(true);
   }
}
