package com.moakiee.ae2lt.client;

public final class TianshuUploadAliasRules {
   private TianshuUploadAliasRules() {
   }

   public static String namespaceGlob(String namespace) {
      return namespace != null && !namespace.isBlank() ? namespace.strip() + ":*" : "";
   }
}
