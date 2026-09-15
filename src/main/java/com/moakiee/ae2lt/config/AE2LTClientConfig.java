package com.moakiee.ae2lt.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;

public final class AE2LTClientConfig {
   public static final ForgeConfigSpec SPEC;
   private static final EnumValue<TianshuUploadTrigger> TIANSHU_UPLOAD_TRIGGER;
   private static final BooleanValue TIANSHU_INTERCEPT_DUPLICATE_ENCODING;
   private static final ConfigValue<List<? extends String>> TIANSHU_UPLOAD_ALIASES;
   private static final BooleanValue DISABLE_CORE_SHADER_RENDERING;
   private static final BooleanValue RENDER_MULTIBLOCK_CORE_EFFECTS;

   private AE2LTClientConfig() {
   }

   public static TianshuUploadTrigger uploadTrigger() {
      return (TianshuUploadTrigger)TIANSHU_UPLOAD_TRIGGER.get();
   }

   public static void setUploadTrigger(TianshuUploadTrigger trigger) {
      if (trigger != null) {
         TIANSHU_UPLOAD_TRIGGER.set(trigger);
         if (SPEC.isLoaded()) {
            SPEC.save();
         }
      }
   }

   public static boolean interceptDuplicatePatternEncoding() {
      return (Boolean)TIANSHU_INTERCEPT_DUPLICATE_ENCODING.get();
   }

   public static boolean renderMultiblockCoreEffects() {
      return (Boolean)RENDER_MULTIBLOCK_CORE_EFFECTS.get();
   }

   public static boolean useCoreShaderRendering() {
      return !(Boolean)DISABLE_CORE_SHADER_RENDERING.get();
   }

   public static void setInterceptDuplicatePatternEncoding(boolean enabled) {
      TIANSHU_INTERCEPT_DUPLICATE_ENCODING.set(enabled);
      if (SPEC.isLoaded()) {
         SPEC.save();
      }
   }

   public static synchronized String findUploadAlias(String sourceKey) {
      String normalized = normalizeAliasKey(sourceKey);
      if (normalized.isEmpty()) {
         return null;
      } else {
         for (String entry : (List)TIANSHU_UPLOAD_ALIASES.get()) {
            int separator = entry.indexOf(61);
            if (separator > 0 && normalizeAliasKey(entry.substring(0, separator)).equals(normalized)) {
               String alias = entry.substring(separator + 1).strip();
               return alias.isEmpty() ? null : alias;
            }
         }

         return null;
      }
   }

   public static synchronized boolean setUploadAlias(String sourceKey, String alias) {
      String normalized = normalizeAliasKey(sourceKey);
      String cleanAlias = alias == null ? "" : alias.strip();
      if (!normalized.isEmpty() && !cleanAlias.isEmpty() && cleanAlias.length() <= 256) {
         ArrayList<String> updated = new ArrayList<>();

         for (String entry : (List)TIANSHU_UPLOAD_ALIASES.get()) {
            int separator = entry.indexOf(61);
            if (separator <= 0 || !normalizeAliasKey(entry.substring(0, separator)).equals(normalized)) {
               updated.add(entry);
            }
         }

         updated.add(normalized + "=" + cleanAlias);
         TIANSHU_UPLOAD_ALIASES.set(List.copyOf(updated));
         if (SPEC.isLoaded()) {
            SPEC.save();
         }

         return true;
      } else {
         return false;
      }
   }

   public static synchronized int removeUploadAliases(String alias) {
      String target = alias == null ? "" : alias.strip();
      if (target.isEmpty()) {
         return 0;
      } else {
         ArrayList<String> updated = new ArrayList<>();
         int removed = 0;

         for (String entry : (List)TIANSHU_UPLOAD_ALIASES.get()) {
            int separator = entry.indexOf(61);
            String storedAlias = separator < 0 ? "" : entry.substring(separator + 1).strip();
            if (storedAlias.equalsIgnoreCase(target)) {
               removed++;
            } else {
               updated.add(entry);
            }
         }

         if (removed > 0) {
            TIANSHU_UPLOAD_ALIASES.set(List.copyOf(updated));
            if (SPEC.isLoaded()) {
               SPEC.save();
            }
         }

         return removed;
      }
   }

   private static String normalizeAliasKey(String sourceKey) {
      return sourceKey == null ? "" : sourceKey.strip().toLowerCase(Locale.ROOT);
   }

   static {
      Builder builder = new Builder();
      builder.push("tianshuTerminal");
      TIANSHU_UPLOAD_TRIGGER = builder.comment("Modifier condition that starts pattern upload after encoding")
         .defineEnum("uploadTrigger", TianshuUploadTrigger.NO_SHIFT);
      TIANSHU_INTERCEPT_DUPLICATE_ENCODING = builder.comment("Cancel encoding when the same pattern already exists on the ME network")
         .define("interceptDuplicatePatternEncoding", true);
      TIANSHU_UPLOAD_ALIASES = builder.comment("Recipe type/category id to pattern-provider alias mappings (source=alias)")
         .defineListAllowEmpty("uploadAliases", List.of(), value -> {
            if (value instanceof String text && text.length() <= 512 && text.indexOf(61) > 0) {
               return true;
            }

            return false;
         });
      builder.pop();
      builder.push("compatibility");
      DISABLE_CORE_SHADER_RENDERING = builder.comment(
            new String[]{
               "Compatibility switch: disable custom core shaders provided by AE2 Lightning Tech.",
               "Enable only when a graphics driver or another rendering mod is incompatible with them."
            }
         )
         .define("disableCoreShaderRendering", false);
      builder.pop();
      builder.push("rendering");
      RENDER_MULTIBLOCK_CORE_EFFECTS = builder.comment("Render formed Tianshu and matter-warping core effects").define("multiblockCoreEffects", true);
      builder.pop();
      SPEC = builder.build();
   }
}
