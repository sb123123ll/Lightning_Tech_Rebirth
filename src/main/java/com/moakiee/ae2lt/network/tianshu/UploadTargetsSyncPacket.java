package com.moakiee.ae2lt.network.tianshu;

import appeng.api.implementations.blockentities.PatternContainerGroup;
import com.moakiee.ae2lt.client.ClientNetworkPacketHandlers;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuUploadTargetData;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record UploadTargetsSyncPacket(int containerId, List<TianshuUploadTargetData> targets) {
   public UploadTargetsSyncPacket(int containerId, List<TianshuUploadTargetData> targets) {
      targets = targets == null ? List.of() : List.copyOf(targets);
      TianshuPacketLimits.requireListSize("upload targets", targets.size());
      this.containerId = containerId;
      this.targets = targets;
   }

   public void write(FriendlyByteBuf buf) {
      buf.m_130130_(this.containerId);
      buf.m_130130_(this.targets.size());

      for (TianshuUploadTargetData target : this.targets) {
         target.group().writeToPacket(buf);
         buf.m_130130_(target.providerCount());
         buf.m_130130_(target.availableSlots());
      }
   }

   public static UploadTargetsSyncPacket decode(FriendlyByteBuf buf) {
      int containerId = buf.m_130242_();
      int size = TianshuPacketLimits.requireDecodedListSize("upload targets", buf.m_130242_());
      ArrayList<TianshuUploadTargetData> targets = new ArrayList<>(size);

      for (int i = 0; i < size; i++) {
         targets.add(new TianshuUploadTargetData(PatternContainerGroup.readFromPacket(buf), buf.m_130242_(), buf.m_130242_()));
      }

      return new UploadTargetsSyncPacket(containerId, targets);
   }

   public static void handle(UploadTargetsSyncPacket packet, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientNetworkPacketHandlers.handleUploadTargetsSync(packet)));
      ctx.setPacketHandled(true);
   }
}
