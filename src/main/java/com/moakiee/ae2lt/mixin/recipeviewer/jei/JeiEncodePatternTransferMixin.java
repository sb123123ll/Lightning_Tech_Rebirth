package com.moakiee.ae2lt.mixin.recipeviewer.jei;

import appeng.integration.modules.jei.transfer.EncodePatternTransferHandler;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.moakiee.ae2lt.client.JeiRecipeTransferMetadata;
import com.moakiee.ae2lt.client.TianshuRecipeTransferContext;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuEncodingMode;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {EncodePatternTransferHandler.class},
   remap = false
)
public abstract class JeiEncodePatternTransferMixin {
   @Inject(
      method = {"transferRecipe(Lappeng/menu/me/items/PatternEncodingTermMenu;Ljava/lang/Object;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/world/entity/player/Player;ZZ)Lmezz/jei/api/recipe/transfer/IRecipeTransferError;"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 1
   )
   private void ae2lt$onTransfer(
      PatternEncodingTermMenu menu,
      Object recipeBase,
      IRecipeSlotsView slotsView,
      Player player,
      boolean maxTransfer,
      boolean doTransfer,
      CallbackInfoReturnable<IRecipeTransferError> cir
   ) {
      if (doTransfer && menu instanceof TianshuPatternEncodingTermMenu tianshuMenu) {
         TianshuRecipeTransferContext.clear(tianshuMenu);
         if (tianshuMenu.tianshuMode == TianshuEncodingMode.CLOSED_LOOP) {
            ItemStack output = firstDisplayedItemOutput(slotsView);
            if (tianshuMenu.markClosedLoopPrimaryOutput(output)) {
               tianshuMenu.autoFillClosedLoop();
               cir.setReturnValue(null);
            }
         } else {
            tianshuMenu.resetProcessingEncoding();
            JeiRecipeTransferMetadata.Snapshot fallback = JeiRecipeTransferMetadata.snapshotFor(tianshuMenu);
            TianshuRecipeTransferContext.captureVanillaRecipe(tianshuMenu, recipeBase, fallback.sourceKey(), fallback.defaultAliases());
         }
      }
   }

   @Inject(
      method = {"transferRecipe(Lappeng/menu/me/items/PatternEncodingTermMenu;Ljava/lang/Object;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/world/entity/player/Player;ZZ)Lmezz/jei/api/recipe/transfer/IRecipeTransferError;"},
      at = {@At("RETURN")},
      require = 1
   )
   private void ae2lt$encodeAndUploadAfterSuccessfulAltTransfer(
      PatternEncodingTermMenu menu,
      Object recipeBase,
      IRecipeSlotsView slotsView,
      Player player,
      boolean maxTransfer,
      boolean doTransfer,
      CallbackInfoReturnable<IRecipeTransferError> cir
   ) {
      if (doTransfer
         && cir.getReturnValue() == null
         && !ModList.get().isLoaded("emi")
         && Screen.m_96639_()
         && menu instanceof TianshuPatternEncodingTermMenu tianshuMenu
         && tianshuMenu.tianshuMode != TianshuEncodingMode.CLOSED_LOOP) {
         tianshuMenu.encodeAndUploadDirectly();
         return;
      }
   }

   private static ItemStack firstDisplayedItemOutput(IRecipeSlotsView slotsView) {
      return slotsView == null
         ? ItemStack.f_41583_
         : slotsView.getSlotViews(RecipeIngredientRole.OUTPUT)
            .stream()
            .map(slot -> slot.getDisplayedIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.f_41583_))
            .filter(stack -> !stack.m_41619_())
            .findFirst()
            .<ItemStack>map(ItemStack::m_41777_)
            .orElse(ItemStack.f_41583_);
   }
}
