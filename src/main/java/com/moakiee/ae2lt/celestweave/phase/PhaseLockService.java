package com.moakiee.ae2lt.celestweave.phase;

import com.moakiee.ae2lt.celestweave.ArmorPart;
import com.moakiee.ae2lt.celestweave.BaseCelestweaveArmorItem;
import com.moakiee.ae2lt.celestweave.CelestweaveArmorState;
import com.moakiee.ae2lt.celestweave.module.PhaseLockSubmodule;
import com.moakiee.ae2lt.celestweave.service.ArmorCapabilityCollector;
import com.moakiee.ae2lt.celestweave.service.ArmorEnergyService;
import com.moakiee.ae2lt.celestweave.service.ArmorLightningService;
import com.moakiee.ae2lt.celestweave.service.ArmorTickService;
import com.moakiee.ae2lt.item.PhaseLockProjectionItem;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.registry.ModItems;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;

public final class PhaseLockService {
   private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
   private static final Set<UUID> TRANSFERRING_PLAYERS = ConcurrentHashMap.newKeySet();

   private PhaseLockService() {
   }

   public static void tick(ServerPlayer player) {
      PhaseArmorVaultSavedData vault = vault(player);
      UUID playerId = player.m_20148_();
      if (!vault.containsAny(playerId)) {
         tryEnter(player, vault);
      } else {
         ItemStack anchor = vault.getMutable(playerId, EquipmentSlot.CHEST);
         if (!isRealArmorForSlot(anchor, EquipmentSlot.CHEST)) {
            recoverInvalidVaultEntries(player, vault);
         } else if (!shouldRemainLocked(player, anchor)) {
            release(player, false);
         } else {
            for (EquipmentSlot slot : ARMOR_SLOTS) {
               ItemStack armor = vault.getMutable(playerId, slot);
               if (armor != null && !isRealArmorForSlot(armor, slot)) {
                  recoverInvalidVaultEntries(player, vault);
                  return;
               }
            }

            EnumSet<EquipmentSlot> newlyLocked = captureNewArmor(player, vault);

            for (EquipmentSlot slotx : ARMOR_SLOTS) {
               ItemStack armor = vault.getMutable(playerId, slotx);
               if (armor == null) {
                  clearStaleProjection(player, slotx);
               } else {
                  if (!isProjectionForSlot(player.m_6844_(slotx), slotx)) {
                     ArmorEnergyService.EnergyPayment payment = ArmorEnergyService.consumeActiveCostPayment(player, armor, 1000000L);
                     if (!completeRegenerationPayment(
                        payment.paid(), () -> ArmorLightningService.consume(player, armor, LightningKey.EXTREME_HIGH_VOLTAGE, 16L), payment::refund
                     )) {
                        release(player, true);
                        return;
                     }

                     displaceArmorOccupant(player, slotx, armor);
                     player.m_8061_(slotx, createProjection(player, slotx, armor));
                  }

                  PhaseLockProjectionSynchronizer.synchronize(player, armor, player.m_6844_(slotx));
               }
            }

            for (EquipmentSlot slotxx : ARMOR_SLOTS) {
               if (!newlyLocked.contains(slotxx)) {
                  ItemStack armor = vault.getMutable(playerId, slotxx);
                  if (armor != null) {
                     ItemStack projection = player.m_6844_(slotxx);
                     PhaseLockProjectionSynchronizer.MirroredSnapshot before = PhaseLockProjectionSynchronizer.captureArmorFields(armor);
                     ArmorTickService.tickEquipped(player, armor, true, player.m_9236_().m_9598_(), Dist.DEDICATED_SERVER);
                     PhaseLockProjectionSynchronizer.publishArmorChanges(player, armor, projection, before);
                  }
               }
            }
         }
      }
   }

   static boolean completeRegenerationPayment(boolean fePaid, BooleanSupplier consumeExtremeHighVoltage, Runnable refundFe) {
      if (!fePaid) {
         return false;
      } else if (consumeExtremeHighVoltage.getAsBoolean()) {
         return true;
      } else {
         refundFe.run();
         return false;
      }
   }

   public static boolean hasPrivateArmor(ServerPlayer player, EquipmentSlot slot) {
      return vault(player).contains(player.m_20148_(), slot);
   }

   public static boolean hasPrivateArmor(ServerPlayer player) {
      return vault(player).containsAny(player.m_20148_());
   }

   public static ItemStack getPrivateArmor(ServerPlayer player, EquipmentSlot slot) {
      ItemStack armor = vault(player).getMutable(player.m_20148_(), slot);
      return armor == null ? ItemStack.f_41583_ : armor;
   }

   public static boolean keepsLogicallyEquipped(ServerPlayer player, EquipmentSlot slot, ItemStack removedArmor) {
      if (TRANSFERRING_PLAYERS.contains(player.m_20148_())) {
         return true;
      } else {
         ItemStack privateArmor = getPrivateArmor(player, slot);
         if (privateArmor == removedArmor) {
            return true;
         } else {
            UUID removedId = CelestweaveArmorState.getArmorId(removedArmor);
            UUID privateId = CelestweaveArmorState.getArmorId(privateArmor);
            return removedId != null && removedId.equals(privateId);
         }
      }
   }

   public static boolean release(ServerPlayer player) {
      return release(player, false);
   }

   private static boolean release(ServerPlayer player, boolean collapsed) {
      PhaseArmorVaultSavedData vault = vault(player);
      synchronizeBeforeRelease(player, vault);
      EnumMap<EquipmentSlot, ItemStack> armorBySlot = vault.takeAll(player.m_20148_());
      clearPlayerProjections(player);
      boolean restoredAny = false;

      for (EquipmentSlot slot : ARMOR_SLOTS) {
         ItemStack armor = armorBySlot.remove(slot);
         if (armor != null && !armor.m_41619_()) {
            if (!isRealArmorForSlot(armor, slot)) {
               player.m_150109_().m_150079_(armor);
            } else {
               displaceArmorOccupant(player, slot, armor);
               player.m_8061_(slot, armor);
               restoredAny = true;
               ArmorTickService.tickEquipped(player, armor, true, player.m_9236_().m_9598_(), Dist.DEDICATED_SERVER);
            }
         }
      }

      for (ItemStack armor : armorBySlot.values()) {
         if (armor != null && !armor.m_41619_()) {
            player.m_150109_().m_150079_(armor);
         }
      }

      ArmorCapabilityCollector.clearCache(player);
      if (collapsed) {
         playCollapseSound(player);
      }

      return restoredAny;
   }

   private static void tryEnter(ServerPlayer player, PhaseArmorVaultSavedData vault) {
      ItemStack anchor = player.m_6844_(EquipmentSlot.CHEST);
      if (isRealArmorForSlot(anchor, EquipmentSlot.CHEST) && shouldRemainLocked(player, anchor)) {
         TRANSFERRING_PLAYERS.add(player.m_20148_());

         try {
            transferIntoVault(player, vault);
         } finally {
            TRANSFERRING_PLAYERS.remove(player.m_20148_());
         }
      }
   }

   private static void transferIntoVault(ServerPlayer player, PhaseArmorVaultSavedData vault) {
      clearPlayerProjections(player);
      EnumMap<EquipmentSlot, ItemStack> armorToLock = new EnumMap<>(EquipmentSlot.class);

      for (EquipmentSlot slot : ARMOR_SLOTS) {
         ItemStack armor = player.m_6844_(slot);
         if (isRealArmorForSlot(armor, slot)) {
            armorToLock.put((Enum)slot, armor);
         }
      }

      for (EquipmentSlot slotx : armorToLock.keySet()) {
         player.m_8061_(slotx, ItemStack.f_41583_);
      }

      for (Entry<EquipmentSlot, ItemStack> entry : armorToLock.entrySet()) {
         if (!vault.store(player.m_20148_(), entry.getKey(), entry.getValue())) {
            restoreFailedEntry(player, vault, armorToLock);
            return;
         }
      }

      for (EquipmentSlot slotx : armorToLock.keySet()) {
         ItemStack armor = vault.getMutable(player.m_20148_(), slotx);
         player.m_8061_(slotx, createProjection(player, slotx, armor));
      }

      ArmorCapabilityCollector.clearCache(player);
   }

   private static EnumSet<EquipmentSlot> captureNewArmor(ServerPlayer player, PhaseArmorVaultSavedData vault) {
      EnumSet<EquipmentSlot> newlyLocked = EnumSet.noneOf(EquipmentSlot.class);
      TRANSFERRING_PLAYERS.add(player.m_20148_());

      try {
         for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!vault.contains(player.m_20148_(), slot)) {
               ItemStack armor = player.m_6844_(slot);
               if (isRealArmorForSlot(armor, slot)) {
                  player.m_8061_(slot, ItemStack.f_41583_);
                  if (!vault.store(player.m_20148_(), slot, armor)) {
                     player.m_8061_(slot, armor);
                  } else {
                     player.m_8061_(slot, createProjection(player, slot, armor));
                     newlyLocked.add(slot);
                  }
               }
            }
         }
      } finally {
         TRANSFERRING_PLAYERS.remove(player.m_20148_());
      }

      if (!newlyLocked.isEmpty()) {
         ArmorCapabilityCollector.clearCache(player);
      }

      return newlyLocked;
   }

   private static void restoreFailedEntry(ServerPlayer player, PhaseArmorVaultSavedData vault, EnumMap<EquipmentSlot, ItemStack> original) {
      EnumMap<EquipmentSlot, ItemStack> stored = vault.takeAll(player.m_20148_());

      for (EquipmentSlot slot : ARMOR_SLOTS) {
         ItemStack armor = stored.getOrDefault(slot, original.get(slot));
         if (armor != null && !armor.m_41619_()) {
            displaceArmorOccupant(player, slot, armor);
            player.m_8061_(slot, armor);
         }
      }

      ArmorCapabilityCollector.clearCache(player);
   }

   private static boolean shouldRemainLocked(ServerPlayer player, ItemStack anchor) {
      return CelestweaveArmorState.hasCore(anchor, player.m_9236_().m_9598_())
         && CelestweaveArmorState.isSubmoduleInstalled(anchor, player.m_9236_().m_9598_(), PhaseLockSubmodule.INSTANCE.id())
         && CelestweaveArmorState.isSubmoduleEnabled(anchor, PhaseLockSubmodule.INSTANCE)
         && PhaseLockSubmodule.isArmorLockEnabled(anchor);
   }

   private static void recoverInvalidVaultEntries(ServerPlayer player, PhaseArmorVaultSavedData vault) {
      EnumMap<EquipmentSlot, ItemStack> invalidEntries = vault.takeAll(player.m_20148_());
      clearPlayerProjections(player);

      for (ItemStack armor : invalidEntries.values()) {
         if (armor != null && !armor.m_41619_()) {
            player.m_150109_().m_150079_(armor);
         }
      }

      ArmorCapabilityCollector.clearCache(player);
      playCollapseSound(player);
   }

   private static void displaceArmorOccupant(ServerPlayer player, EquipmentSlot slot, ItemStack authoritativeArmor) {
      ItemStack displaced = player.m_6844_(slot);
      if (!displaced.m_41619_()) {
         player.m_8061_(slot, ItemStack.f_41583_);
         if (displaced != authoritativeArmor) {
            if (!isProjection(displaced) && !isDuplicateOf(displaced, authoritativeArmor)) {
               player.m_150109_().m_150079_(displaced);
            } else {
               displaced.m_41764_(0);
            }
         }
      }
   }

   private static boolean isDuplicateOf(ItemStack candidate, ItemStack authoritativeArmor) {
      if (!(candidate.m_41720_() instanceof BaseCelestweaveArmorItem)) {
         return false;
      } else {
         UUID authoritativeId = CelestweaveArmorState.getArmorId(authoritativeArmor);
         UUID candidateId = CelestweaveArmorState.getArmorId(candidate);
         return authoritativeId != null && authoritativeId.equals(candidateId);
      }
   }

   private static ItemStack createProjection(ServerPlayer player, EquipmentSlot slot, ItemStack armor) {
      ItemStack projection = new ItemStack(switch (slot) {
         case HEAD -> (PhaseLockProjectionItem)ModItems.PHASE_LOCK_PROJECTION_HEAD.get();
         case CHEST -> (PhaseLockProjectionItem)ModItems.PHASE_LOCK_PROJECTION.get();
         case LEGS -> (PhaseLockProjectionItem)ModItems.PHASE_LOCK_PROJECTION_LEGS.get();
         case FEET -> (PhaseLockProjectionItem)ModItems.PHASE_LOCK_PROJECTION_FEET.get();
         default -> throw new IllegalArgumentException("Not an armor slot: " + slot);
      });
      PhaseLockProjectionSynchronizer.synchronize(player, armor, projection);
      return projection;
   }

   private static void synchronizeBeforeRelease(ServerPlayer player, PhaseArmorVaultSavedData vault) {
      for (EquipmentSlot slot : ARMOR_SLOTS) {
         ItemStack armor = vault.getMutable(player.m_20148_(), slot);
         ItemStack projection = player.m_6844_(slot);
         if (armor != null && isProjectionForSlot(projection, slot)) {
            PhaseLockProjectionSynchronizer.synchronize(player, armor, projection);
         }
      }
   }

   private static void clearPlayerProjections(ServerPlayer player) {
      Inventory inventory = player.m_150109_();

      for (int slot = 0; slot < inventory.m_6643_(); slot++) {
         ItemStack stack = inventory.m_8020_(slot);
         if (isProjection(stack)) {
            inventory.m_6836_(slot, ItemStack.f_41583_);
         }
      }
   }

   private static void clearStaleProjection(ServerPlayer player, EquipmentSlot slot) {
      ItemStack stack = player.m_6844_(slot);
      if (isProjection(stack)) {
         player.m_8061_(slot, ItemStack.f_41583_);
      }
   }

   private static boolean isProjection(ItemStack stack) {
      return stack != null && !stack.m_41619_() && stack.m_41720_() instanceof PhaseLockProjectionItem;
   }

   private static boolean isProjectionForSlot(ItemStack stack, EquipmentSlot slot) {
      return isProjection(stack) && ((PhaseLockProjectionItem)stack.m_41720_()).equipmentSlot() == slot;
   }

   private static boolean isRealArmorForSlot(ItemStack stack, EquipmentSlot slot) {
      return stack != null && !stack.m_41619_() && stack.m_41720_() instanceof BaseCelestweaveArmorItem armor
         ? equipmentSlot(armor.armorPart()) == slot
         : false;
   }

   private static EquipmentSlot equipmentSlot(ArmorPart part) {
      return switch (part) {
         case HEAD -> EquipmentSlot.HEAD;
         case CHEST -> EquipmentSlot.CHEST;
         case LEGS -> EquipmentSlot.LEGS;
         case FEET -> EquipmentSlot.FEET;
      };
   }

   private static PhaseArmorVaultSavedData vault(ServerPlayer player) {
      return PhaseArmorVaultSavedData.get(player.m_20194_());
   }

   private static void playCollapseSound(ServerPlayer player) {
      player.m_9236_()
         .m_6263_(null, player.m_20185_(), player.m_20186_(), player.m_20189_(), (SoundEvent)SoundEvents.f_12377_.m_203334_(), SoundSource.PLAYERS, 0.8F, 0.7F);
   }
}
