package com.moakiee.ae2lt.logic.railgun;

import java.util.IdentityHashMap;
import java.util.Objects;
import net.minecraft.world.entity.LivingEntity;

final class OverloadExecutionContext {
   private static final ThreadLocal<IdentityHashMap<LivingEntity, Integer>> ACTIVE = new ThreadLocal<>();

   private OverloadExecutionContext() {
   }

   static OverloadExecutionContext.Scope enter(LivingEntity target) {
      Objects.requireNonNull(target, "target");
      IdentityHashMap<LivingEntity, Integer> active = ACTIVE.get();
      if (active == null) {
         active = new IdentityHashMap<>();
         ACTIVE.set(active);
      }

      active.merge(target, Integer.valueOf(1), Integer::sum);
      return new OverloadExecutionContext.Scope(target);
   }

   static boolean contains(LivingEntity target) {
      IdentityHashMap<LivingEntity, Integer> active = ACTIVE.get();
      return active != null && active.containsKey(target);
   }

   private static void exit(LivingEntity target) {
      IdentityHashMap<LivingEntity, Integer> active = ACTIVE.get();
      if (active == null) {
         throw new IllegalStateException("Overload execution scope closed on the wrong thread");
      } else {
         Integer count = active.get(target);
         if (count == null) {
            throw new IllegalStateException("Overload execution scope already closed");
         } else {
            if (count == 1) {
               active.remove(target);
            } else {
               active.put(target, count - 1);
            }

            if (active.isEmpty()) {
               ACTIVE.remove();
            }
         }
      }
   }

   static final class Scope implements AutoCloseable {
      private final LivingEntity target;
      private boolean closed;

      private Scope(LivingEntity target) {
         this.target = target;
      }

      @Override
      public void close() {
         if (!this.closed) {
            this.closed = true;
            OverloadExecutionContext.exit(this.target);
         }
      }
   }
}
