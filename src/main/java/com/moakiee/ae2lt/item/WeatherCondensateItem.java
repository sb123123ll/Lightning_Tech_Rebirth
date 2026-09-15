package com.moakiee.ae2lt.item;

import com.moakiee.ae2lt.logic.WeatherControlHelper;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import org.jetbrains.annotations.Nullable;

public class WeatherCondensateItem extends AE2LTItem {
   private final WeatherCondensateItem.Type type;

   public WeatherCondensateItem(WeatherCondensateItem.Type type, Properties properties) {
      super(properties);
      this.type = type;
   }

   public WeatherCondensateItem.Type getType() {
      return this.type;
   }

   @Nullable
   public static WeatherCondensateItem.Type getType(ItemStack stack) {
      return stack.m_41720_() instanceof WeatherCondensateItem condensateItem ? condensateItem.getType() : null;
   }

   @Override
   public void appendHoverText(ItemStack stack, AE2LTItem.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
      tooltipComponents.add(
         Component.m_237110_("item.ae2lt.weather_condensate.target", new Object[]{this.type.getWeatherName()}).m_130940_(ChatFormatting.AQUA)
      );
      tooltipComponents.add(Component.m_237110_("item.ae2lt.weather_condensate.energy", new Object[]{this.type.totalEnergy()}).m_130940_(ChatFormatting.GRAY));
      tooltipComponents.add(
         Component.m_237110_("item.ae2lt.weather_condensate.duration", new Object[]{this.type.minDuration(), this.type.maxDuration()})
            .m_130940_(ChatFormatting.DARK_GRAY)
      );
   }

   public static enum Type implements StringRepresentable {
      CLEAR("clear", "ae2lt.weather.clear"),
      RAIN("rain", "ae2lt.weather.rain"),
      THUNDERSTORM("thunderstorm", "ae2lt.weather.thunderstorm");

      private static final int CLEAR_ENERGY = 500000;
      private static final int RAIN_ENERGY = 1000000;
      private static final int THUNDERSTORM_ENERGY = 8000000;
      private static final int CLEAR_DURATION_MIN = 12000;
      private static final int CLEAR_DURATION_MAX = 180000;
      private static final int RAIN_DURATION_MIN = 12000;
      private static final int RAIN_DURATION_MAX = 24000;
      private static final int THUNDERSTORM_DURATION_MIN = 3600;
      private static final int THUNDERSTORM_DURATION_MAX = 15600;
      private final String serializedName;
      private final String weatherTranslationKey;

      private Type(String serializedName, String weatherTranslationKey) {
         this.serializedName = serializedName;
         this.weatherTranslationKey = weatherTranslationKey;
      }

      public Component getWeatherName() {
         return Component.m_237115_(this.weatherTranslationKey);
      }

      public long totalEnergy() {
         return switch (this) {
            case CLEAR -> 500000L;
            case RAIN -> 1000000L;
            case THUNDERSTORM -> 8000000L;
         };
      }

      public int minDuration() {
         return switch (this) {
            case CLEAR -> 12000;
            case RAIN -> 12000;
            case THUNDERSTORM -> 3600;
         };
      }

      public int maxDuration() {
         return switch (this) {
            case CLEAR -> 180000;
            case RAIN -> 24000;
            case THUNDERSTORM -> 15600;
         };
      }

      public boolean isActive(ServerLevel level) {
         return switch (this) {
            case CLEAR -> !level.m_46471_() && !level.m_46470_();
            case RAIN -> level.m_46471_() && !level.m_46470_();
            case THUNDERSTORM -> level.m_46471_() && level.m_46470_();
         };
      }

      public boolean apply(ServerLevel level, RandomSource random) {
         if (!WeatherControlHelper.supportsWeather(level)) {
            return false;
         } else {
            int duration = WeatherControlHelper.rollDuration(random, this.minDuration(), this.maxDuration());
            switch (this) {
               case CLEAR:
                  WeatherControlHelper.setClearWeather(level, duration);
                  break;
               case RAIN:
                  WeatherControlHelper.setRainWeather(level, duration);
                  break;
               case THUNDERSTORM:
                  WeatherControlHelper.setThunderstorm(level, duration);
            }

            return true;
         }
      }

      public String m_7912_() {
         return this.serializedName;
      }

      public static WeatherCondensateItem.Type fromOrdinal(int ordinal) {
         WeatherCondensateItem.Type[] values = values();
         return ordinal >= 0 && ordinal < values.length ? values[ordinal] : CLEAR;
      }

      @Nullable
      public static WeatherCondensateItem.Type fromName(String name) {
         for (WeatherCondensateItem.Type value : values()) {
            if (value.serializedName.equals(name)) {
               return value;
            }
         }

         return null;
      }
   }
}
