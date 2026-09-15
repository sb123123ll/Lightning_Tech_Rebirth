package com.moakiee.ae2lt.celestweave.module;

import java.util.List;
import java.util.Objects;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public record CelestweaveArmorSubmoduleConfig(
   String key, Component label, Tag value, List<CelestweaveArmorSubmoduleConfigChoice> choices, @Nullable Component hint
) {
   public CelestweaveArmorSubmoduleConfig(String key, Component label, Tag value, List<CelestweaveArmorSubmoduleConfigChoice> choices, @Nullable Component hint) {
      Objects.requireNonNull(key, "key");
      Objects.requireNonNull(label, "label");
      value = Objects.requireNonNull(value, "value").m_6426_();
      choices = List.copyOf(Objects.requireNonNullElse(choices, List.of()));
      this.key = key;
      this.label = label;
      this.value = value;
      this.choices = choices;
      this.hint = hint;
   }

   public boolean editable() {
      return !this.choices.isEmpty();
   }

   @Nullable
   public CelestweaveArmorSubmoduleConfigChoice currentChoice() {
      for (CelestweaveArmorSubmoduleConfigChoice choice : this.choices) {
         if (choice.matches(this.value)) {
            return choice;
         }
      }

      return null;
   }

   @Nullable
   public Tag nextValue() {
      return this.stepValue(1);
   }

   @Nullable
   public Tag previousValue() {
      return this.stepValue(-1);
   }

   @Nullable
   private Tag stepValue(int delta) {
      if (this.choices.isEmpty()) {
         return null;
      } else {
         int currentIndex = -1;

         for (int index = 0; index < this.choices.size(); index++) {
            if (this.choices.get(index).matches(this.value)) {
               currentIndex = index;
               break;
            }
         }

         int size = this.choices.size();
         int nextIndex;
         if (currentIndex < 0) {
            nextIndex = delta >= 0 ? 0 : size - 1;
         } else {
            nextIndex = Math.floorMod(currentIndex + delta, size);
         }

         return this.choices.get(nextIndex).value();
      }
   }
}
