package com.moakiee.ae2lt.network.railgun;

import java.util.Objects;

public final class RailgunClientBridge {
   private static RailgunClientBridge.Hooks hooks = RailgunClientBridge.Hooks.NOOP;

   private RailgunClientBridge() {
   }

   public static void install(RailgunClientBridge.Hooks hooks) {
      RailgunClientBridge.hooks = Objects.requireNonNull(hooks);
   }

   public static void fire(RailgunFirePacket packet) {
      hooks.handleFire(packet);
   }

   public static void beamUpdate(RailgunBeamUpdatePacket packet) {
      hooks.handleBeamUpdate(packet);
   }

   public static void beamChainFx(RailgunBeamChainFxPacket packet) {
      hooks.handleBeamChainFx(packet);
   }

   public static void recoil(RailgunRecoilFxPacket packet) {
      hooks.handleRecoil(packet);
   }

   public interface Hooks {
      RailgunClientBridge.Hooks NOOP = new RailgunClientBridge.Hooks() {
      };

      default void handleFire(RailgunFirePacket packet) {
      }

      default void handleBeamUpdate(RailgunBeamUpdatePacket packet) {
      }

      default void handleBeamChainFx(RailgunBeamChainFxPacket packet) {
      }

      default void handleRecoil(RailgunRecoilFxPacket packet) {
      }
   }
}
