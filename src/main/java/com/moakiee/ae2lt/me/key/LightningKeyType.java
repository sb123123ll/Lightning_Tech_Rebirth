package com.moakiee.ae2lt.me.key;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class LightningKeyType extends AEKeyType {
   public static final LightningKeyType INSTANCE = new LightningKeyType();

   private LightningKeyType() {
      super(LightningKey.TYPE_ID, LightningKey.class, Component.m_237115_("key_type.ae2lt.lightning"));
   }

   public int getAmountPerByte() {
      return 1;
   }

   public int getAmountPerOperation() {
      return 1;
   }

   public int getAmountPerUnit() {
      return 1;
   }

   @Nullable
   public AEKey readFromPacket(FriendlyByteBuf input) {
      return LightningKey.fromOrdinal(input.readByte());
   }

   @Nullable
   public AEKey loadKeyFromTag(CompoundTag tag) {
      return LightningKey.of(LightningKey.Tier.fromSerializedName(tag.m_128461_("tier")));
   }
}
