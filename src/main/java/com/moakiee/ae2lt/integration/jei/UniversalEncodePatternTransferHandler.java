package com.moakiee.ae2lt.integration.jei;

import appeng.integration.modules.jei.transfer.EncodePatternTransferHandler;
import appeng.menu.me.items.PatternEncodingTermMenu;
import java.util.Optional;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

final class UniversalEncodePatternTransferHandler<T extends PatternEncodingTermMenu> implements IUniversalRecipeTransferHandler<T> {
   private final EncodePatternTransferHandler<T> delegate;

   UniversalEncodePatternTransferHandler(MenuType<T> menuType, Class<T> menuClass, IRecipeTransferHandlerHelper helper) {
      this.delegate = new EncodePatternTransferHandler(menuType, menuClass, helper);
   }

   public Class<? extends T> getContainerClass() {
      return this.delegate.getContainerClass();
   }

   public Optional<MenuType<T>> getMenuType() {
      return this.delegate.getMenuType();
   }

   @Nullable
   public IRecipeTransferError transferRecipe(T container, Object recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
      return this.delegate.transferRecipe(container, recipe, recipeSlots, player, maxTransfer, doTransfer);
   }
}
