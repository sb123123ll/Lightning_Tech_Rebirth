package com.moakiee.ae2lt.mixin.recipeviewer.jei;

import com.moakiee.ae2lt.client.JeiRecipeTransferMetadata;
import com.moakiee.ae2lt.client.TianshuDirectUploadClient;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.gui.input.UserInput;
import mezz.jei.gui.recipes.RecipeTransferButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {RecipeTransferButton.class},
   remap = false
)
public abstract class JeiRecipeTransferButtonControllerMixin {
   @Accessor("recipeLayout")
   protected abstract IRecipeLayoutDrawable<?> ae2lt$getRecipeLayout();

   @Accessor("parentContainer")
   protected abstract AbstractContainerMenu ae2lt$getParentContainer();

   @Inject(
      method = {"onMouseClicked(Lmezz/jei/gui/input/UserInput;)Z"},
      at = {@At("HEAD")},
      require = 0
   )
   private void ae2lt$beginRecipeTransferMetadata(UserInput input, CallbackInfoReturnable<Boolean> cir) {
      JeiRecipeTransferMetadata.clear();
      if (input != null && !input.isSimulate()) {
         if (this.ae2lt$getParentContainer() instanceof TianshuPatternEncodingTermMenu tianshuMenu) {
            JeiRecipeTransferMetadata.begin(tianshuMenu, this.ae2lt$getRecipeLayout());
         }
      }
   }

   @Inject(
      method = {"onMouseClicked(Lmezz/jei/gui/input/UserInput;)Z"},
      at = {@At("RETURN")},
      require = 0
   )
   private void ae2lt$clearRecipeTransferMetadata(UserInput input, CallbackInfoReturnable<Boolean> cir) {
      JeiRecipeTransferMetadata.clear();
   }

   @Redirect(
      method = {"onMouseClicked(Lmezz/jei/gui/input/UserInput;)Z"},
      at = @At(
         value = "INVOKE",
         target = "Ljava/lang/Runnable;run()V"
      ),
      require = 0
   )
   private void ae2lt$keepRecipePageForDirectUpload(Runnable onClose) {
      AbstractContainerMenu menu = this.ae2lt$getParentContainer();
      Screen recipeScreen = Minecraft.m_91087_().f_91080_;
      if (menu instanceof TianshuPatternEncodingTermMenu tianshuMenu && TianshuDirectUploadClient.holdRecipeScreen(tianshuMenu, recipeScreen)) {
         return;
      }

      onClose.run();
   }
}
