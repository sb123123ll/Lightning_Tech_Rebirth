package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

public final class TianshuRecipeTransferContext {
   private static final int ENCODING_RESULT_GRACE_TICKS = 5;
   private static WeakReference<TianshuPatternEncodingTermMenu> owner = new WeakReference<>(null);
   private static TianshuRecipeTransferContext.Snapshot snapshot = TianshuRecipeTransferContext.Snapshot.EMPTY;
   private static ItemStack encodingSourcePattern = ItemStack.f_41583_;
   private static boolean encodingPending;
   private static boolean encodingAckReceived;
   private static int encodingBindingDeadline;

   private TianshuRecipeTransferContext() {
   }

   public static void captureVanillaRecipe(TianshuPatternEncodingTermMenu menu, Object recipeBase) {
      captureVanillaRecipe(menu, recipeBase, List.of());
   }

   public static void captureVanillaRecipe(TianshuPatternEncodingTermMenu menu, Object recipeBase, Iterable<String> additionalAliases) {
      captureVanillaRecipe(menu, recipeBase, "", additionalAliases);
   }

   public static void captureVanillaRecipe(TianshuPatternEncodingTermMenu menu, Object recipeBase, String fallbackSourceKey, Iterable<String> additionalAliases) {
      Recipe<?> recipe = recipeBase instanceof Recipe<?> direct ? direct : null;
      String sourceKey = "";
      String recipeId = "";
      ArrayList<String> defaultAliases = new ArrayList<>();
      if (recipe != null) {
         ResourceLocation typeId = BuiltInRegistries.f_256990_.m_7981_(recipe.m_6671_());
         sourceKey = typeId == null ? "" : typeId.toString();
      }

      if (sourceKey.isBlank() && fallbackSourceKey != null) {
         sourceKey = fallbackSourceKey;
      }

      if (additionalAliases != null) {
         additionalAliases.forEach(value -> addDefaultAlias(defaultAliases, value));
      }

      publish(menu, sourceKey, recipeId, defaultAliases);
   }

   public static synchronized void publish(TianshuPatternEncodingTermMenu menu, String sourceKey, String recipeId, Iterable<String> defaultAliases) {
      if (menu != null) {
         LinkedHashSet<String> aliases = new LinkedHashSet<>();
         addSourceAliases(aliases, sourceKey);
         if (defaultAliases != null) {
            defaultAliases.forEach(value -> {
               if (value != null && !value.isBlank()) {
                  aliases.add(value);
               }
            });
         }

         owner = new WeakReference<>(menu);
         snapshot = new TianshuRecipeTransferContext.Snapshot(sourceKey == null ? "" : sourceKey, recipeId == null ? "" : recipeId, List.copyOf(aliases));
         resetPendingEncoding();
      }
   }

   private static void addSourceAliases(LinkedHashSet<String> aliases, String sourceKey) {
      if (sourceKey != null && !sourceKey.isBlank()) {
         aliases.add(sourceKey);
         ResourceLocation sourceId = ResourceLocation.m_135820_(sourceKey);
         if (sourceId != null) {
            aliases.add(TianshuUploadAliasRules.namespaceGlob(sourceId.m_135827_()));
         }
      }
   }

   public static synchronized TianshuRecipeTransferContext.Snapshot snapshotFor(TianshuPatternEncodingTermMenu menu) {
      return owner.get() == menu ? snapshot : TianshuRecipeTransferContext.Snapshot.EMPTY;
   }

   public static synchronized void clear(TianshuPatternEncodingTermMenu menu) {
      if (menu != null) {
         owner = new WeakReference<>(menu);
         snapshot = TianshuRecipeTransferContext.Snapshot.EMPTY;
         resetPendingEncoding();
      }
   }

   public static synchronized void beginEncoding(TianshuPatternEncodingTermMenu menu, ItemStack currentPattern) {
      if (owner.get() == menu && !snapshot.sourceKey().isBlank()) {
         encodingSourcePattern = copyOrEmpty(currentPattern);
         encodingPending = true;
         encodingAckReceived = false;
         encodingBindingDeadline = menu.getPlayer().f_19797_ + 40;
      } else {
         resetPendingEncoding();
      }
   }

   public static synchronized void acceptEncodedPattern(TianshuPatternEncodingTermMenu menu, ItemStack encodedPattern) {
      if (encodingPending && owner.get() == menu && !snapshot.sourceKey().isBlank()) {
         if (!encodingAckReceived) {
            encodingAckReceived = true;
            encodingBindingDeadline = menu.getPlayer().f_19797_ + 5;
         }

         if (isNewEncodingResult(encodedPattern)) {
            resetPendingEncoding();
         }
      }
   }

   public static synchronized boolean isEncodingResultReady(TianshuPatternEncodingTermMenu menu, ItemStack current) {
      finishExpiredEncoding(menu);
      if (encodingPending && owner.get() == menu && !snapshot.sourceKey().isBlank()) {
         if (encodingAckReceived && isNewEncodingResult(current)) {
            resetPendingEncoding();
         }

         return !encodingPending;
      } else {
         return true;
      }
   }

   private static void finishExpiredEncoding(TianshuPatternEncodingTermMenu menu) {
      if (encodingPending && owner.get() == menu && menu.getPlayer().f_19797_ > encodingBindingDeadline) {
         resetPendingEncoding();
      }
   }

   private static boolean isNewEncodingResult(ItemStack pattern) {
      return pattern != null && !pattern.m_41619_() && (encodingSourcePattern.m_41619_() || !ItemStack.m_41728_(encodingSourcePattern, pattern));
   }

   private static ItemStack copyOrEmpty(ItemStack stack) {
      return stack != null && !stack.m_41619_() ? stack.m_41777_() : ItemStack.f_41583_;
   }

   private static void resetPendingEncoding() {
      encodingSourcePattern = ItemStack.f_41583_;
      encodingPending = false;
      encodingAckReceived = false;
      encodingBindingDeadline = 0;
   }

   public static void addDefaultAlias(List<String> aliases, String value) {
      if (aliases != null && value != null && !value.isBlank() && !aliases.contains(value)) {
         aliases.add(value);
      }
   }

   public static String firstPathSegment(String path) {
      if (path != null && !path.isBlank()) {
         int slash = path.indexOf(47);
         return slash >= 0 ? path.substring(0, slash) : path;
      } else {
         return "";
      }
   }

   public static record Snapshot(String sourceKey, String recipeId, List<String> defaultAliases) {
      private static final TianshuRecipeTransferContext.Snapshot EMPTY = new TianshuRecipeTransferContext.Snapshot("", "", List.of());

      public Snapshot(String sourceKey, String recipeId, List<String> defaultAliases) {
         sourceKey = sourceKey == null ? "" : sourceKey;
         recipeId = recipeId == null ? "" : recipeId;
         defaultAliases = defaultAliases == null ? List.of() : List.copyOf(defaultAliases);
         this.sourceKey = sourceKey;
         this.recipeId = recipeId;
         this.defaultAliases = defaultAliases;
      }
   }
}
