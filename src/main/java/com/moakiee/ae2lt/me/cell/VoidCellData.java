package com.moakiee.ae2lt.me.cell;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.moakiee.ae2lt.util.ItemStackTagSupport;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2LongMap.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public final class VoidCellData {
   static final String ROOT_TAG = "ae2lt:void_cell";
   private static final String TAG_VERSION = "version";
   private static final String TAG_MODE = "mode";
   private static final String TAG_ENERGY = "energy";
   private static final String TAG_INVENTORY = "inventory";
   private static final int VERSION = 1;

   private VoidCellData() {
   }

   public static VoidCellData.State read(ItemStack stack) {
      CompoundTag data = getDataTag(stack);
      VoidCellMode mode = VoidCellMode.fromSerializedName(data.m_128461_("mode"));
      double energy = Math.max(0.0, data.m_128459_("energy"));
      Object2LongOpenHashMap<AEKey> inventory = new Object2LongOpenHashMap();
      ListTag entries = data.m_128437_("inventory", 10);

      for (int i = 0; i < entries.size(); i++) {
         GenericStack genericStack = GenericStack.readTag(entries.m_128728_(i));
         if (genericStack != null && genericStack.amount() > 0L) {
            inventory.addTo(genericStack.what(), genericStack.amount());
         }
      }

      return new VoidCellData.State(mode, energy, inventory);
   }

   public static VoidCellMode readMode(ItemStack stack) {
      return VoidCellMode.fromSerializedName(getDataTag(stack).m_128461_("mode"));
   }

   public static void writeMode(ItemStack stack, VoidCellMode mode) {
      ItemStackTagSupport.updateTag(stack, root -> {
         CompoundTag data = root.m_128425_("ae2lt:void_cell", 10) ? root.m_128469_("ae2lt:void_cell") : new CompoundTag();
         if (mode == VoidCellMode.TRASH) {
            data.m_128473_("mode");
         } else {
            data.m_128359_("mode", mode.m_7912_());
         }

         attachOrRemove(root, data);
      });
   }

   public static void write(ItemStack stack, VoidCellMode mode, double energy, Object2LongMap<AEKey> inventory) {
      ItemStackTagSupport.updateTag(stack, root -> {
         CompoundTag data = new CompoundTag();
         if (mode != VoidCellMode.TRASH) {
            data.m_128359_("mode", mode.m_7912_());
         }

         if (energy > 0.0) {
            data.m_128347_("energy", energy);
         }

         ListTag entries = new ListTag();
         ObjectIterator var7 = inventory.object2LongEntrySet().iterator();

         while (var7.hasNext()) {
            Entry<AEKey> entry = (Entry<AEKey>)var7.next();
            if (entry.getLongValue() > 0L) {
               entries.add(GenericStack.writeTag(new GenericStack((AEKey)entry.getKey(), entry.getLongValue())));
            }
         }

         if (!entries.isEmpty()) {
            data.m_128365_("inventory", entries);
         }

         attachOrRemove(root, data);
      });
   }

   private static CompoundTag getDataTag(ItemStack stack) {
      CompoundTag root = stack.m_41783_();
      return root != null && root.m_128425_("ae2lt:void_cell", 10) ? root.m_128469_("ae2lt:void_cell") : new CompoundTag();
   }

   private static void attachOrRemove(CompoundTag root, CompoundTag data) {
      data.m_128473_("version");
      if (data.m_128456_()) {
         root.m_128473_("ae2lt:void_cell");
      } else {
         data.m_128405_("version", 1);
         root.m_128365_("ae2lt:void_cell", data);
      }
   }

   public static record State(VoidCellMode mode, double energy, Object2LongOpenHashMap<AEKey> inventory) {
   }
}
