package com.moakiee.ae2lt.integration.jei.compat.ae2jeiintegration;

import appeng.api.integrations.jei.IngredientConverter;
import appeng.api.integrations.jei.IngredientConverters;
import appeng.api.stacks.GenericStack;
import com.moakiee.ae2lt.integration.jei.LightningJeiIngredients;
import com.moakiee.ae2lt.me.key.LightningKey;
import mezz.jei.api.ingredients.IIngredientType;
import org.jetbrains.annotations.Nullable;

public final class AE2JeiIntegrationCompat {
   private static boolean registered;

   private AE2JeiIntegrationCompat() {
   }

   public static synchronized void registerConverter() {
      if (!registered) {
         IngredientConverters.register(AE2JeiIntegrationCompat.LightningKeyIngredientConverter.INSTANCE);
         registered = true;
      }
   }

   private static final class LightningKeyIngredientConverter implements IngredientConverter<LightningKey> {
      private static final AE2JeiIntegrationCompat.LightningKeyIngredientConverter INSTANCE = new AE2JeiIntegrationCompat.LightningKeyIngredientConverter();

      public IIngredientType<LightningKey> getIngredientType() {
         return LightningJeiIngredients.TYPE;
      }

      @Nullable
      public LightningKey getIngredientFromStack(GenericStack stack) {
         return stack.what() instanceof LightningKey lightningKey ? LightningKey.of(lightningKey.tier()) : null;
      }

      @Nullable
      public GenericStack getStackFromIngredient(LightningKey ingredient) {
         return ingredient == null ? null : new GenericStack(LightningKey.of(ingredient.tier()), 1L);
      }
   }
}
