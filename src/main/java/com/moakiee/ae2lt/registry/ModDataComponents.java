package com.moakiee.ae2lt.registry;

import com.moakiee.ae2lt.celestweave.phase.PhaseLockProjectionLink;
import com.moakiee.ae2lt.celestweave.state.CelestweaveModuleContainer;
import com.moakiee.ae2lt.item.railgun.RailgunModuleEntries;
import com.moakiee.ae2lt.item.railgun.RailgunSettings;
import com.moakiee.ae2lt.util.ItemStackTagSupport;
import com.mojang.serialization.Codec;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ModDataComponents {
   public static final String TAG_PREFIX = "ae2lt:";
   public static final ModDataComponents.ComponentKey<RailgunModuleEntries> RAILGUN_MODULE_ENTRIES = new ModDataComponents.ComponentKey<>(
      "ae2lt:railgun_module_entries",
      tag -> RailgunModuleEntries.load(tag.m_128469_("ae2lt:railgun_module_entries")),
      (tag, v) -> tag.m_128365_("ae2lt:railgun_module_entries", v.save())
   );
   public static final ModDataComponents.ComponentKey<RailgunSettings> RAILGUN_SETTINGS = codecKey(
      "ae2lt:railgun_settings", RailgunSettings.CODEC, RailgunSettings.DEFAULT
   );
   public static final ModDataComponents.ComponentKey<ItemStack> RAILGUN_STRUCTURAL_CORE = itemStackKey("ae2lt:railgun_structural_core");
   public static final ModDataComponents.ComponentKey<Long> RAILGUN_CHARGE_TICKS = longKey("ae2lt:railgun_charge_ticks");
   public static final ModDataComponents.ComponentKey<Long> RAILGUN_ENERGY_BUFFER = longKey("ae2lt:railgun_energy_buffer");
   public static final ModDataComponents.ComponentKey<ItemStack> CELESTWEAVE_STRUCTURAL_CORE = itemStackKey("ae2lt:celestweave_structural_core");
   public static final ModDataComponents.ComponentKey<Long> CELESTWEAVE_ENERGY_BUFFER = longKey("ae2lt:celestweave_energy_buffer");
   public static final ModDataComponents.ComponentKey<CelestweaveModuleContainer> CELESTWEAVE_MODULES = new ModDataComponents.ComponentKey<>(
      "ae2lt:celestweave_modules",
      tag -> CelestweaveModuleContainer.load(tag.m_128469_("ae2lt:celestweave_modules")),
      (tag, v) -> tag.m_128365_("ae2lt:celestweave_modules", v.save())
   );
   public static final ModDataComponents.ComponentKey<Boolean> CELESTWEAVE_MODULES_POWERED = boolKey("ae2lt:celestweave_modules_powered");
   public static final ModDataComponents.ComponentKey<PhaseLockProjectionLink> PHASE_LOCK_PROJECTION_LINK = codecKey(
      "ae2lt:phase_lock_projection_link", PhaseLockProjectionLink.CODEC, null
   );
   public static final ModDataComponents.ComponentKey<Long> PHASE_LOCK_ARMOR_UPDATE = longKey("ae2lt:phase_lock_armor_update");

   private ModDataComponents() {
   }

   private static <T> ModDataComponents.ComponentKey<T> codecKey(String key, Codec<T> codec, T def) {
      return new ModDataComponents.ComponentKey<>(
         key,
         tag -> (T)codec.parse(NbtOps.f_128958_, tag.m_128423_(key)).result().orElse(def),
         (tag, v) -> codec.encodeStart(NbtOps.f_128958_, v).result().ifPresent(t -> tag.m_128365_(key, t))
      );
   }

   private static ModDataComponents.ComponentKey<Long> longKey(String key) {
      return new ModDataComponents.ComponentKey<>(key, tag -> tag.m_128454_(key), (tag, v) -> tag.m_128356_(key, v));
   }

   private static ModDataComponents.ComponentKey<Boolean> boolKey(String key) {
      return new ModDataComponents.ComponentKey<>(key, tag -> tag.m_128471_(key), (tag, v) -> tag.m_128379_(key, v));
   }

   private static ModDataComponents.ComponentKey<ItemStack> itemStackKey(String key) {
      return new ModDataComponents.ComponentKey<>(key, tag -> {
         ItemStack stack = ItemStack.m_41712_(tag.m_128469_(key));
         return stack.m_41619_() ? ItemStack.f_41583_ : stack;
      }, (tag, v) -> {
         if (v != null && !v.m_41619_()) {
            tag.m_128365_(key, v.m_41739_(new CompoundTag()));
         } else {
            tag.m_128473_(key);
         }
      });
   }

   public static final class ComponentKey<T> {
      private final String nbtKey;
      private final Function<CompoundTag, T> decode;
      private final BiConsumer<CompoundTag, T> encode;

      private ComponentKey(String nbtKey, Function<CompoundTag, T> decode, BiConsumer<CompoundTag, T> encode) {
         this.nbtKey = nbtKey;
         this.decode = decode;
         this.encode = encode;
      }

      public String nbtKey() {
         return this.nbtKey;
      }

      @Nullable
      public T get(ItemStack stack) {
         CompoundTag tag = stack.m_41783_();
         return tag != null && tag.m_128441_(this.nbtKey) ? this.decode.apply(tag) : null;
      }

      public T getOrDefault(ItemStack stack, T def) {
         T value = this.get(stack);
         return value != null ? value : def;
      }

      public void set(ItemStack stack, T value) {
         if (stack != null && !stack.m_41619_()) {
            ItemStackTagSupport.updateTag(stack, tag -> this.encode.accept(tag, value));
         }
      }

      public void remove(ItemStack stack) {
         if (stack != null && !stack.m_41619_()) {
            ItemStackTagSupport.updateTag(stack, tag -> tag.m_128473_(this.nbtKey));
         }
      }

      public boolean has(ItemStack stack) {
         CompoundTag tag = stack.m_41783_();
         return tag != null && tag.m_128441_(this.nbtKey);
      }
   }
}
