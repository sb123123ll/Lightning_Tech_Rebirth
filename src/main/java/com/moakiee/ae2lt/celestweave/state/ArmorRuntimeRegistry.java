package com.moakiee.ae2lt.celestweave.state;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ArmorRuntimeRegistry {
   private static final Map<String, Boolean> SUBMODULE_RUNTIME_ACTIVE = new ConcurrentHashMap<>();

   private ArmorRuntimeRegistry() {
   }

   public static Boolean setSubmoduleRuntimeActive(UUID armorId, String submoduleId, boolean active) {
      if (armorId == null) {
         return null;
      } else {
         String key = cacheKey(armorId, submoduleId);
         return active ? SUBMODULE_RUNTIME_ACTIVE.put(key, Boolean.TRUE) : SUBMODULE_RUNTIME_ACTIVE.remove(key);
      }
   }

   public static boolean isSubmoduleRuntimeActive(UUID armorId, String submoduleId) {
      return armorId != null && SUBMODULE_RUNTIME_ACTIVE.getOrDefault(cacheKey(armorId, submoduleId), false);
   }

   public static Set<String> submoduleIds(UUID armorId) {
      Set<String> ids = new HashSet<>();
      if (armorId == null) {
         return ids;
      } else {
         String prefix = armorId + "#";

         for (String key : SUBMODULE_RUNTIME_ACTIVE.keySet()) {
            if (key.startsWith(prefix)) {
               ids.add(key.substring(prefix.length()));
            }
         }

         return ids;
      }
   }

   public static void removeSubmodule(UUID armorId, String submoduleId) {
      if (armorId != null) {
         SUBMODULE_RUNTIME_ACTIVE.remove(cacheKey(armorId, submoduleId));
      }
   }

   public static void clear(UUID armorId) {
      if (armorId != null) {
         String prefix = armorId + "#";
         SUBMODULE_RUNTIME_ACTIVE.keySet().removeIf(key -> key.startsWith(prefix));
      }
   }

   private static String cacheKey(UUID armorId, String submoduleId) {
      return armorId + "#" + submoduleId;
   }
}
