package com.moakiee.ae2lt.celestweave;

import com.moakiee.ae2lt.celestweave.module.ResistanceSubmodule;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class PhaseShieldChargeWindow {
   public static final int WINDOW_TICKS = 20;
   private static final String TAG_WINDOW_UNTIL = "PhaseShieldChargeUntil";
   private static final String TAG_COVERED_DAMAGE = "PhaseShieldCoveredDamage";

   private PhaseShieldChargeWindow() {
   }

   public static PhaseShieldChargeWindow.Quote quote(ItemStack armor, long gameTime, float preventedDamage) {
      return quote(readState(armor), gameTime, (double)preventedDamage);
   }

   static PhaseShieldChargeWindow.Quote quote(PhaseShieldChargeWindow.State state, long gameTime, double preventedDamage) {
      PhaseShieldChargeWindow.State safeState = state == null ? PhaseShieldChargeWindow.State.EMPTY : state;
      double damage = normalizeDamage(preventedDamage);
      if (damage <= 0.0) {
         return new PhaseShieldChargeWindow.Quote(0L, 0L, safeState);
      } else {
         boolean activeWindow = safeState.windowUntil() > gameTime;
         double previousCovered = activeWindow ? safeState.coveredDamage() : 0.0;
         double nextCovered = Math.max(previousCovered, damage);
         long windowUntil = activeWindow ? safeState.windowUntil() : saturatingAdd(gameTime, 20L);
         long previousFe = totalCost(previousCovered, 20000L, 2000000000L);
         long nextFe = totalCost(nextCovered, 20000L, 2000000000L);
         long previousEhv = totalCost(previousCovered, 1L, 512L);
         long nextEhv = totalCost(nextCovered, 1L, 512L);
         return new PhaseShieldChargeWindow.Quote(
            Math.max(0L, nextFe - previousFe), Math.max(0L, nextEhv - previousEhv), new PhaseShieldChargeWindow.State(windowUntil, nextCovered)
         );
      }
   }

   public static void record(ItemStack armor, PhaseShieldChargeWindow.Quote quote) {
      if (armor != null && !armor.m_41619_() && quote != null) {
         CompoundTag data = CelestweaveArmorState.getSubmoduleData(armor, ResistanceSubmodule.T2);
         data.m_128356_("PhaseShieldChargeUntil", quote.nextState().windowUntil());
         data.m_128347_("PhaseShieldCoveredDamage", quote.nextState().coveredDamage());
         CelestweaveArmorState.setSubmoduleData(armor, ResistanceSubmodule.T2, data);
      }
   }

   private static PhaseShieldChargeWindow.State readState(ItemStack armor) {
      if (armor != null && !armor.m_41619_()) {
         CompoundTag data = CelestweaveArmorState.getSubmoduleData(armor, ResistanceSubmodule.T2);
         long windowUntil = data.m_128425_("PhaseShieldChargeUntil", 4) ? data.m_128454_("PhaseShieldChargeUntil") : Long.MIN_VALUE;
         double coveredDamage = data.m_128425_("PhaseShieldCoveredDamage", 6) ? data.m_128459_("PhaseShieldCoveredDamage") : 0.0;
         return new PhaseShieldChargeWindow.State(windowUntil, coveredDamage);
      } else {
         return PhaseShieldChargeWindow.State.EMPTY;
      }
   }

   private static long totalCost(double damage, long costPerDamage, long cap) {
      if (!(damage <= 0.0) && costPerDamage > 0L && cap > 0L) {
         double rawCost = damage * (double)costPerDamage;
         return Double.isFinite(rawCost) && !(rawCost >= (double)cap) ? Math.min(cap, (long)Math.ceil(rawCost)) : cap;
      } else {
         return 0L;
      }
   }

   private static double normalizeDamage(double damage) {
      return !Double.isNaN(damage) && !(damage <= 0.0) ? Math.min(Float.MAX_VALUE, damage) : 0.0;
   }

   private static long saturatingAdd(long left, long right) {
      if (right <= 0L) {
         return left;
      } else {
         return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
      }
   }

   public static record Quote(long feCost, long ehvCost, PhaseShieldChargeWindow.State nextState) {
      public Quote(long feCost, long ehvCost, PhaseShieldChargeWindow.State nextState) {
         feCost = Math.max(0L, feCost);
         ehvCost = Math.max(0L, ehvCost);
         nextState = nextState == null ? PhaseShieldChargeWindow.State.EMPTY : nextState;
         this.feCost = feCost;
         this.ehvCost = ehvCost;
         this.nextState = nextState;
      }
   }

   static record State(long windowUntil, double coveredDamage) {
      static final PhaseShieldChargeWindow.State EMPTY = new PhaseShieldChargeWindow.State(Long.MIN_VALUE, 0.0);

      State(long windowUntil, double coveredDamage) {
         coveredDamage = PhaseShieldChargeWindow.normalizeDamage(coveredDamage);
         this.windowUntil = windowUntil;
         this.coveredDamage = coveredDamage;
      }
   }
}
