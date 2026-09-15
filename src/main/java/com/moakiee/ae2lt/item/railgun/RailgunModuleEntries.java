package com.moakiee.ae2lt.item.railgun;

import com.moakiee.ae2lt.device.capability.DeviceCapability;
import com.moakiee.ae2lt.device.module.OverloadDeviceModuleItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public record RailgunModuleEntries(List<ItemStack> entries) {
   public static final RailgunModuleEntries EMPTY = new RailgunModuleEntries(List.of());

   public RailgunModuleEntries(List<ItemStack> entries) {
      entries = compact(entries);
      this.entries = entries;
   }

   public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      ListTag list = new ListTag();

      for (ItemStack stack : this.entries) {
         list.add(stack.m_41739_(new CompoundTag()));
      }

      tag.m_128365_("entries", list);
      return tag;
   }

   public static RailgunModuleEntries load(CompoundTag tag) {
      List<ItemStack> stacks = new ArrayList<>();
      ListTag list = tag.m_128437_("entries", 10);

      for (int i = 0; i < list.size(); i++) {
         ItemStack stack = ItemStack.m_41712_(list.m_128728_(i));
         if (!stack.m_41619_()) {
            stacks.add(stack);
         }
      }

      return new RailgunModuleEntries(stacks);
   }

   public boolean hasCore() {
      return this.getCount(RailgunModuleType.CORE) > 0;
   }

   public boolean hasOverloadExecution() {
      return this.getCount(RailgunModuleType.OVERLOAD_EXECUTION) > 0;
   }

   public boolean hasMultidimensionalExecution() {
      return this.getCount(RailgunModuleType.MULTIDIMENSIONAL_EXECUTION) > 0;
   }

   public boolean hasAnyExecution() {
      return this.hasOverloadExecution() || this.hasMultidimensionalExecution();
   }

   public int computeCount() {
      return this.getCount(RailgunModuleType.COMPUTE);
   }

   public int accelerationCount() {
      return this.getCount(RailgunModuleType.ACCELERATION);
   }

   public int getCount(RailgunModuleType type) {
      return this.getCount(type.m_7912_());
   }

   public int getCount(String typeId) {
      int total = 0;

      for (ItemStack stack : this.entries) {
         if (typeId.equals(typeId(stack))) {
            total += stack.m_41613_();
         }
      }

      return total;
   }

   public ItemStack first(RailgunModuleType type) {
      String typeId = type.m_7912_();

      for (ItemStack stack : this.entries) {
         if (typeId.equals(typeId(stack))) {
            return stack.m_255036_(1);
         }
      }

      return ItemStack.f_41583_;
   }

   public List<ItemStack> unitStacks(RailgunModuleType type) {
      String typeId = type.m_7912_();
      ArrayList<ItemStack> result = new ArrayList<>();

      for (ItemStack stack : this.entries) {
         if (typeId.equals(typeId(stack))) {
            for (int i = 0; i < stack.m_41613_(); i++) {
               result.add(stack.m_255036_(1));
            }
         }
      }

      return result;
   }

   public Stream<ItemStack> installedModuleStacks() {
      ArrayList<ItemStack> result = new ArrayList<>();

      for (ItemStack stack : this.entries) {
         for (int i = 0; i < stack.m_41613_(); i++) {
            result.add(stack.m_255036_(1));
         }
      }

      return result.stream();
   }

   public List<DeviceCapability> capabilities() {
      List<DeviceCapability> out = new ArrayList<>();
      this.installedModuleStacks().forEach(stack -> append(out, stack));
      return out;
   }

   public static RailgunModuleEntries fromSlotStacks(List<ItemStack> stacks) {
      return new RailgunModuleEntries(stacks);
   }

   public static String typeId(ItemStack stack) {
      if (stack != null && !stack.m_41619_()) {
         return stack.m_41720_() instanceof OverloadDeviceModuleItem module ? module.moduleTypeId(stack) : "";
      } else {
         return "";
      }
   }

   private static List<ItemStack> compact(List<ItemStack> source) {
      if (source != null && !source.isEmpty()) {
         Map<String, ItemStack> merged = new LinkedHashMap<>();

         for (ItemStack stack : source) {
            String typeId = typeId(stack);
            if (!typeId.isBlank()) {
               int count = Math.max(stack.m_41613_(), 1);
               ItemStack existing = merged.get(typeId);
               if (existing == null) {
                  merged.put(typeId, stack.m_255036_(count));
               } else {
                  existing.m_41769_(count);
               }
            }
         }

         return List.copyOf(merged.values());
      } else {
         return List.of();
      }
   }

   private static void append(List<DeviceCapability> out, ItemStack stack) {
      if (!stack.m_41619_()) {
         if (stack.m_41720_() instanceof OverloadDeviceModuleItem module) {
            out.addAll(module.capabilities(stack));
         }
      }
   }
}
