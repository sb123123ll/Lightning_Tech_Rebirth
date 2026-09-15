package com.moakiee.ae2lt.lightning;

import com.google.gson.JsonObject;
import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;

public record CountedIngredient(Ingredient ingredient, int count) {
   public CountedIngredient(Ingredient ingredient, int count) {
      Objects.requireNonNull(ingredient, "ingredient");
      if (count <= 0) {
         throw new IllegalArgumentException("count must be positive");
      } else {
         this.ingredient = ingredient;
         this.count = count;
      }
   }

   public static CountedIngredient fromJson(JsonObject json) {
      if (!json.has("ingredient")) {
         throw new IllegalArgumentException("Missing required field 'ingredient'");
      } else {
         return new CountedIngredient(Ingredient.m_43917_(json.get("ingredient")), GsonHelper.m_13927_(json, "count"));
      }
   }

   public static CountedIngredient fromNetwork(FriendlyByteBuf buffer) {
      return new CountedIngredient(Ingredient.m_43940_(buffer), buffer.readInt());
   }

   public void toNetwork(FriendlyByteBuf buffer) {
      this.ingredient.m_43923_(buffer);
      buffer.writeInt(this.count);
   }
}
