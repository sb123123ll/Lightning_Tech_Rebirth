package com.moakiee.ae2lt.logic.tianshu.maintenance;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.IntSupplier;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public final class InventoryMaintenanceRepository {
   private static final String TAG_RULES = "Rules";
   private final IntSupplier capacity;
   private final LinkedHashMap<AEKey, InventoryMaintenanceRule> rules = new LinkedHashMap<>();

   public InventoryMaintenanceRepository(IntSupplier capacity) {
      this.capacity = capacity;
   }

   public int capacity() {
      return Math.max(0, this.capacity.getAsInt());
   }

   public int size() {
      return this.rules.size();
   }

   public List<InventoryMaintenanceRule> rules() {
      return List.copyOf(this.rules.values());
   }

   public List<InventoryMaintenanceRule> rules(int limit) {
      return limit <= 0 ? List.of() : this.rules.values().stream().limit((long)limit).toList();
   }

   public List<InventoryMaintenanceRule> activeRules() {
      return this.rules.size() > this.capacity() ? List.of() : this.rules.values().stream().limit((long)this.capacity()).toList();
   }

   public InventoryMaintenanceRule get(AEKey key) {
      return this.rules.get(key);
   }

   public InventoryMaintenanceRule getById(UUID id) {
      if (id == null) {
         return null;
      } else {
         for (InventoryMaintenanceRule rule : this.rules.values()) {
            if (id.equals(rule.id())) {
               return rule;
            }
         }

         return null;
      }
   }

   public InventoryMaintenanceRepository.PutResult put(InventoryMaintenanceRule rule) {
      if (rule == null) {
         return InventoryMaintenanceRepository.PutResult.INVALID;
      } else if (this.rules.size() > this.capacity()) {
         return InventoryMaintenanceRepository.PutResult.FULL;
      } else if (!this.rules.containsKey(rule.key()) && this.rules.size() >= this.capacity()) {
         return this.capacity() <= 0 ? InventoryMaintenanceRepository.PutResult.UNAVAILABLE : InventoryMaintenanceRepository.PutResult.FULL;
      } else {
         boolean update = this.rules.containsKey(rule.key());
         this.rules.put(rule.key(), rule);
         return update ? InventoryMaintenanceRepository.PutResult.UPDATED : InventoryMaintenanceRepository.PutResult.ADDED;
      }
   }

   public boolean remove(AEKey key) {
      return this.rules.remove(key) != null;
   }

   public void writeTo(CompoundTag parent, Provider registries) {
      ListTag list = new ListTag();

      for (InventoryMaintenanceRule rule : this.rules.values()) {
         CompoundTag tag = new CompoundTag();
         tag.m_128362_("Id", rule.id());
         tag.m_128365_("Key", GenericStack.writeTag(new GenericStack(rule.key(), 1L)));
         tag.m_128356_("Lower", rule.lowerThreshold());
         tag.m_128356_("Upper", rule.upperThreshold());
         tag.m_128356_("PerJob", rule.amountPerJob());
         tag.m_128379_("Enabled", rule.enabled());
         tag.m_128379_("Replenishing", rule.replenishing());
         if (rule.activeCraftingId() != null) {
            tag.m_128362_("CraftingId", rule.activeCraftingId());
         }

         list.add(tag);
      }

      parent.m_128365_("Rules", list);
   }

   public void readFrom(CompoundTag parent, Provider registries) {
      this.rules.clear();
      ListTag list = parent.m_128437_("Rules", 10);

      for (int i = 0; i < list.size(); i++) {
         try {
            CompoundTag tag = list.m_128728_(i);
            GenericStack keyStack = GenericStack.readTag(tag.m_128469_("Key"));
            if (keyStack != null && tag.m_128403_("Id")) {
               InventoryMaintenanceRule rule = new InventoryMaintenanceRule(
                  tag.m_128342_("Id"),
                  keyStack.what(),
                  tag.m_128454_("Lower"),
                  tag.m_128454_("Upper"),
                  tag.m_128454_("PerJob"),
                  tag.m_128471_("Enabled"),
                  tag.m_128471_("Replenishing"),
                  tag.m_128403_("CraftingId") ? tag.m_128342_("CraftingId") : null
               );
               this.rules.put(rule.key(), rule);
            }
         } catch (RuntimeException var8) {
         }
      }
   }

   public static enum PutResult {
      ADDED,
      UPDATED,
      FULL,
      UNAVAILABLE,
      INVALID;
   }
}
