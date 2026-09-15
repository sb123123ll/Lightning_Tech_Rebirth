package com.moakiee.ae2lt.integration.jei;

import com.moakiee.ae2lt.me.key.LightningKey;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class LightningJeiIngredientHelper implements IIngredientHelper<LightningKey> {
   public IIngredientType<LightningKey> getIngredientType() {
      return LightningJeiIngredients.TYPE;
   }

   public String getDisplayName(LightningKey ingredient) {
      return ingredient.getDisplayName().getString();
   }

   public String getUniqueId(LightningKey ingredient, UidContext context) {
      return ingredient.getId().toString();
   }

   public long getAmount(LightningKey ingredient) {
      return 1L;
   }

   public ResourceLocation getResourceLocation(LightningKey ingredient) {
      return ingredient.getId();
   }

   public ItemStack getCheatItemStack(LightningKey ingredient) {
      return ItemStack.f_41583_;
   }

   public LightningKey copyIngredient(LightningKey ingredient) {
      return LightningKey.of(ingredient.tier());
   }

   public LightningKey normalizeIngredient(LightningKey ingredient) {
      return LightningKey.of(ingredient.tier());
   }

   public String getErrorInfo(@Nullable LightningKey ingredient) {
      return ingredient == null ? "null lightning key" : ingredient.toString();
   }
}
