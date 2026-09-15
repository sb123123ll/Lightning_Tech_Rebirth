package com.moakiee.ae2lt.recipe;

import com.moakiee.ae2lt.registry.ModFumos;
import com.moakiee.ae2lt.registry.ModRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public final class HyperdimensionalPigmeeConversionRecipe extends CustomRecipe {
   public HyperdimensionalPigmeeConversionRecipe(ResourceLocation id, CraftingBookCategory category) {
      super(id, category);
   }

   public boolean matches(CraftingContainer input, Level level) {
      return !findTarget(input).m_41619_();
   }

   public ItemStack assemble(CraftingContainer input, RegistryAccess registryAccess) {
      return PigmeeConversionLogic.createResult(findTarget(input));
   }

   public boolean m_8004_(int width, int height) {
      return width * height >= 2;
   }

   public RecipeSerializer<?> m_7707_() {
      return (RecipeSerializer<?>)ModRecipeTypes.HYPERDIMENSIONAL_PIGMEE_CONVERSION_SERIALIZER.get();
   }

   public NonNullList<ItemStack> getRemainingItems(CraftingContainer input) {
      NonNullList<ItemStack> remaining = NonNullList.m_122780_(input.m_6643_(), ItemStack.f_41583_);

      for (int slot = 0; slot < input.m_6643_(); slot++) {
         if (input.m_8020_(slot).m_150930_((Item)ModFumos.HYPERDIMENSIONAL_PIGMEE_FUMO_ITEM.get())) {
            remaining.set(slot, new ItemStack((ItemLike)ModFumos.PIGMEE_FUMO_ITEM.get()));
            break;
         }
      }

      return remaining;
   }

   private static ItemStack findTarget(CraftingContainer input) {
      ItemStack target = ItemStack.f_41583_;
      boolean foundCatalyst = false;

      for (int slot = 0; slot < input.m_6643_(); slot++) {
         ItemStack stack = input.m_8020_(slot);
         if (!stack.m_41619_()) {
            if (stack.m_150930_((Item)ModFumos.HYPERDIMENSIONAL_PIGMEE_FUMO_ITEM.get())) {
               if (foundCatalyst) {
                  return ItemStack.f_41583_;
               }

               foundCatalyst = true;
            } else {
               if (!target.m_41619_()) {
                  return ItemStack.f_41583_;
               }

               target = stack;
            }
         }
      }

      return foundCatalyst && PigmeeConversionLogic.canConvert(target) ? target : ItemStack.f_41583_;
   }
}
