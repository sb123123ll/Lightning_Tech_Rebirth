package com.moakiee.ae2lt.network.hub;

import com.moakiee.ae2lt.menu.hub.DeviceHubMenu;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record DeviceHubActionPacket(int action, int value) {
   public static final int ACTION_SELECT_TAB = 0;
   public static final int ACTION_TOGGLE_MODULE = 1;
   public static final int ACTION_TOGGLE_TERRAIN = 2;
   public static final int ACTION_TOGGLE_PVP = 3;
   public static final int ACTION_SELECT_MODULE = 4;
   public static final int ACTION_CYCLE_MODULE_CONFIG = 5;
   public static final int ACTION_TOGGLE_SOUND = 6;
   public static final int ACTION_TOGGLE_CHAIN_DAMAGE = 7;
   public static final int ACTION_CYCLE_EXECUTION_MODE = 8;
   public static final int ACTION_TOGGLE_CHARGED_SPLASH = 9;

   public static DeviceHubActionPacket decode(FriendlyByteBuf buf) {
      return new DeviceHubActionPacket(buf.m_130242_(), buf.m_130242_());
   }

   public void write(FriendlyByteBuf buf) {
      buf.m_130130_(this.action);
      buf.m_130130_(this.value);
   }

   public static void handle(DeviceHubActionPacket pkt, Supplier<Context> ctxSup) {
      Context ctx = ctxSup.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            if (player.f_36096_ instanceof DeviceHubMenu menu) {
               menu.setPlayer(player);
               switch (pkt.action()) {
                  case 0:
                     menu.selectTab(pkt.value());
                     break;
                  case 1:
                     menu.toggleModule(pkt.value());
                     break;
                  case 2:
                     menu.toggleRailgunTerrain();
                     break;
                  case 3:
                     menu.toggleRailgunPvp();
                     break;
                  case 4:
                     menu.selectModule(pkt.value());
                     break;
                  case 5:
                     menu.cycleSelectedModuleConfig(pkt.value());
                     break;
                  case 6:
                     menu.toggleRailgunSound();
                     break;
                  case 7:
                     menu.toggleRailgunChainDamage();
                     break;
                  case 8:
                     menu.cycleRailgunExecutionMode();
                     break;
                  case 9:
                     menu.toggleRailgunChargedSplash();
               }
            }
         }
      });
      ctx.setPacketHandled(true);
   }
}
