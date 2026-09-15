package com.moakiee.ae2lt.network;

import java.util.Objects;

public final class ShieldHitFeedbackClientBridge {
   private static ShieldHitFeedbackClientBridge.Hooks hooks = ShieldHitFeedbackClientBridge.Hooks.NOOP;

   private ShieldHitFeedbackClientBridge() {
   }

   public static void install(ShieldHitFeedbackClientBridge.Hooks hooks) {
      ShieldHitFeedbackClientBridge.hooks = Objects.requireNonNull(hooks);
   }

   public static void suppress(ShieldHitFeedbackSuppressionPacket packet) {
      hooks.suppress(packet.entityId());
   }

   public interface Hooks {
      ShieldHitFeedbackClientBridge.Hooks NOOP = new ShieldHitFeedbackClientBridge.Hooks() {
      };

      default void suppress(int entityId) {
      }
   }
}
