package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import java.util.LinkedHashSet;
import java.util.List;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

public final class JeiRecipeTransferMetadata {
   private static final ThreadLocal<JeiRecipeTransferMetadata.Snapshot> CURRENT = new ThreadLocal<>();

   private JeiRecipeTransferMetadata() {
   }

   public static void begin(TianshuPatternEncodingTermMenu menu, IRecipeLayoutDrawable<?> recipeLayout) {
      clear();
      if (menu != null && recipeLayout != null && recipeLayout.getRecipeCategory() != null) {
         IRecipeCategory<?> category = recipeLayout.getRecipeCategory();
         RecipeType<?> type = category.getRecipeType();
         String sourceKey = type != null && type.getUid() != null ? type.getUid().toString() : "";
         LinkedHashSet<String> aliases = new LinkedHashSet<>();
         if (!sourceKey.isBlank()) {
            aliases.add(sourceKey);
         }

         if (category.getTitle() != null && !category.getTitle().getString().isBlank()) {
            aliases.add(category.getTitle().getString());
         }

         CURRENT.set(new JeiRecipeTransferMetadata.Snapshot(menu, sourceKey, List.copyOf(aliases)));
      }
   }

   public static JeiRecipeTransferMetadata.Snapshot snapshotFor(TianshuPatternEncodingTermMenu menu) {
      JeiRecipeTransferMetadata.Snapshot snapshot = CURRENT.get();
      return snapshot != null && snapshot.menu() == menu ? snapshot : JeiRecipeTransferMetadata.Snapshot.EMPTY;
   }

   public static void clear() {
      CURRENT.remove();
   }

   public static record Snapshot(TianshuPatternEncodingTermMenu menu, String sourceKey, List<String> defaultAliases) {
      private static final JeiRecipeTransferMetadata.Snapshot EMPTY = new JeiRecipeTransferMetadata.Snapshot(null, "", List.of());
   }
}
