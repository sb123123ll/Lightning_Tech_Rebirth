package com.moakiee.ae2lt.logic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class WeatherControlHelper {
   private WeatherControlHelper() {
   }

   public static boolean supportsWeather(ServerLevel level) {
      return level.m_6042_().f_223549_() && !level.m_6042_().f_63856_();
   }

   public static int rollDuration(RandomSource random, int minDuration, int maxDuration) {
      int clampedMin = Math.max(1, minDuration);
      int clampedMax = Math.max(clampedMin, maxDuration);
      return clampedMin == clampedMax ? clampedMin : Mth.m_216271_(random, clampedMin, clampedMax);
   }

   public static void setClearWeather(ServerLevel level, int duration) {
      level.m_8606_(Math.max(1, duration), 0, false, false);
   }

   public static void setRainWeather(ServerLevel level, int duration) {
      level.m_8606_(0, Math.max(1, duration), true, false);
   }

   public static void setThunderstorm(ServerLevel level, int duration) {
      level.m_8606_(0, Math.max(1, duration), true, true);
   }
}
