package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.config.AE2LTClientConfig;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import java.util.List;

final class TianshuUploadSourceSelection {
   private TianshuUploadSourceSelection() {
   }

   static TianshuUploadSourceSelection.Selection collect(TianshuPatternEncodingTermMenu menu) {
      TianshuRecipeTransferContext.Snapshot recipeContext = TianshuRecipeTransferContext.snapshotFor(menu);
      return !recipeContext.sourceKey().isBlank()
         ? new TianshuUploadSourceSelection.Selection(recipeContext.sourceKey(), recipeContext.defaultAliases(), true)
         : TianshuUploadSourceSelection.Selection.EMPTY;
   }

   static record Selection(String sourceKey, List<String> defaultAliases, boolean selectFirstDefault) {
      private static final TianshuUploadSourceSelection.Selection EMPTY = new TianshuUploadSourceSelection.Selection("", List.of(), false);

      Selection(String sourceKey, List<String> defaultAliases, boolean selectFirstDefault) {
         sourceKey = sourceKey == null ? "" : sourceKey;
         defaultAliases = defaultAliases == null ? List.of() : List.copyOf(defaultAliases);
         this.sourceKey = sourceKey;
         this.defaultAliases = defaultAliases;
         this.selectFirstDefault = selectFirstDefault;
      }

      String savedAlias() {
         String alias = AE2LTClientConfig.findUploadAlias(this.sourceKey);
         return alias == null ? "" : alias.strip();
      }

      String initialQuery() {
         String saved = this.savedAlias();
         if (!saved.isBlank()) {
            return saved;
         } else {
            return this.selectFirstDefault && !this.defaultAliases.isEmpty() ? this.defaultAliases.get(0) : "";
         }
      }
   }
}
