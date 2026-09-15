package com.moakiee.ae2lt.celestweave.phase;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

public final class PhaseArmorVaultSavedData extends SavedData {
   private static final String DATA_NAME = "ae2lt_phase_armor_vault";
   private static final String TAG_ENTRIES = "Entries";
   private static final String TAG_PLAYER = "Player";
   private static final String TAG_SLOT = "Slot";
   private static final String TAG_ARMOR = "Armor";
   private final Map<UUID, EnumMap<EquipmentSlot, ItemStack>> armorByPlayer = new HashMap<>();

   public static PhaseArmorVaultSavedData get(MinecraftServer server) {
      return (PhaseArmorVaultSavedData)server.m_129783_()
         .m_8895_()
         .m_164861_(PhaseArmorVaultSavedData::load, PhaseArmorVaultSavedData::new, "ae2lt_phase_armor_vault");
   }

   @Nullable
   ItemStack getMutable(UUID playerId, EquipmentSlot slot) {
      EnumMap<EquipmentSlot, ItemStack> armorBySlot = this.armorByPlayer.get(playerId);
      ItemStack armor = armorBySlot == null ? null : armorBySlot.get(slot);
      if (armor != null) {
         this.m_77762_();
      }

      return armor;
   }

   boolean store(UUID playerId, EquipmentSlot slot, ItemStack armor) {
      if (isArmorSlot(slot) && armor != null && !armor.m_41619_()) {
         EnumMap<EquipmentSlot, ItemStack> armorBySlot = this.armorByPlayer.computeIfAbsent(playerId, ignored -> new EnumMap<>(EquipmentSlot.class));
         if (armorBySlot.containsKey(slot)) {
            return false;
         } else {
            armorBySlot.put((Enum)slot, armor);
            this.m_77762_();
            return true;
         }
      } else {
         return false;
      }
   }

   ItemStack take(UUID playerId, EquipmentSlot slot) {
      EnumMap<EquipmentSlot, ItemStack> armorBySlot = this.armorByPlayer.get(playerId);
      if (armorBySlot == null) {
         return ItemStack.f_41583_;
      } else {
         ItemStack armor = armorBySlot.remove(slot);
         if (armor == null) {
            return ItemStack.f_41583_;
         } else {
            if (armorBySlot.isEmpty()) {
               this.armorByPlayer.remove(playerId);
            }

            this.m_77762_();
            return armor;
         }
      }
   }

   EnumMap<EquipmentSlot, ItemStack> takeAll(UUID playerId) {
      EnumMap<EquipmentSlot, ItemStack> armorBySlot = this.armorByPlayer.remove(playerId);
      if (armorBySlot == null) {
         return new EnumMap<>(EquipmentSlot.class);
      } else {
         this.m_77762_();
         return armorBySlot;
      }
   }

   boolean contains(UUID playerId, EquipmentSlot slot) {
      EnumMap<EquipmentSlot, ItemStack> armorBySlot = this.armorByPlayer.get(playerId);
      return armorBySlot != null && armorBySlot.containsKey(slot);
   }

   boolean containsAny(UUID playerId) {
      EnumMap<EquipmentSlot, ItemStack> armorBySlot = this.armorByPlayer.get(playerId);
      return armorBySlot != null && !armorBySlot.isEmpty();
   }

   public CompoundTag m_7176_(CompoundTag tag) {
      ListTag entries = new ListTag();

      for (Entry<UUID, EnumMap<EquipmentSlot, ItemStack>> playerEntry : this.armorByPlayer.entrySet()) {
         for (Entry<EquipmentSlot, ItemStack> armorEntry : playerEntry.getValue().entrySet()) {
            ItemStack armor = armorEntry.getValue();
            if (armor != null && !armor.m_41619_()) {
               CompoundTag entryTag = new CompoundTag();
               entryTag.m_128362_("Player", playerEntry.getKey());
               entryTag.m_128359_("Slot", armorEntry.getKey().m_20751_());
               entryTag.m_128365_("Armor", armor.m_41739_(new CompoundTag()));
               entries.add(entryTag);
            }
         }
      }

      tag.m_128365_("Entries", entries);
      return tag;
   }

   static PhaseArmorVaultSavedData load(CompoundTag tag) {
      PhaseArmorVaultSavedData data = new PhaseArmorVaultSavedData();
      if (!tag.m_128425_("Entries", 9)) {
         return data;
      } else {
         ListTag entries = tag.m_128437_("Entries", 10);

         for (int i = 0; i < entries.size(); i++) {
            CompoundTag entryTag = entries.m_128728_(i);
            if (entryTag.m_128403_("Player") && entryTag.m_128425_("Armor", 10)) {
               EquipmentSlot slot = entryTag.m_128425_("Slot", 8) ? armorSlot(entryTag.m_128461_("Slot")) : EquipmentSlot.CHEST;
               if (slot != null) {
                  ItemStack armor = ItemStack.m_41712_(entryTag.m_128469_("Armor"));
                  if (!armor.m_41619_()) {
                     data.armorByPlayer.computeIfAbsent(entryTag.m_128342_("Player"), ignored -> new EnumMap<>(EquipmentSlot.class)).putIfAbsent(slot, armor);
                  }
               }
            }
         }

         return data;
      }
   }

   private static boolean isArmorSlot(EquipmentSlot slot) {
      return slot != null && slot.m_20743_() == Type.ARMOR;
   }

   @Nullable
   private static EquipmentSlot armorSlot(String name) {
      return switch (name) {
         case "head" -> EquipmentSlot.HEAD;
         case "chest" -> EquipmentSlot.CHEST;
         case "legs" -> EquipmentSlot.LEGS;
         case "feet" -> EquipmentSlot.FEET;
         default -> null;
      };
   }
}
