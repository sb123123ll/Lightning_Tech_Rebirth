package com.moakiee.ae2lt.me.cell;

import appeng.api.config.CondenserOutput;
import appeng.core.definitions.AEItems;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public enum VoidCellMode implements StringRepresentable {
   TRASH(Items.f_41852_, CondenserOutput.TRASH),
   MATTER_BALLS(AEItems.MATTER_BALL, CondenserOutput.MATTER_BALLS),
   SINGULARITY(AEItems.SINGULARITY, CondenserOutput.SINGULARITY);

   private final ItemLike output;
   private final CondenserOutput condenserOutput;

   private VoidCellMode(ItemLike output, CondenserOutput condenserOutput) {
      this.output = output;
      this.condenserOutput = condenserOutput;
   }

   public ItemLike getOutput() {
      return this.output;
   }

   public int getRequiredPower() {
      return this.condenserOutput.requiredPower;
   }

   public String m_7912_() {
      return this.name();
   }

   public static VoidCellMode fromSerializedName(String name) {
      if (name != null && !name.isBlank()) {
         try {
            return valueOf(name.toUpperCase(Locale.ROOT));
         } catch (IllegalArgumentException var2) {
            return TRASH;
         }
      } else {
         return TRASH;
      }
   }

   public static VoidCellMode fromOrdinal(int ordinal) {
      return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : TRASH;
   }
}
