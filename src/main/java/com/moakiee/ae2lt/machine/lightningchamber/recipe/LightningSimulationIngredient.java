package com.moakiee.ae2lt.machine.lightningchamber.recipe;

import com.google.gson.JsonObject;
import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;

public record LightningSimulationIngredient(Ingredient ingredient, int count) {
   public LightningSimulationIngredient(Ingredient ingredient, int count) {
      Objects.requireNonNull(ingredient, "ingredient");
      if (count <= 0) {
         throw new IllegalArgumentException("count must be positive");
      } else {
         this.ingredient = ingredient;
         this.count = count;
      }
   }

   public static LightningSimulationIngredient fromJson(JsonObject json) {
      if (!json.has("ingredient")) {
         throw new IllegalArgumentException("Missing required field 'ingredient'");
      } else {
         return new LightningSimulationIngredient(Ingredient.m_43917_(json.get("ingredient")), GsonHelper.m_13927_(json, "count"));
      }
   }

   public static LightningSimulationIngredient fromNetwork(FriendlyByteBuf buffer) {
      return new LightningSimulationIngredient(Ingredient.m_43940_(buffer), buffer.readInt());
   }

   public void toNetwork(FriendlyByteBuf buffer) {
      this.ingredient.m_43923_(buffer);
      buffer.writeInt(this.count);
   }
}
