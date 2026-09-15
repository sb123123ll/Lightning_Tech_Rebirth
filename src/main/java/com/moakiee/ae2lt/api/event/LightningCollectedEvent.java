package com.moakiee.ae2lt.api.event;

import com.moakiee.ae2lt.api.lightning.LightningTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class LightningCollectedEvent extends Event {
   private final ServerLevel level;
   private final BlockPos collectorPos;
   private final LightningTier tier;
   private final boolean naturalWeather;
   private long amount;

   public LightningCollectedEvent(ServerLevel level, BlockPos collectorPos, LightningTier tier, long amount, boolean naturalWeather) {
      this.level = level;
      this.collectorPos = collectorPos.m_7949_();
      this.tier = tier;
      this.amount = Math.max(0L, amount);
      this.naturalWeather = naturalWeather;
   }

   public ServerLevel getLevel() {
      return this.level;
   }

   public BlockPos getCollectorPos() {
      return this.collectorPos;
   }

   public LightningTier getTier() {
      return this.tier;
   }

   public boolean isNaturalWeather() {
      return this.naturalWeather;
   }

   public long getAmount() {
      return this.amount;
   }

   public void setAmount(long amount) {
      this.amount = Math.max(0L, amount);
   }
}
