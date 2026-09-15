package com.moakiee.ae2lt.network.tianshu;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.moakiee.ae2lt.client.ClientNetworkPacketHandlers;
import com.moakiee.ae2lt.logic.tianshu.terminal.ClosedLoopResultPage;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record ClosedLoopResultPagePacket(int containerId, ClosedLoopResultPage page) {
   public ClosedLoopResultPagePacket(int containerId, ClosedLoopResultPage page) {
      if (page == null) {
         throw new IllegalArgumentException("missing closed-loop result page");
      } else {
         this.containerId = containerId;
         this.page = page;
      }
   }

   public void write(FriendlyByteBuf buf) {
      buf.m_130130_(this.containerId);
      buf.m_130130_(this.page.revision());
      buf.m_130068_(this.page.kind());
      buf.m_130130_(this.page.offset());
      buf.m_130130_(this.page.total());
      buf.m_130130_(this.page.entries().size());

      for (GenericStack entry : this.page.entries()) {
         AEKey.writeKey(buf, entry.what());
         buf.m_130103_(entry.amount());
      }
   }

   public static ClosedLoopResultPagePacket decode(FriendlyByteBuf buf) {
      int containerId = buf.m_130242_();
      int revision = buf.m_130242_();
      ClosedLoopResultPage.Kind kind = (ClosedLoopResultPage.Kind)buf.m_130066_(ClosedLoopResultPage.Kind.class);
      int offset = buf.m_130242_();
      int total = buf.m_130242_();
      int size = buf.m_130242_();
      if (size >= 0 && size <= 5) {
         ArrayList<GenericStack> entries = new ArrayList<>(size);

         for (int i = 0; i < size; i++) {
            AEKey what = AEKey.readKey(buf);
            long amount = buf.m_130258_();
            if (what == null || amount <= 0L) {
               throw new DecoderException("invalid closed-loop result entry");
            }

            entries.add(new GenericStack(what, amount));
         }

         try {
            return new ClosedLoopResultPagePacket(containerId, new ClosedLoopResultPage(revision, kind, offset, total, entries));
         } catch (IllegalArgumentException var12) {
            throw new DecoderException("invalid closed-loop result page", var12);
         }
      } else {
         throw new DecoderException("invalid closed-loop result page size: " + size);
      }
   }

   public static void handle(ClosedLoopResultPagePacket packet, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientNetworkPacketHandlers.handleClosedLoopResultPage(packet)));
      ctx.setPacketHandled(true);
   }
}
