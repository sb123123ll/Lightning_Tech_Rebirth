package com.moakiee.ae2lt.celestweave;

import com.moakiee.ae2lt.celestweave.module.CelestweaveArmorSubmodule;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class ArmorOverloadCombo {
   private static final String TAG_COMBO_UNTIL = "ComboUntil";
   private static final String TAG_COMBO_COUNT = "ComboCount";

   private ArmorOverloadCombo() {
   }

   public static int nextComboIndex(ArmorOverloadCombo.State state, long gameTime) {
      ArmorOverloadCombo.State safeState = state == null ? ArmorOverloadCombo.State.EMPTY : state;
      return safeState.comboUntil() >= gameTime ? saturatingIncrement(Math.max(0, safeState.comboCount())) : 1;
   }

   public static ArmorOverloadCombo.State recordTrigger(ArmorOverloadCombo.State state, long gameTime, int comboWindowTicks, int comboIndex) {
      return new ArmorOverloadCombo.State(saturatingAdd(gameTime, Math.max(1L, (long)comboWindowTicks)), Math.max(1, comboIndex));
   }

   public static long scaledCost(long baseCost, int comboIndex) {
      int safeCombo = Math.max(1, comboIndex);
      if (baseCost <= 0L) {
         return 0L;
      } else {
         return baseCost > Long.MAX_VALUE / (long)safeCombo ? Long.MAX_VALUE : baseCost * (long)safeCombo;
      }
   }

   public static int nextComboIndex(ItemStack armor, CelestweaveArmorSubmodule submodule, long gameTime) {
      return nextComboIndex(readState(CelestweaveArmorState.getSubmoduleData(armor, submodule)), gameTime);
   }

   public static void recordTrigger(ItemStack armor, CelestweaveArmorSubmodule submodule, long gameTime, int comboWindowTicks, int comboIndex) {
      CompoundTag data = CelestweaveArmorState.getSubmoduleData(armor, submodule);
      writeState(data, recordTrigger(readState(data), gameTime, comboWindowTicks, comboIndex));
      CelestweaveArmorState.setSubmoduleData(armor, submodule, data);
   }

   private static ArmorOverloadCombo.State readState(CompoundTag data) {
      if (data == null) {
         return ArmorOverloadCombo.State.EMPTY;
      } else {
         long comboUntil = data.m_128425_("ComboUntil", 4) ? data.m_128454_("ComboUntil") : -1L;
         int comboCount = data.m_128425_("ComboCount", 3) ? data.m_128451_("ComboCount") : 0;
         return new ArmorOverloadCombo.State(comboUntil, comboCount);
      }
   }

   private static void writeState(CompoundTag data, ArmorOverloadCombo.State state) {
      data.m_128356_("ComboUntil", state.comboUntil());
      data.m_128405_("ComboCount", state.comboCount());
   }

   private static int saturatingIncrement(int value) {
      return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : value + 1;
   }

   private static long saturatingAdd(long left, long right) {
      if (right <= 0L) {
         return left;
      } else {
         return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
      }
   }

   public static record State(long comboUntil, int comboCount) {
      public static final ArmorOverloadCombo.State EMPTY = new ArmorOverloadCombo.State(-1L, 0);

      public State(long comboUntil, int comboCount) {
         comboCount = Math.max(0, comboCount);
         this.comboUntil = comboUntil;
         this.comboCount = comboCount;
      }
   }
}
