package com.moakiee.ae2lt.network.railgun;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent.Context;

public record RailgunBeamChainFxPacket(UUID shooterId, Vec3 firstHit, List<Vec3> chainPath, boolean soundEnabled) {
   public void write(FriendlyByteBuf buf) {
      buf.m_130077_(this.shooterId);
      buf.writeDouble(this.firstHit.f_82479_);
      buf.writeDouble(this.firstHit.f_82480_);
      buf.writeDouble(this.firstHit.f_82481_);
      buf.m_130130_(this.chainPath.size());

      for (Vec3 v : this.chainPath) {
         buf.writeDouble(v.f_82479_);
         buf.writeDouble(v.f_82480_);
         buf.writeDouble(v.f_82481_);
      }

      buf.writeBoolean(this.soundEnabled);
   }

   public static RailgunBeamChainFxPacket decode(FriendlyByteBuf buf) {
      UUID id = buf.m_130259_();
      Vec3 first = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
      int n = buf.m_130242_();
      List<Vec3> path = new ArrayList<>(n);

      for (int i = 0; i < n; i++) {
         path.add(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
      }

      boolean soundEnabled = buf.readBoolean();
      return new RailgunBeamChainFxPacket(id, first, path, soundEnabled);
   }

   public static void handle(RailgunBeamChainFxPacket p, Supplier<Context> ctxSup) {
      Context ctx = ctxSup.get();
      ctx.enqueueWork(() -> RailgunClientBridge.beamChainFx(p));
      ctx.setPacketHandled(true);
   }
}
