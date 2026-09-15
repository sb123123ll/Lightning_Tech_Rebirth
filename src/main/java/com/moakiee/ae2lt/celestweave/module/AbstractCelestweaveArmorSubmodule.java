package com.moakiee.ae2lt.celestweave.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractCelestweaveArmorSubmodule implements CelestweaveArmorSubmodule {
   protected static final String OPTIONS_TAG = "Options";

   protected CompoundTag getOptions(ItemStack armor) {
      CompoundTag data = this.getData(armor);
      return !data.m_128425_("Options", 10) ? new CompoundTag() : data.m_128469_("Options").m_6426_();
   }

   protected void setOptions(ItemStack armor, CompoundTag options) {
      CompoundTag data = this.getData(armor);
      if (options != null && !options.m_128456_()) {
         data.m_128365_("Options", options.m_6426_());
      } else {
         data.m_128473_("Options");
      }

      this.setData(armor, data);
   }

   @Override
   public List<CelestweaveArmorSubmoduleConfig> getConfigs(ItemStack armor) {
      ArrayList<CelestweaveArmorSubmoduleConfig> configs = new ArrayList<>();
      CompoundTag options = this.getOptions(armor);
      options.m_128431_().stream().sorted().forEach(key -> {
         Tag tag = options.m_128423_(key);
         if (tag != null) {
            configs.add(this.defaultConfig(key, tag));
         }
      });
      return List.copyOf(configs);
   }

   @Override
   public boolean setConfig(ItemStack armor, String key, @Nullable Tag value) {
      if (key != null && !key.isBlank()) {
         boolean knownConfig = this.getConfigs(armor).stream().anyMatch(config -> config.key().equals(key));
         if (!knownConfig) {
            return false;
         } else {
            CompoundTag options = this.getOptions(armor);
            if (value == null) {
               options.m_128473_(key);
            } else {
               options.m_128365_(key, value.m_6426_());
            }

            this.setOptions(armor, options);
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public List<CelestweaveArmorSubmoduleOptionUi> getConfigUI(ItemStack armor) {
      ArrayList<CelestweaveArmorSubmoduleOptionUi> configUi = new ArrayList<>();

      for (CelestweaveArmorSubmoduleConfig config : this.getConfigs(armor)) {
         CelestweaveArmorSubmoduleConfigChoice choice = config.currentChoice();
         CelestweaveArmorSubmoduleOptionUi.Kind kind;
         if (!config.editable()) {
            kind = CelestweaveArmorSubmoduleOptionUi.Kind.READ_ONLY;
         } else if (isBooleanChoiceSet(config.choices())) {
            kind = CelestweaveArmorSubmoduleOptionUi.Kind.BOOLEAN;
         } else {
            kind = CelestweaveArmorSubmoduleOptionUi.Kind.CYCLE;
         }

         configUi.add(
            new CelestweaveArmorSubmoduleOptionUi(
               config.key(),
               config.label(),
               choice != null ? choice.label() : formatOptionValue(config.value()),
               config.editable(),
               config.hint() != null ? config.hint() : defaultHint(config),
               kind
            )
         );
      }

      return List.copyOf(configUi);
   }

   protected CelestweaveArmorSubmoduleConfig defaultConfig(String key, Tag value) {
      return isBooleanOption(value)
         ? this.config(
            key, Component.m_237113_(formatOptionLabel(key)), value, this.booleanChoices(), Component.m_237115_("ae2lt.celestweave.config.toggle_hint")
         )
         : this.config(Component.m_237113_(formatOptionLabel(key)), key, value);
   }

   protected CelestweaveArmorSubmoduleConfig config(Component label, String key, Tag value) {
      return this.config(key, label, value, List.of(), null);
   }

   protected CelestweaveArmorSubmoduleConfig config(
      String key, Component label, Tag value, List<CelestweaveArmorSubmoduleConfigChoice> choices, @Nullable Component hint
   ) {
      return new CelestweaveArmorSubmoduleConfig(key, label, value, choices, hint);
   }

   protected CelestweaveArmorSubmoduleConfigChoice choice(Tag value, Component label) {
      return new CelestweaveArmorSubmoduleConfigChoice(value, label);
   }

   protected List<CelestweaveArmorSubmoduleConfigChoice> booleanChoices() {
      return List.of(
         this.choice(ByteTag.m_128273_(false), Component.m_237115_("ae2lt.celestweave.screen.flag.no")),
         this.choice(ByteTag.m_128273_(true), Component.m_237115_("ae2lt.celestweave.screen.flag.yes"))
      );
   }

   protected <E extends Enum<E>> CelestweaveArmorSubmoduleConfig enumConfig(String key, Component label, E value, Class<E> enumType) {
      return this.enumConfig(key, label, value, enumType, enumValue -> Component.m_237113_(formatOptionLabel(enumValue.name())));
   }

   protected <E extends Enum<E>> CelestweaveArmorSubmoduleConfig enumConfig(
      String key, Component label, E value, Class<E> enumType, Function<E, Component> valueLabeler
   ) {
      return this.config(
         key, label, StringTag.m_129297_(value.name()), this.enumChoices(enumType, valueLabeler), Component.m_237115_("ae2lt.celestweave.config.cycle_hint")
      );
   }

   protected <E extends Enum<E>> List<CelestweaveArmorSubmoduleConfigChoice> enumChoices(Class<E> enumType) {
      return this.enumChoices(enumType, enumValue -> Component.m_237113_(formatOptionLabel(enumValue.name())));
   }

   protected <E extends Enum<E>> List<CelestweaveArmorSubmoduleConfigChoice> enumChoices(Class<E> enumType, Function<E, Component> valueLabeler) {
      ArrayList<CelestweaveArmorSubmoduleConfigChoice> choices = new ArrayList<>();

      for (E enumValue : (Enum[])enumType.getEnumConstants()) {
         choices.add(this.choice(StringTag.m_129297_(enumValue.name()), valueLabeler.apply(enumValue)));
      }

      return List.copyOf(choices);
   }

   protected static boolean isBooleanOption(Tag tag) {
      if (tag instanceof ByteTag byteTag && (byteTag.m_7063_() == 0 || byteTag.m_7063_() == 1)) {
         return true;
      }

      return false;
   }

   protected static Component formatOptionValue(Tag tag) {
      if (isBooleanOption(tag)) {
         return Component.m_237115_(((ByteTag)tag).m_7063_() != 0 ? "ae2lt.celestweave.screen.flag.yes" : "ae2lt.celestweave.screen.flag.no");
      } else if (tag instanceof NumericTag numericTag) {
         return Component.m_237113_(String.valueOf(numericTag.m_8103_()));
      } else if (tag instanceof StringTag stringTag) {
         return Component.m_237113_(stringTag.m_7916_());
      } else if (tag instanceof ListTag listTag) {
         return Component.m_237113_("[" + listTag.size() + "]");
      } else {
         return tag instanceof CompoundTag compoundTag ? Component.m_237113_("{" + compoundTag.m_128431_().size() + "}") : Component.m_237113_(tag.m_7916_());
      }
   }

   protected static String formatOptionLabel(String key) {
      String normalized = key.replace('_', ' ').replace('-', ' ').trim();
      if (normalized.isEmpty()) {
         return key;
      } else {
         String[] words = normalized.split("\\s+");
         StringBuilder builder = new StringBuilder();

         for (int index = 0; index < words.length; index++) {
            String word = words[index];
            if (!word.isEmpty()) {
               if (builder.length() > 0) {
                  builder.append(' ');
               }

               builder.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
               if (word.length() > 1) {
                  builder.append(word.substring(1).toLowerCase(Locale.ROOT));
               }
            }
         }

         return builder.toString();
      }
   }

   @Nullable
   private static Component defaultHint(CelestweaveArmorSubmoduleConfig config) {
      if (!config.editable()) {
         return null;
      } else {
         return isBooleanChoiceSet(config.choices())
            ? Component.m_237115_("ae2lt.celestweave.config.toggle_hint")
            : Component.m_237115_("ae2lt.celestweave.config.cycle_hint");
      }
   }

   private static boolean isBooleanChoiceSet(List<CelestweaveArmorSubmoduleConfigChoice> choices) {
      if (choices.size() != 2) {
         return false;
      } else {
         boolean hasFalse = false;
         boolean hasTrue = false;

         for (CelestweaveArmorSubmoduleConfigChoice choice : choices) {
            if (choice.value() instanceof ByteTag byteTag && byteTag.m_7063_() == 0) {
               hasFalse = true;
               continue;
            }

            if (choice.value() instanceof ByteTag byteTag && byteTag.m_7063_() == 1) {
               hasTrue = true;
            }
         }

         return hasFalse && hasTrue;
      }
   }
}
