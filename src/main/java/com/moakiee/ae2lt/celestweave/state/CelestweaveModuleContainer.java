package com.moakiee.ae2lt.celestweave.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public record CelestweaveModuleContainer(
   Optional<UUID> armorId, List<ItemStack> modules, Map<String, Boolean> toggles, Map<String, CompoundTag> submoduleData, Optional<Long> energyModuleCapacityFe
) {
   public static final CelestweaveModuleContainer EMPTY = new CelestweaveModuleContainer(Optional.empty(), List.of(), Map.of(), Map.of(), Optional.empty());

   public CelestweaveModuleContainer(
      Optional<UUID> armorId,
      List<ItemStack> modules,
      Map<String, Boolean> toggles,
      Map<String, CompoundTag> submoduleData,
      Optional<Long> energyModuleCapacityFe
   ) {
      modules = copyModules(modules);
      toggles = toggles == null ? Map.of() : Map.copyOf(toggles);
      submoduleData = copySubmoduleData(submoduleData);
      this.armorId = armorId;
      this.modules = modules;
      this.toggles = toggles;
      this.submoduleData = submoduleData;
      this.energyModuleCapacityFe = energyModuleCapacityFe;
   }

   public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      this.armorId.ifPresent(id -> tag.m_128362_("armor_id", id));
      ListTag moduleList = new ListTag();

      for (ItemStack stack : this.modules) {
         moduleList.add(stack.m_41739_(new CompoundTag()));
      }

      tag.m_128365_("modules", moduleList);
      CompoundTag togglesTag = new CompoundTag();
      this.toggles.forEach(togglesTag::m_128379_);
      tag.m_128365_("toggles", togglesTag);
      CompoundTag dataTag = new CompoundTag();
      this.submoduleData.forEach(dataTag::m_128365_);
      tag.m_128365_("submodule_data", dataTag);
      this.energyModuleCapacityFe.ifPresent(v -> tag.m_128356_("energy_capacity_fe", v));
      return tag;
   }

   public static CelestweaveModuleContainer load(CompoundTag tag) {
      Optional<UUID> armorId = tag.m_128403_("armor_id") ? Optional.of(tag.m_128342_("armor_id")) : Optional.empty();
      List<ItemStack> moduleStacks = new ArrayList<>();
      ListTag moduleList = tag.m_128437_("modules", 10);

      for (int i = 0; i < moduleList.size(); i++) {
         ItemStack stack = ItemStack.m_41712_(moduleList.m_128728_(i));
         if (!stack.m_41619_()) {
            moduleStacks.add(stack);
         }
      }

      Map<String, Boolean> toggles = new HashMap<>();
      CompoundTag togglesTag = tag.m_128469_("toggles");

      for (String key : togglesTag.m_128431_()) {
         toggles.put(key, togglesTag.m_128471_(key));
      }

      Map<String, CompoundTag> submoduleData = new HashMap<>();
      CompoundTag dataTag = tag.m_128469_("submodule_data");

      for (String key : dataTag.m_128431_()) {
         submoduleData.put(key, dataTag.m_128469_(key).m_6426_());
      }

      Optional<Long> capacity = tag.m_128441_("energy_capacity_fe") ? Optional.of(tag.m_128454_("energy_capacity_fe")) : Optional.empty();
      return new CelestweaveModuleContainer(armorId, moduleStacks, toggles, submoduleData, capacity);
   }

   public List<ItemStack> modules() {
      return copyModules(this.modules);
   }

   public Map<String, CompoundTag> submoduleData() {
      return copySubmoduleData(this.submoduleData);
   }

   public CelestweaveModuleContainer withArmorId(UUID id) {
      return new CelestweaveModuleContainer(Optional.ofNullable(id), this.modules, this.toggles, this.submoduleData, this.energyModuleCapacityFe);
   }

   public CelestweaveModuleContainer withModules(List<ItemStack> newModules, Optional<Long> capacityFe) {
      return new CelestweaveModuleContainer(this.armorId, newModules, this.toggles, this.submoduleData, capacityFe);
   }

   public CelestweaveModuleContainer withToggles(Map<String, Boolean> newToggles) {
      return new CelestweaveModuleContainer(this.armorId, this.modules, newToggles, this.submoduleData, this.energyModuleCapacityFe);
   }

   public CelestweaveModuleContainer withSubmoduleData(Map<String, CompoundTag> newData) {
      return new CelestweaveModuleContainer(this.armorId, this.modules, this.toggles, newData, this.energyModuleCapacityFe);
   }

   public CelestweaveModuleContainer withCapacity(Optional<Long> capacityFe) {
      return new CelestweaveModuleContainer(this.armorId, this.modules, this.toggles, this.submoduleData, capacityFe);
   }

   private static List<ItemStack> copyModules(List<ItemStack> modules) {
      return modules != null && !modules.isEmpty()
         ? modules.stream().filter(stack -> stack != null && !stack.m_41619_()).<ItemStack>map(ItemStack::m_41777_).toList()
         : List.of();
   }

   private static Map<String, CompoundTag> copySubmoduleData(Map<String, CompoundTag> data) {
      if (data != null && !data.isEmpty()) {
         Map<String, CompoundTag> copy = new LinkedHashMap<>();

         for (Entry<String, CompoundTag> entry : data.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isBlank() && entry.getValue() != null) {
               copy.put(entry.getKey(), entry.getValue().m_6426_());
            }
         }

         return Map.copyOf(copy);
      } else {
         return Map.of();
      }
   }
}
