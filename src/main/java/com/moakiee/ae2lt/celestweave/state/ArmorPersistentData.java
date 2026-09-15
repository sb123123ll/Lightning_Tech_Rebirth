package com.moakiee.ae2lt.celestweave.state;

import com.moakiee.ae2lt.celestweave.ArmorEnergyModuleItem;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import com.moakiee.ae2lt.device.module.OverloadDeviceModuleItem;
import com.moakiee.ae2lt.registry.ModDataComponents;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class ArmorPersistentData {
   private ArmorPersistentData() {
   }

   public static UUID ensureArmorId(ItemStack armor) {
      CelestweaveModuleContainer container = container(armor);
      if (container.armorId().isPresent()) {
         return container.armorId().get();
      } else {
         UUID created = UUID.randomUUID();
         setContainer(armor, container.withArmorId(created));
         return created;
      }
   }

   public static Optional<UUID> armorId(ItemStack armor) {
      return container(armor).armorId();
   }

   public static long getCachedEnergyModuleCapacityFe(ItemStack armor) {
      return Math.max(0L, container(armor).energyModuleCapacityFe().orElse(0L));
   }

   public static boolean hasCachedEnergyModuleCapacityFe(ItemStack armor) {
      return container(armor).energyModuleCapacityFe().isPresent();
   }

   public static void setCachedEnergyModuleCapacityFe(ItemStack armor, long capacityFe) {
      if (armor != null && !armor.m_41619_()) {
         setContainer(armor, container(armor).withCapacity(Optional.of(Math.max(0L, capacityFe))));
      }
   }

   public static ItemStack structuralCore(ItemStack armor) {
      return ModDataComponents.CELESTWEAVE_STRUCTURAL_CORE.getOrDefault(armor, ItemStack.f_41583_).m_255036_(1);
   }

   public static void setStructuralCore(ItemStack armor, ItemStack stack) {
      if (armor != null && !armor.m_41619_()) {
         if (stack != null && !stack.m_41619_()) {
            ModDataComponents.CELESTWEAVE_STRUCTURAL_CORE.set(armor, stack.m_255036_(1));
         } else {
            ModDataComponents.CELESTWEAVE_STRUCTURAL_CORE.remove(armor);
         }
      }
   }

   public static boolean hasStructuralCore(ItemStack armor) {
      return !structuralCore(armor).m_41619_();
   }

   public static List<ItemStack> loadModuleStacks(ItemStack armor, Provider registries) {
      List<ItemStack> modules = container(armor).modules();
      if (modules.isEmpty()) {
         return List.of();
      } else {
         List<ItemStack> result = new ArrayList<>(modules.size());

         for (ItemStack stack : modules) {
            if (!stack.m_41619_()) {
               result.add(stack.m_41777_());
            }
         }

         return List.copyOf(result);
      }
   }

   public static boolean hasInstalledSubmodule(ItemStack armor, String submoduleId) {
      if (submoduleId != null && !submoduleId.isBlank()) {
         for (ItemStack stack : container(armor).modules()) {
            if (submoduleId.equals(resolveModuleTypeId(stack))) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static void saveModuleStacks(ItemStack armor, Provider registries, List<ItemStack> stacks) {
      if (armor != null && !armor.m_41619_()) {
         LinkedHashMap<String, ItemStack> merged = new LinkedHashMap<>();

         for (ItemStack stack : stacks) {
            if (stack != null && !stack.m_41619_()) {
               String id = resolveModuleTypeId(stack);
               if (!id.isBlank()) {
                  merged.compute(id, (ignored, existing) -> {
                     if (existing == null) {
                        return stack.m_41777_();
                     } else {
                        existing.m_41769_(stack.m_41613_());
                        return (ItemStack)existing;
                     }
                  });
               }
            }
         }

         List<ItemStack> out = new ArrayList<>();
         long energyCapacityFe = 0L;

         for (ItemStack stackx : merged.values()) {
            ItemStack writtenStack = stackx.m_255036_(Math.max(1, stackx.m_41613_()));
            out.add(writtenStack);
            energyCapacityFe = Math.max(energyCapacityFe, energyCapacityFe(writtenStack));
         }

         Optional<Long> capacity = out.isEmpty() ? Optional.empty() : Optional.of(energyCapacityFe);
         setContainer(armor, container(armor).withModules(out, capacity));
      }
   }

   public static boolean getToggle(ItemStack armor, String key, boolean defaultValue) {
      Boolean value = container(armor).toggles().get(key);
      return value == null ? defaultValue : value;
   }

   public static void setToggle(ItemStack armor, String key, boolean value, boolean defaultValue) {
      if (key != null && !key.isBlank()) {
         CelestweaveModuleContainer container = container(armor);
         Map<String, Boolean> toggles = new LinkedHashMap<>(container.toggles());
         if (value == defaultValue) {
            toggles.remove(key);
         } else {
            toggles.put(key, value);
         }

         setContainer(armor, container.withToggles(toggles));
      }
   }

   public static CompoundTag getSubmoduleData(ItemStack armor, String submoduleId) {
      CompoundTag data = container(armor).submoduleData().get(submoduleId);
      return data == null ? new CompoundTag() : data.m_6426_();
   }

   public static void setSubmoduleData(ItemStack armor, String submoduleId, CompoundTag data) {
      CelestweaveModuleContainer container = container(armor);
      Map<String, CompoundTag> allData = new LinkedHashMap<>(container.submoduleData());
      if (data != null && !data.m_128456_()) {
         allData.put(submoduleId, data.m_6426_());
      } else {
         allData.remove(submoduleId);
      }

      setContainer(armor, container.withSubmoduleData(allData));
   }

   private static CelestweaveModuleContainer container(ItemStack armor) {
      return armor != null && !armor.m_41619_()
         ? ModDataComponents.CELESTWEAVE_MODULES.getOrDefault(armor, CelestweaveModuleContainer.EMPTY)
         : CelestweaveModuleContainer.EMPTY;
   }

   private static void setContainer(ItemStack armor, CelestweaveModuleContainer container) {
      if (armor != null && !armor.m_41619_()) {
         ModDataComponents.CELESTWEAVE_MODULES.set(armor, container);
      }
   }

   private static String resolveModuleTypeId(ItemStack stack) {
      return stack != null && !stack.m_41619_() && stack.m_41720_() instanceof OverloadDeviceModuleItem provider ? provider.moduleTypeId(stack) : "";
   }

   private static long energyCapacityFe(ItemStack stack) {
      if (stack.m_41619_()) {
         return 0L;
      } else if (stack.m_41720_() instanceof ArmorEnergyModuleItem energyModule) {
         return energyModule.armorCapacityFe();
      } else if (stack.m_41720_() instanceof OverloadDeviceModuleItem provider) {
         long capacity = 0L;

         for (DeviceCapability capability : provider.capabilities(stack.m_255036_(1))) {
            if (capability instanceof DeviceCapability.EnergyCapacity energyCapacity) {
               capacity = Math.max(capacity, energyCapacity.fe());
            }
         }

         return capacity;
      } else {
         return 0L;
      }
   }
}
