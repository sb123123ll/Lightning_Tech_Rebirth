package com.moakiee.ae2lt.logic.tianshu.maintenance;

import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import it.unimi.dsi.fastutil.objects.Object2LongMap.Entry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

public final class MaintenanceVariantService {
   public static List<MaintenanceVariantService.Variant> list(MEStorage storage, ICraftingService crafting, AEKey selected) {
      return storage != null && crafting != null && selected != null
         ? list(storage.getAvailableStacks(), crafting.getCraftables(key -> sameGroup(selected, key)), selected)
         : List.of();
   }

   public static List<MaintenanceVariantService.Variant> list(KeyCounter stored, Iterable<AEKey> craftables, AEKey selected) {
      if (selected == null) {
         return List.of();
      } else {
         LinkedHashMap<AEKey, MaintenanceVariantService.MutableVariant> variants = new LinkedHashMap<>();
         variants.put(selected, new MaintenanceVariantService.MutableVariant());
         if (stored != null) {
            for (Entry<AEKey> entry : stored) {
               if (sameGroup(selected, (AEKey)entry.getKey())) {
                  variants.computeIfAbsent((AEKey)entry.getKey(), ignored -> new MaintenanceVariantService.MutableVariant()).stored = Math.max(
                     0L, entry.getLongValue()
                  );
               }
            }
         }

         if (craftables != null) {
            for (AEKey key : craftables) {
               if (key != null && sameGroup(selected, key)) {
                  variants.computeIfAbsent(key, ignored -> new MaintenanceVariantService.MutableVariant()).craftable = true;
               }
            }
         }

         ArrayList<MaintenanceVariantService.Variant> result = new ArrayList<>(variants.size());
         variants.forEach((keyx, value) -> result.add(new MaintenanceVariantService.Variant(keyx, value.stored, value.craftable)));
         result.sort(Comparator.comparing(entryx -> stableKey(entryx.key())));
         return List.copyOf(result);
      }
   }

   private static boolean sameGroup(AEKey left, AEKey right) {
      return left != null && right != null && left.dropSecondary().equals(right.dropSecondary());
   }

   private static String stableKey(AEKey key) {
      return key.getId() + "|" + key.getPrimaryKey() + "|" + key.hashCode();
   }

   private MaintenanceVariantService() {
   }

   private static final class MutableVariant {
      long stored;
      boolean craftable;
   }

   public static record Variant(AEKey key, long storedAmount, boolean craftable) {
   }
}
