package com.moakiee.ae2lt.me.key;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import com.moakiee.ae2lt.api.lightning.LightningTier;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class LightningKey extends AEKey {
   public static final ResourceLocation TYPE_ID = new ResourceLocation("ae2lt", "lightning");
   public static final ResourceLocation ID = TYPE_ID;
   public static final ResourceLocation HIGH_VOLTAGE_ID = new ResourceLocation("ae2lt", "high_voltage_lightning");
   public static final ResourceLocation EXTREME_HIGH_VOLTAGE_ID = new ResourceLocation("ae2lt", "extreme_high_voltage_lightning");
   public static final LightningKey HIGH_VOLTAGE = new LightningKey(LightningKey.Tier.HIGH_VOLTAGE);
   public static final LightningKey EXTREME_HIGH_VOLTAGE = new LightningKey(LightningKey.Tier.EXTREME_HIGH_VOLTAGE);
   private final LightningKey.Tier tier;

   private LightningKey(LightningKey.Tier tier) {
      this.tier = tier;
   }

   public static LightningKey of(LightningKey.Tier tier) {
      return tier == LightningKey.Tier.EXTREME_HIGH_VOLTAGE ? EXTREME_HIGH_VOLTAGE : HIGH_VOLTAGE;
   }

   public static LightningKey fromOrdinal(int ordinal) {
      return of(LightningKey.Tier.fromOrdinal(ordinal));
   }

   public static LightningKey of(LightningTier tier) {
      return tier == LightningTier.EXTREME_HIGH_VOLTAGE ? EXTREME_HIGH_VOLTAGE : HIGH_VOLTAGE;
   }

   public static LightningTier toApiTier(LightningKey.Tier tier) {
      return tier == LightningKey.Tier.EXTREME_HIGH_VOLTAGE ? LightningTier.EXTREME_HIGH_VOLTAGE : LightningTier.HIGH_VOLTAGE;
   }

   public static LightningKey.Tier fromApiTier(LightningTier tier) {
      return tier == LightningTier.EXTREME_HIGH_VOLTAGE ? LightningKey.Tier.EXTREME_HIGH_VOLTAGE : LightningKey.Tier.HIGH_VOLTAGE;
   }

   public LightningKey.Tier tier() {
      return this.tier;
   }

   public LightningTier apiTier() {
      return toApiTier(this.tier);
   }

   public AEKeyType getType() {
      return LightningKeyType.INSTANCE;
   }

   public AEKey dropSecondary() {
      return this;
   }

   public CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();
      tag.m_128359_("tier", this.tier.m_7912_());
      return tag;
   }

   public Object getPrimaryKey() {
      return this.tier;
   }

   public ResourceLocation getId() {
      return this.tier == LightningKey.Tier.EXTREME_HIGH_VOLTAGE ? EXTREME_HIGH_VOLTAGE_ID : HIGH_VOLTAGE_ID;
   }

   public void writeToPacket(FriendlyByteBuf data) {
      data.writeByte(this.tier.ordinal());
   }

   protected Component computeDisplayName() {
      return this.tier == LightningKey.Tier.EXTREME_HIGH_VOLTAGE
         ? Component.m_237115_("key.ae2lt.extreme_high_voltage_lightning")
         : Component.m_237115_("key.ae2lt.high_voltage_lightning");
   }

   public void addDrops(long amount, List<ItemStack> drops, Level level, BlockPos pos) {
   }

   public boolean equals(Object obj) {
      if (obj instanceof LightningKey other && this.tier == other.tier) {
         return true;
      }

      return false;
   }

   public int hashCode() {
      return this.tier.hashCode();
   }

   public String toString() {
      return this.getId().toString();
   }

   public static enum Tier implements StringRepresentable {
      HIGH_VOLTAGE("high_voltage"),
      EXTREME_HIGH_VOLTAGE("extreme_high_voltage");

      public static final Codec<LightningKey.Tier> CODEC = StringRepresentable.m_216439_(LightningKey.Tier::values);
      private final String serializedName;

      private Tier(String serializedName) {
         this.serializedName = serializedName;
      }

      public String m_7912_() {
         return this.serializedName;
      }

      public static LightningKey.Tier fromOrdinal(int ordinal) {
         return switch (ordinal) {
            case 1 -> EXTREME_HIGH_VOLTAGE;
            default -> HIGH_VOLTAGE;
         };
      }

      public static LightningKey.Tier fromSerializedName(String serializedName) {
         for (LightningKey.Tier value : values()) {
            if (value.serializedName.equals(serializedName)) {
               return value;
            }
         }

         return HIGH_VOLTAGE;
      }
   }
}
