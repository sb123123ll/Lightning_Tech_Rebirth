package com.moakiee.ae2lt.integration.jei;

import com.moakiee.ae2lt.me.key.LightningKey;
import com.mojang.serialization.Codec;
import java.util.List;
import mezz.jei.api.ingredients.IIngredientType;

public final class LightningJeiIngredients {
   public static final IIngredientType<LightningKey> TYPE = new IIngredientType<LightningKey>() {
      public String getUid() {
         return "ae2lt:lightning";
      }

      public Class<? extends LightningKey> getIngredientClass() {
         return LightningKey.class;
      }
   };
   public static final Codec<LightningKey> CODEC = LightningKey.Tier.CODEC.xmap(LightningKey::of, LightningKey::tier);
   public static final List<LightningKey> INGREDIENTS = List.of();
   public static final LightningJeiIngredientHelper HELPER = new LightningJeiIngredientHelper();
   public static final LightningJeiIngredientRenderer RENDERER = new LightningJeiIngredientRenderer();

   private LightningJeiIngredients() {
   }
}
