package com.moakiee.ae2lt.machine.crystalcatalyzer.recipe;

import com.google.gson.JsonObject;
import com.moakiee.ae2lt.util.RecipeSerializationHelper;
import java.util.Iterator;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public sealed interface CrystalCatalyzerOutput permits CrystalCatalyzerOutput.OfItem, CrystalCatalyzerOutput.OfTag {
   ItemStack resolve();

   int count();

   static CrystalCatalyzerOutput ofItem(ItemStack stack) {
      return new CrystalCatalyzerOutput.OfItem(stack.m_41777_());
   }

   static CrystalCatalyzerOutput ofTag(TagKey<Item> tag, int count) {
      return new CrystalCatalyzerOutput.OfTag(tag, count);
   }

   static CrystalCatalyzerOutput fromJson(JsonObject json) {
      return (CrystalCatalyzerOutput)(json.has("tag")
         ? new CrystalCatalyzerOutput.OfTag(
            TagKey.m_203882_(Registries.f_256913_, ResourceLocation.m_135820_(GsonHelper.m_13906_(json, "tag"))), GsonHelper.m_13824_(json, "count", 1)
         )
         : new CrystalCatalyzerOutput.OfItem(RecipeSerializationHelper.itemStackFromJson(json)));
   }

   static void encode(FriendlyByteBuf buf, CrystalCatalyzerOutput output) {
      if (output instanceof CrystalCatalyzerOutput.OfItem item) {
         buf.writeBoolean(false);
         buf.m_130055_(item.stack());
      } else if (output instanceof CrystalCatalyzerOutput.OfTag tag) {
         buf.writeBoolean(true);
         buf.m_130085_(tag.tag().f_203868_());
         buf.writeInt(tag.count());
      } else {
         throw new IllegalStateException("Unknown crystal catalyzer output type: " + output);
      }
   }

   static CrystalCatalyzerOutput decode(FriendlyByteBuf buf) {
      if (buf.readBoolean()) {
         ResourceLocation tagId = buf.m_130281_();
         int count = buf.readInt();
         return new CrystalCatalyzerOutput.OfTag(TagKey.m_203882_(Registries.f_256913_, tagId), count);
      } else {
         ItemStack stack = buf.m_130267_();
         return new CrystalCatalyzerOutput.OfItem(stack);
      }
   }

   public static record OfItem(ItemStack stack) implements CrystalCatalyzerOutput {
      public OfItem(ItemStack stack) {
         stack = stack.m_41777_();
         if (stack.m_41619_()) {
            throw new IllegalArgumentException("output stack cannot be empty");
         } else {
            this.stack = stack;
         }
      }

      @Override
      public ItemStack resolve() {
         return this.stack.m_41777_();
      }

      @Override
      public int count() {
         return this.stack.m_41613_();
      }
   }

   public static record OfTag(TagKey<Item> tag, int count) implements CrystalCatalyzerOutput {
      public OfTag(TagKey<Item> tag, int count) {
         if (count <= 0) {
            throw new IllegalArgumentException("tag output count must be positive");
         } else {
            this.tag = tag;
            this.count = count;
         }
      }

      @Override
      public ItemStack resolve() {
         Named<Item> holders = (Named<Item>)BuiltInRegistries.f_257033_.m_203431_(this.tag).orElse(null);
         if (holders != null && holders.m_203632_() != 0) {
            Iterator<Holder<Item>> iterator = holders.iterator();
            return !iterator.hasNext() ? ItemStack.f_41583_ : new ItemStack((ItemLike)iterator.next().m_203334_(), this.count);
         } else {
            return ItemStack.f_41583_;
         }
      }
   }
}
