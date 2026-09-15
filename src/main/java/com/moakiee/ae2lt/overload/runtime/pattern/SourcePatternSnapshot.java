package com.moakiee.ae2lt.overload.runtime.pattern;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class SourcePatternSnapshot {
   private static final String TAG_ITEM = "Item";
   private static final String TAG_STACK = "Stack";
   private static final String TAG_CUSTOM_DATA = "CustomData";
   private final ResourceLocation itemId;
   @Nullable
   private final CompoundTag serializedStackTag;
   @Nullable
   private final CompoundTag customDataTag;
   @Nullable
   private String cachedFingerprint;

   public SourcePatternSnapshot(ResourceLocation itemId, @Nullable CompoundTag serializedStackTag, @Nullable CompoundTag customDataTag) {
      this.itemId = Objects.requireNonNull(itemId, "itemId");
      this.serializedStackTag = serializedStackTag == null ? null : serializedStackTag.m_6426_();
      this.customDataTag = customDataTag == null ? null : customDataTag.m_6426_();
   }

   public static SourcePatternSnapshot fromItemStack(ItemStack stack, Provider registries) {
      Objects.requireNonNull(registries, "registries");
      return fromItemStack(stack);
   }

   public static SourcePatternSnapshot fromItemStack(ItemStack stack) {
      Objects.requireNonNull(stack, "stack");
      if (stack.m_41619_()) {
         throw new IllegalArgumentException("source pattern stack must not be empty");
      } else {
         ResourceLocation itemId = BuiltInRegistries.f_257033_.m_7981_(stack.m_41720_());
         CompoundTag stackTag = stack.m_41739_(new CompoundTag());
         return new SourcePatternSnapshot(itemId, stackTag, null);
      }
   }

   public ResourceLocation itemId() {
      return this.itemId;
   }

   public String fingerprint() {
      String cached = this.cachedFingerprint;
      if (cached != null) {
         return cached;
      } else {
         String computed = this.computeFingerprint();
         this.cachedFingerprint = computed;
         return computed;
      }
   }

   private String computeFingerprint() {
      CompoundTag identity = this.toTag();
      if (identity.m_128425_("Stack", 10)) {
         CompoundTag stack = identity.m_128469_("Stack");
         stack.m_128473_("count");
         stack.m_128473_("Count");
      }

      String canonical = canonicalCopy(identity).toString();

      try {
         return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
      } catch (NoSuchAlgorithmException var4) {
         throw new IllegalStateException("SHA-256 is unavailable", var4);
      }
   }

   @Nullable
   public CompoundTag customDataTag() {
      return this.customDataTag == null ? null : this.customDataTag.m_6426_();
   }

   public ItemStack toItemStack(Provider registries) {
      Objects.requireNonNull(registries, "registries");
      return this.toItemStack();
   }

   public ItemStack toItemStack() {
      if (this.serializedStackTag != null && !this.serializedStackTag.m_128456_()) {
         return ItemStack.m_41712_(this.serializedStackTag.m_6426_());
      } else {
         Item item = (Item)BuiltInRegistries.f_257033_.m_7745_(this.itemId);
         ItemStack stack = new ItemStack(item);
         if (this.customDataTag != null && !this.customDataTag.m_128456_()) {
            stack.m_41784_().m_128391_(this.customDataTag.m_6426_());
         }

         return stack;
      }
   }

   public CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();
      tag.m_128359_("Item", this.itemId.toString());
      if (this.serializedStackTag != null && !this.serializedStackTag.m_128456_()) {
         tag.m_128365_("Stack", this.serializedStackTag.m_6426_());
      } else if (this.customDataTag != null && !this.customDataTag.m_128456_()) {
         tag.m_128365_("CustomData", this.customDataTag.m_6426_());
      }

      return tag;
   }

   public static SourcePatternSnapshot fromTag(CompoundTag tag) {
      ResourceLocation itemId;
      if (tag.m_128425_("Item", 8)) {
         itemId = parseRequiredItemId(tag.m_128461_("Item"));
      } else {
         if (!tag.m_128425_("Stack", 10)) {
            throw new IllegalArgumentException("source pattern snapshot is missing an item id");
         }

         itemId = parseRequiredItemId(tag.m_128469_("Stack").m_128461_("id"));
      }

      CompoundTag serializedStack = null;
      if (tag.m_128425_("Stack", 10)) {
         serializedStack = tag.m_128469_("Stack").m_6426_();
      }

      CompoundTag customData = null;
      if (tag.m_128425_("CustomData", 10)) {
         customData = tag.m_128469_("CustomData").m_6426_();
      }

      return new SourcePatternSnapshot(itemId, serializedStack, customData);
   }

   private static ResourceLocation parseRequiredItemId(String value) {
      ResourceLocation itemId = ResourceLocation.m_135820_(value);
      if (itemId == null) {
         throw new IllegalArgumentException("source pattern snapshot has an invalid item id: " + value);
      } else {
         return itemId;
      }
   }

   private static Tag canonicalCopy(Tag source) {
      if (source instanceof CompoundTag compound) {
         CompoundTag result = new CompoundTag();
         compound.m_128431_().stream().sorted().forEach(key -> {
            Tag valuex = compound.m_128423_(key);
            if (valuex != null) {
               result.m_128365_(key, canonicalCopy(valuex));
            }
         });
         return result;
      } else if (!(source instanceof ListTag list)) {
         return source.m_6426_();
      } else {
         ListTag result = new ListTag();

         for (Tag value : list) {
            result.add(canonicalCopy(value));
         }

         return result;
      }
   }
}
