package com.moakiee.ae2lt.machine.firmament.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.crafting.Ingredient;

public record FirmamentConversionIngredient(Ingredient ingredient, int count) {
   private static final Codec<Integer> POSITIVE_COUNT_CODEC = Codec.intRange(1, Integer.MAX_VALUE);
   private static final Codec<Ingredient> INGREDIENT_CODEC = ExtraCodecs.f_252400_.xmap(Ingredient::m_43917_, Ingredient::m_43942_);
   public static final MapCodec<FirmamentConversionIngredient> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
               INGREDIENT_CODEC.fieldOf("ingredient").forGetter(FirmamentConversionIngredient::ingredient),
               POSITIVE_COUNT_CODEC.fieldOf("count").forGetter(FirmamentConversionIngredient::count)
            )
            .apply(instance, FirmamentConversionIngredient::new)
   );

   public FirmamentConversionIngredient(Ingredient ingredient, int count) {
      Objects.requireNonNull(ingredient, "ingredient");
      if (count <= 0) {
         throw new IllegalArgumentException("count must be positive");
      } else {
         this.ingredient = ingredient;
         this.count = count;
      }
   }
}
