package com.moakiee.ae2lt.client;

import appeng.api.implementations.blockentities.PatternContainerGroup;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuUploadTargetData;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

final class TianshuUploadTargetMatcher {
   private TianshuUploadTargetMatcher() {
   }

   static boolean matches(TianshuUploadTargetData target, String query) {
      if (target != null && query != null && !query.isBlank()) {
         String normalizedQuery = normalize(query.strip());
         PatternContainerGroup group = target.group();
         return group.icon() != null && idMatches(group.icon().getId().toString(), normalizedQuery)
            ? true
            : nameMatches(group.name().getString(), normalizedQuery);
      } else {
         return true;
      }
   }

   static TianshuUploadTargetData findUniqueCandidate(List<TianshuUploadTargetData> targets, String query) {
      return findUniqueCandidate(targets, target -> matches(target, query), target -> target.availableSlots() > 0);
   }

   static <T> T findUniqueCandidate(List<T> candidates, Predicate<T> matches, Predicate<T> writable) {
      if (candidates == null) {
         return null;
      } else {
         T selected = null;

         for (T target : candidates) {
            if (matches.test(target)) {
               if (selected != null) {
                  return null;
               }

               selected = target;
            }
         }

         return selected != null && writable.test(selected) ? selected : null;
      }
   }

   static String findClosestUniqueAlias(List<TianshuUploadTargetData> targets, String preferredAlias, List<String> defaultAliases) {
      return findClosestUniqueAlias(targets, preferredAlias, defaultAliases, TianshuUploadTargetMatcher::matches);
   }

   static <T> String findClosestUniqueAlias(List<T> targets, String preferredAlias, List<String> defaultAliases, BiPredicate<T, String> matches) {
      String fallback = preferredAlias == null ? "" : preferredAlias;
      String selected = fallback;
      int selectedMatches = countMatches(targets, fallback, matches);
      if (selectedMatches == 1) {
         return fallback;
      } else {
         if (defaultAliases != null) {
            for (String alias : defaultAliases) {
               if (alias != null && !alias.isBlank() && !alias.equalsIgnoreCase(fallback)) {
                  int aliasMatches = countMatches(targets, alias, matches);
                  if (aliasMatches > 0 && (selectedMatches <= 0 || aliasMatches < selectedMatches)) {
                     selected = alias;
                     selectedMatches = aliasMatches;
                     if (aliasMatches == 1) {
                        return alias;
                     }
                  }
               }
            }
         }

         return selectedMatches > 0 ? selected : fallback;
      }
   }

   private static <T> int countMatches(List<T> targets, String alias, BiPredicate<T, String> matches) {
      if (targets != null && alias != null && !alias.isBlank() && matches != null) {
         int count = 0;

         for (T target : targets) {
            if (matches.test(target, alias)) {
               count++;
            }
         }

         return count;
      } else {
         return 0;
      }
   }

   static boolean idMatches(String machineId, String query) {
      return machineId != null && query != null && !query.isBlank() ? globMatches(normalize(machineId), normalize(query)) : false;
   }

   static boolean nameMatches(String machineName, String query) {
      return nameMatches(machineName, query, JecSearchCompat::contains);
   }

   static boolean nameMatches(String machineName, String query, BiPredicate<String, String> pinyinContains) {
      if (machineName != null && query != null && !query.isBlank()) {
         String normalizedName = normalize(machineName);
         String normalizedQuery = normalize(query);
         if (containsWildcard(normalizedQuery) ? !globMatches(normalizedName, "*" + normalizedQuery + "*") : !normalizedName.contains(normalizedQuery)) {
            if (containsWildcard(normalizedQuery)) {
               boolean hasFragment = false;

               for (String fragment : normalizedQuery.split("[?*]+")) {
                  if (!fragment.isBlank()) {
                     if (!isPinyinFragment(fragment)) {
                        return false;
                     }

                     hasFragment = true;
                     if (!pinyinContains.test(normalizedName, fragment)) {
                        return false;
                     }
                  }
               }

               return hasFragment;
            } else {
               return isPinyinFragment(normalizedQuery) && pinyinContains.test(normalizedName, normalizedQuery);
            }
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private static boolean isPinyinFragment(String value) {
      if (value != null && !value.isEmpty()) {
         for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character < 'a' || character > 'z') {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   static String preferredAlias(String machineId, String defaultName, String currentName) {
      String id = machineId == null ? "" : machineId.strip();
      String defaultLabel = defaultName == null ? "" : defaultName.strip();
      String currentLabel = currentName == null ? "" : currentName.strip();
      return !currentLabel.isEmpty() && !currentLabel.equals(defaultLabel) ? currentLabel : id;
   }

   static boolean globMatches(String value, String pattern) {
      if (value != null && pattern != null) {
         int valueIndex = 0;
         int patternIndex = 0;
         int starIndex = -1;
         int starValueIndex = -1;

         while (valueIndex < value.length()) {
            if (patternIndex >= pattern.length() || pattern.charAt(patternIndex) != '?' && pattern.charAt(patternIndex) != value.charAt(valueIndex)) {
               if (patternIndex < pattern.length() && pattern.charAt(patternIndex) == '*') {
                  starIndex = patternIndex++;
                  starValueIndex = valueIndex;
               } else {
                  if (starIndex < 0) {
                     return false;
                  }

                  patternIndex = starIndex + 1;
                  valueIndex = ++starValueIndex;
               }
            } else {
               valueIndex++;
               patternIndex++;
            }
         }

         while (patternIndex < pattern.length() && pattern.charAt(patternIndex) == '*') {
            patternIndex++;
         }

         return patternIndex == pattern.length();
      } else {
         return false;
      }
   }

   private static boolean containsWildcard(String value) {
      return value.indexOf(42) >= 0 || value.indexOf(63) >= 0;
   }

   private static String normalize(String value) {
      return value.toLowerCase(Locale.ROOT);
   }
}
