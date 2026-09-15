package com.moakiee.ae2lt.network;

import com.moakiee.ae2lt.client.ClientNetworkPacketHandlers;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record PigmeeAssemblerAnimationPacket(BlockPos pos, byte speed, ItemStack output) {
   public static PigmeeAssemblerAnimationPacket decode(FriendlyByteBuf buffer) {
      return new PigmeeAssemblerAnimationPacket(buffer.m_130135_(), buffer.readByte(), buffer.m_130267_());
   }

   public void write(FriendlyByteBuf buffer) {
      buffer.m_130064_(this.pos);
      buffer.writeByte(this.speed);
      buffer.m_130055_(this.output);
   }

   public static void handle(PigmeeAssemblerAnimationPacket packet, Supplier<Context> ctxSup) {
      Context ctx = ctxSup.get();
      ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientNetworkPacketHandlers.handlePigmeeAssemblerAnimation(packet)));
      ctx.setPacketHandled(true);
   }
}
