package com.moakiee.ae2lt.celestweave.service;

import com.moakiee.ae2lt.celestweave.ArmorPart;
import com.moakiee.ae2lt.celestweave.CelestweaveArmorState;
import com.moakiee.ae2lt.celestweave.module.CelestweaveArmorSubmodule;
import com.moakiee.ae2lt.celestweave.module.CelestweaveArmorSubmoduleItem;
import com.moakiee.ae2lt.celestweave.phase.CelestweaveEquipmentAccess;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import com.moakiee.ae2lt.device.module.OverloadDeviceModuleItem;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ArmorCapabilityCollector {
   private static final ConcurrentHashMap<UUID, ArmorCapabilityCollector.CachedCapabilities> CACHE = new ConcurrentHashMap<>();
   private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

   private ArmorCapabilityCollector() {
   }

   public static List<ArmorCapabilityCollector.ActiveCapability> collectPerInstalledStack(Player player) {
      return collect(player, false);
   }

   public static List<ArmorCapabilityCollector.ActiveCapability> collectPerInstalledUnit(Player player) {
      return collect(player, true);
   }

   public static void clearCache(Player player) {
      CACHE.remove(player.m_20148_());
   }

   private static List<ArmorCapabilityCollector.ActiveCapability> collect(Player player, boolean expandByCount) {
      return CACHE.computeIfAbsent(player.m_20148_(), ignored -> new ArmorCapabilityCollector.CachedCapabilities()).get(player, expandByCount);
   }

   private static List<ArmorCapabilityCollector.ActiveCapability> collectUncached(Player player, boolean expandByCount) {
      ArrayList<ArmorCapabilityCollector.ActiveCapability> out = new ArrayList<>();

      for (EquipmentSlot slot : ARMOR_SLOTS) {
         ItemStack armor = CelestweaveEquipmentAccess.findArmor(player, slot);
         if (!armor.m_41619_()) {
            CelestweaveArmorState.Snapshot snapshot = CelestweaveArmorState.snapshot(player, armor, player.m_9236_().m_9598_(), true);
            if (snapshot.hasCore()) {
               for (ItemStack moduleStack : CelestweaveArmorState.loadModuleStacks(armor, player.m_9236_().m_9598_())) {
                  if (moduleStack.m_41720_() instanceof OverloadDeviceModuleItem module) {
                     ArmorPart armorPart = CelestweaveArmorState.armorPart(armor);
                     if (module.accepts(armorPart.deviceKind(), armorPart.moduleSlot())) {
                        int iterations = expandByCount ? Math.max(1, moduleStack.m_41613_()) : 1;

                        for (int i = 0; i < iterations; i++) {
                           ItemStack unit = moduleStack.m_255036_(1);
                           if (moduleStack.m_41720_() instanceof CelestweaveArmorSubmoduleItem submoduleProvider) {
                              submoduleProvider.collectSubmodules(unit, submodule -> {
                                 if (submodule != null && isSubmoduleActiveForSide(player, armor, submodule)) {
                                    for (DeviceCapability capabilityx : module.capabilities(unit)) {
                                       out.add(new ArmorCapabilityCollector.ActiveCapability(armor, submodule.id(), capabilityx));
                                    }
                                 }
                              });
                           } else {
                              for (DeviceCapability capability : module.capabilities(unit)) {
                                 out.add(new ArmorCapabilityCollector.ActiveCapability(armor, module.moduleTypeId(unit), capability));
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return List.copyOf(out);
   }

   private static boolean isSubmoduleActiveForSide(Player player, ItemStack armor, CelestweaveArmorSubmodule submodule) {
      return player.m_9236_().m_5776_()
         ? CelestweaveArmorState.isSubmoduleActiveClient(armor, submodule)
         : CelestweaveArmorState.isSubmoduleRuntimeActive(armor, submodule.id());
   }

   public static record ActiveCapability(ItemStack armor, String submoduleId, DeviceCapability capability) {
   }

   private static final class CachedCapabilities {
      private long gameTime = Long.MIN_VALUE;
      private boolean clientSide;
      private ResourceKey<Level> dimension;
      private List<ArmorCapabilityCollector.ActiveCapability> perInstalledStack;
      private List<ArmorCapabilityCollector.ActiveCapability> perInstalledUnit;

      synchronized List<ArmorCapabilityCollector.ActiveCapability> get(Player player, boolean expandByCount) {
         long currentGameTime = player.m_9236_().m_46467_();
         boolean currentClientSide = player.m_9236_().m_5776_();
         ResourceKey<Level> currentDimension = player.m_9236_().m_46472_();
         if (this.gameTime != currentGameTime || this.clientSide != currentClientSide || !currentDimension.equals(this.dimension)) {
            this.gameTime = currentGameTime;
            this.clientSide = currentClientSide;
            this.dimension = currentDimension;
            this.perInstalledStack = null;
            this.perInstalledUnit = null;
         }

         if (expandByCount) {
            if (this.perInstalledUnit == null) {
               this.perInstalledUnit = ArmorCapabilityCollector.collectUncached(player, true);
            }

            return this.perInstalledUnit;
         } else {
            if (this.perInstalledStack == null) {
               this.perInstalledStack = ArmorCapabilityCollector.collectUncached(player, false);
            }

            return this.perInstalledStack;
         }
      }
   }
}
