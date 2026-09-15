package com.moakiee.ae2lt.mixin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class AE2LTMixinConfigPlugin implements IMixinConfigPlugin {
   private static final Map<String, String> REQUIRED_MODS = Map.of("AdvCraftingCpuAccessor", "advanced_ae", "AdvCraftingCpuLogicMixin", "advanced_ae");

   public void onLoad(String mixinPackage) {
   }

   public String getRefMapperConfig() {
      return null;
   }

   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      int separator = mixinClassName.lastIndexOf(46);
      String simpleName = separator >= 0 ? mixinClassName.substring(separator + 1) : mixinClassName;
      String requiredMod = REQUIRED_MODS.get(simpleName);
      return requiredMod == null || isModLoaded(requiredMod);
   }

   private static boolean isModLoaded(String modId) {
      try {
         LoadingModList loadingMods = LoadingModList.get();
         return loadingMods != null && loadingMods.getModFileById(modId) != null;
      } catch (RuntimeException var2) {
         return false;
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
