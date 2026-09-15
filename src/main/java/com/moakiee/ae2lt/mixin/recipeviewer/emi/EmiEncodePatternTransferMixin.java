package com.moakiee.ae2lt.mixin.recipeviewer.emi;

import appeng.api.stacks.GenericStack;
import appeng.integration.modules.emi.EmiEncodePatternHandler;
import appeng.integration.modules.emi.EmiStackHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.moakiee.ae2lt.client.TianshuDirectUploadClient;
import com.moakiee.ae2lt.client.TianshuRecipeTransferContext;
import com.moakiee.ae2lt.client.TianshuUploadAliasRules;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuEncodingMode;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {EmiEncodePatternHandler.class},
   remap = false
)
public abstract class EmiEncodePatternTransferMixin {
   @Unique
   private boolean ae2lt$restoreClosedLoopMode;

   @Inject(
      method = {"transferRecipe(Lappeng/menu/me/items/PatternEncodingTermMenu;Lnet/minecraft/world/item/crafting/Recipe;Ldev/emi/emi/api/recipe/EmiRecipe;Z)Lappeng/integration/modules/emi/AbstractRecipeHandler$Result;"},
      at = {@At("HEAD")},
      require = 1
   )
   private void ae2lt$onTransfer(PatternEncodingTermMenu menu, Recipe<?> recipe, EmiRecipe emiRecipe, boolean doTransfer, CallbackInfoReturnable<Object> cir) {
      if (doTransfer && menu instanceof TianshuPatternEncodingTermMenu tianshuMenu) {
         TianshuRecipeTransferContext.clear(tianshuMenu);
         if (tianshuMenu.tianshuMode == TianshuEncodingMode.CLOSED_LOOP && emiRecipe != null) {
            GenericStack output = (GenericStack)EmiStackHelper.ofOutputs(emiRecipe).stream().findFirst().orElse(null);
            if (output != null && tianshuMenu.markClosedLoopPrimaryOutput(GenericStack.wrapInItemStack(output))) {
               tianshuMenu.autoFillClosedLoop();
               this.ae2lt$restoreClosedLoopMode = true;
            }
         } else {
            tianshuMenu.resetProcessingEncoding();
            String sourceKey = "";
            String recipeId = "";
            ArrayList<String> defaultAliases = new ArrayList<>();
            ArrayList<String> workstationAliases = new ArrayList<>();
            if (emiRecipe != null) {
               EmiRecipeCategory category = emiRecipe.getCategory();
               if (category != null) {
                  sourceKey = category.getId().toString();
                  TianshuRecipeTransferContext.addDefaultAlias(defaultAliases, sourceKey);
                  if (category.getName() != null) {
                     TianshuRecipeTransferContext.addDefaultAlias(defaultAliases, category.getName().getString());
                  }

                  List<EmiIngredient> workstations = EmiApi.getRecipeManager().getWorkstations(category);

                  for (int w = workstations.size() - 1; w >= 0; w--) {
                     EmiIngredient workstation = workstations.get(w);
                     List<EmiStack> workstationStacks = workstation.getEmiStacks();

                     for (int s = workstationStacks.size() - 1; s >= 0; s--) {
                        EmiStack stack = workstationStacks.get(s);
                        if (stack.getName() != null) {
                           TianshuRecipeTransferContext.addDefaultAlias(workstationAliases, stack.getName().getString());
                        }
                     }
                  }
               }

               if (emiRecipe.getId() != null) {
                  recipeId = emiRecipe.getId().toString();
                  TianshuRecipeTransferContext.addDefaultAlias(defaultAliases, TianshuRecipeTransferContext.firstPathSegment(emiRecipe.getId().m_135815_()));
                  TianshuRecipeTransferContext.addDefaultAlias(defaultAliases, TianshuUploadAliasRules.namespaceGlob(emiRecipe.getId().m_135827_()));
               }

               workstationAliases.forEach(alias -> TianshuRecipeTransferContext.addDefaultAlias(defaultAliases, alias));
            }

            if (recipe != null) {
               TianshuRecipeTransferContext.captureVanillaRecipe(tianshuMenu, recipe, sourceKey, defaultAliases);
            } else {
               TianshuRecipeTransferContext.publish(tianshuMenu, sourceKey, recipeId, defaultAliases);
            }
         }
      }
   }

   @Inject(
      method = {"transferRecipe(Lappeng/menu/me/items/PatternEncodingTermMenu;Lnet/minecraft/world/item/crafting/Recipe;Ldev/emi/emi/api/recipe/EmiRecipe;Z)Lappeng/integration/modules/emi/AbstractRecipeHandler$Result;"},
      at = {@At("RETURN")},
      require = 1
   )
   private void ae2lt$restoreClosedLoopMode(
      PatternEncodingTermMenu menu, Recipe<?> recipe, EmiRecipe emiRecipe, boolean doTransfer, CallbackInfoReturnable<Object> cir
   ) {
      if (this.ae2lt$restoreClosedLoopMode && menu instanceof TianshuPatternEncodingTermMenu tianshuMenu) {
         this.ae2lt$restoreClosedLoopMode = false;
         tianshuMenu.setTianshuMode(TianshuEncodingMode.CLOSED_LOOP);
      } else {
         if (doTransfer
            && Screen.m_96639_()
            && menu instanceof TianshuPatternEncodingTermMenu tianshuMenu
            && tianshuMenu.tianshuMode != TianshuEncodingMode.CLOSED_LOOP
            && cir.getReturnValue() instanceof EmiRecipeTransferResultAccessor result
            && result.ae2lt$canCraft()) {
            tianshuMenu.encodeAndUploadDirectly();
            Screen currentScreen = Minecraft.m_91087_().f_91080_;
            if (currentScreen != null && currentScreen != EmiApi.getHandledScreen()) {
               TianshuDirectUploadClient.holdRecipeScreen(tianshuMenu, currentScreen);
            }
         }
      }
   }
}
