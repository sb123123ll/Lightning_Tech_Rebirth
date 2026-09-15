package com.moakiee.ae2lt.celestweave.module;

import java.util.Objects;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

public record CelestweaveArmorSubmoduleConfigChoice(Tag value, Component label) {
   public CelestweaveArmorSubmoduleConfigChoice(Tag value, Component label) {
      value = Objects.requireNonNull(value, "value").m_6426_();
      Objects.requireNonNull(label, "label");
      this.value = value;
      this.label = label;
   }

   public boolean matches(Tag candidate) {
      return this.value.equals(candidate);
   }
}
