package com.moakiee.ae2lt.logic.research;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public enum RitualGoal {
   HYPERDIMENSIONAL_PIGMEE;

   public String translationKey() {
      return "ae2lt.research_note.goal." + this.name().toLowerCase(Locale.ROOT);
   }

   public Component getDisplayName() {
      return Component.m_237115_(this.translationKey());
   }

   @Nullable
   public static RitualGoal fromName(String name) {
      try {
         return valueOf(name);
      } catch (IllegalArgumentException var2) {
         return null;
      }
   }
}
