package com.moakiee.ae2lt.celestweave.module;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public record CelestweaveArmorSubmoduleOptionUi(
   String key, Component label, Component value, boolean editable, @Nullable Component hint, CelestweaveArmorSubmoduleOptionUi.Kind kind
) {
   public CelestweaveArmorSubmoduleOptionUi(String key, Component label, Component value, boolean editable, @Nullable Component hint) {
      this(key, label, value, editable, hint, editable ? CelestweaveArmorSubmoduleOptionUi.Kind.CYCLE : CelestweaveArmorSubmoduleOptionUi.Kind.READ_ONLY);
   }

   public static enum Kind {
      BOOLEAN,
      CYCLE,
      READ_ONLY;
   }
}
