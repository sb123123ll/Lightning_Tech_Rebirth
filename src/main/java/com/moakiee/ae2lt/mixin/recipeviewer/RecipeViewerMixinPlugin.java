package com.moakiee.ae2lt.mixin.recipeviewer;

import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class RecipeViewerMixinPlugin implements IMixinConfigPlugin {
   private boolean jeiPresent;
   private boolean emiPresent;

   public void onLoad(String mixinPackage) {
      LoadingModList mods = LoadingModList.get();
      this.jeiPresent = mods.getModFileById("jei") != null;
      this.emiPresent = mods.getModFileById("emi") != null;
   }

   public String getRefMapperConfig() {
      return null;
   }

   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      if (mixinClassName.contains(".jei.")) {
         return this.jeiPresent;
      } else {
         return mixinClassName.contains(".emi.") ? this.emiPresent : false;
      }
   }

   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   public List<String> getMixins() {
      return null;
   }

   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }
}
