package com.moakiee.ae2lt.celestweave.phase;

import com.moakiee.ae2lt.celestweave.CelestweaveArmorState;
import com.moakiee.ae2lt.registry.ModDataComponents;
import com.moakiee.ae2lt.util.ItemStackTagSupport;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.registries.ForgeRegistries;

final class PhaseLockProjectionSynchronizer {
   private static final String TAG_ENCHANTMENTS = "Enchantments";

   private PhaseLockProjectionSynchronizer() {
   }

   static void synchronize(ServerPlayer player, ItemStack armor, ItemStack projection) {
      clearMisplacedPrivateComponents(armor, projection);
      UUID armorId = CelestweaveArmorState.ensureArmorId(armor);
      long armorUpdate = ModDataComponents.PHASE_LOCK_ARMOR_UPDATE.getOrDefault(armor, 0L);
      PhaseLockProjectionLink link = ModDataComponents.PHASE_LOCK_PROJECTION_LINK.get(projection);
      PhaseLockProjectionSynchronizer.ProjectionCurses projectionCurses = projectionCurses();
      boolean equal = armorSnapshot(armor).equals(projectionSnapshot(projection, armor, projectionCurses));
      PhaseLockProjectionSyncRules.Direction direction = PhaseLockProjectionSyncRules.direction(armorId, armorUpdate, link, equal);
      if (direction == PhaseLockProjectionSyncRules.Direction.NONE) {
         if (link != null && link.armorId().equals(armorId) && link.update() > armorUpdate) {
            ModDataComponents.PHASE_LOCK_ARMOR_UPDATE.set(armor, link.update());
         }

         ensureProjectionCurses(projection, projectionCurses);
      } else {
         long nextUpdate = PhaseLockProjectionSyncRules.nextUpdate(armorUpdate, link);
         if (direction == PhaseLockProjectionSyncRules.Direction.ARMOR_TO_PROJECTION) {
            ModDataComponents.PHASE_LOCK_ARMOR_UPDATE.set(armor, nextUpdate);
            replaceMirroredComponents(armor, projection, false);
            ensureProjectionCurses(projection, projectionCurses);
         } else {
            replaceMirroredComponents(projection, armor, true);
            copyProjectionEnchantmentsToArmor(projection, armor, projectionCurses);
            ModDataComponents.PHASE_LOCK_ARMOR_UPDATE.set(armor, nextUpdate);
         }

         ModDataComponents.PHASE_LOCK_PROJECTION_LINK.set(projection, new PhaseLockProjectionLink(armorId, nextUpdate));
         ensureProjectionCurses(projection, projectionCurses);
      }
   }

   static PhaseLockProjectionSynchronizer.MirroredSnapshot captureArmorFields(ItemStack armor) {
      return armorSnapshot(armor);
   }

   static void publishArmorChanges(ServerPlayer player, ItemStack armor, ItemStack projection, PhaseLockProjectionSynchronizer.MirroredSnapshot before) {
      if (!before.equals(armorSnapshot(armor))) {
         UUID armorId = CelestweaveArmorState.ensureArmorId(armor);
         long armorUpdate = ModDataComponents.PHASE_LOCK_ARMOR_UPDATE.getOrDefault(armor, 0L);
         PhaseLockProjectionLink link = ModDataComponents.PHASE_LOCK_PROJECTION_LINK.get(projection);
         long nextUpdate = PhaseLockProjectionSyncRules.nextUpdate(armorUpdate, link);
         ModDataComponents.PHASE_LOCK_ARMOR_UPDATE.set(armor, nextUpdate);
         replaceMirroredComponents(armor, projection, false);
         ensureProjectionCurses(projection, projectionCurses());
         ModDataComponents.PHASE_LOCK_PROJECTION_LINK.set(projection, new PhaseLockProjectionLink(armorId, nextUpdate));
      }
   }

   private static PhaseLockProjectionSynchronizer.MirroredSnapshot armorSnapshot(ItemStack armor) {
      return snapshot(armor, null, null);
   }

   private static PhaseLockProjectionSynchronizer.MirroredSnapshot projectionSnapshot(
      ItemStack projection, ItemStack armor, PhaseLockProjectionSynchronizer.ProjectionCurses projectionCurses
   ) {
      return snapshot(projection, armor, projectionCurses);
   }

   private static PhaseLockProjectionSynchronizer.MirroredSnapshot snapshot(
      ItemStack stack, ItemStack authoritativeArmor, PhaseLockProjectionSynchronizer.ProjectionCurses projectionCurses
   ) {
      CompoundTag tag = stack.m_41783_();
      if (tag == null) {
         return new PhaseLockProjectionSynchronizer.MirroredSnapshot(Map.of());
      } else {
         Map<String, Tag> fields = new HashMap<>();

         for (String key : tag.m_128431_()) {
            if (isMirrored(key)) {
               Tag value;
               if ("Enchantments".equals(key)) {
                  Map<Enchantment, Integer> normalizedEnchantments = authoritativeArmor == null
                     ? enchantments(stack)
                     : projectionEnchantmentsForArmor(stack, authoritativeArmor, projectionCurses);
                  if (normalizedEnchantments.isEmpty()) {
                     continue;
                  }

                  value = enchantmentsToTag(normalizedEnchantments);
               } else {
                  value = tag.m_128423_(key);
               }

               fields.put(key, value);
            }
         }

         return new PhaseLockProjectionSynchronizer.MirroredSnapshot(Map.copyOf(fields));
      }
   }

   private static void replaceMirroredComponents(ItemStack source, ItemStack target, boolean skipEnchantments) {
      ItemStackTagSupport.updateTag(target, targetTag -> {
         for (String keyx : new ArrayList(targetTag.m_128431_())) {
            if (isMirrored(keyx) && (!skipEnchantments || !"Enchantments".equals(keyx))) {
               targetTag.m_128473_(keyx);
            }
         }
      });
      CompoundTag sourceTag = source.m_41783_();
      if (sourceTag != null) {
         for (String key : sourceTag.m_128431_()) {
            if (isMirrored(key) && (!skipEnchantments || !"Enchantments".equals(key))) {
               Tag value = sourceTag.m_128423_(key);
               ItemStackTagSupport.updateTag(target, t -> t.m_128365_(key, value.m_6426_()));
            }
         }
      }
   }

   private static void copyProjectionEnchantmentsToArmor(
      ItemStack projection, ItemStack armor, PhaseLockProjectionSynchronizer.ProjectionCurses projectionCurses
   ) {
      setEnchantments(armor, projectionEnchantmentsForArmor(projection, armor, projectionCurses));
   }

   private static Map<Enchantment, Integer> projectionEnchantmentsForArmor(
      ItemStack projection, ItemStack armor, PhaseLockProjectionSynchronizer.ProjectionCurses projectionCurses
   ) {
      Map<Enchantment, Integer> projectionEnchantments = enchantments(projection);
      Map<Enchantment, Integer> armorEnchantments = enchantments(armor);
      removeProjectionOnlyCurse(projectionEnchantments, armorEnchantments, projectionCurses.binding());
      removeProjectionOnlyCurse(projectionEnchantments, armorEnchantments, projectionCurses.vanishing());
      return projectionEnchantments;
   }

   private static void removeProjectionOnlyCurse(
      Map<Enchantment, Integer> projectionEnchantments, Map<Enchantment, Integer> armorEnchantments, Enchantment curse
   ) {
      if (armorEnchantments.getOrDefault(curse, 0) == 0) {
         projectionEnchantments.remove(curse);
      }
   }

   private static void ensureProjectionCurses(ItemStack projection, PhaseLockProjectionSynchronizer.ProjectionCurses projectionCurses) {
      Map<Enchantment, Integer> enchantments = enchantments(projection);
      if (enchantments.getOrDefault(projectionCurses.binding(), 0) != 1 || enchantments.getOrDefault(projectionCurses.vanishing(), 0) != 1) {
         enchantments.put(projectionCurses.binding(), 1);
         enchantments.put(projectionCurses.vanishing(), 1);
         setEnchantments(projection, enchantments);
      }
   }

   private static Map<Enchantment, Integer> enchantments(ItemStack stack) {
      return new HashMap<>(EnchantmentHelper.m_44831_(stack));
   }

   private static ListTag enchantmentsToTag(Map<Enchantment, Integer> enchantments) {
      ListTag list = new ListTag();
      ArrayList<Entry<Enchantment, Integer>> entries = new ArrayList<>(enchantments.entrySet());
      entries.sort(Comparator.comparing(entryx -> ForgeRegistries.ENCHANTMENTS.getKey((Enchantment)entryx.getKey()).toString()));

      for (Entry<Enchantment, Integer> entry : entries) {
         CompoundTag entryTag = new CompoundTag();
         entryTag.m_128359_("id", ForgeRegistries.ENCHANTMENTS.getKey(entry.getKey()).toString());
         entryTag.m_128376_("lvl", entry.getValue().shortValue());
         list.add(entryTag);
      }

      return list;
   }

   private static void setEnchantments(ItemStack stack, Map<Enchantment, Integer> enchantments) {
      EnchantmentHelper.m_44865_(enchantments, stack);
   }

   private static PhaseLockProjectionSynchronizer.ProjectionCurses projectionCurses() {
      return new PhaseLockProjectionSynchronizer.ProjectionCurses(Enchantments.f_44975_, Enchantments.f_44963_);
   }

   private static boolean isMirrored(String key) {
      return !key.startsWith("ae2lt:");
   }

   private static void clearMisplacedPrivateComponents(ItemStack armor, ItemStack projection) {
      ModDataComponents.PHASE_LOCK_PROJECTION_LINK.remove(armor);
      ModDataComponents.PHASE_LOCK_ARMOR_UPDATE.remove(projection);
      ModDataComponents.CELESTWEAVE_STRUCTURAL_CORE.remove(projection);
      ModDataComponents.CELESTWEAVE_ENERGY_BUFFER.remove(projection);
      ModDataComponents.CELESTWEAVE_MODULES.remove(projection);
      ModDataComponents.CELESTWEAVE_MODULES_POWERED.remove(projection);
   }

   static record MirroredSnapshot(Map<String, Tag> fields) {
   }

   private static record ProjectionCurses(Enchantment binding, Enchantment vanishing) {
   }
}
