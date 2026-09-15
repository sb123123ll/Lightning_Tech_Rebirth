package com.moakiee.ae2lt.logic;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

public class PatternStorageSavedData extends SavedData {
   private static final String DATA_NAME = "ae2lt_patterns";
   private static final String TAG_ENTRIES = "Entries";
   private static final String TAG_POS = "Pos";
   private static final String TAG_SLOTS = "Slots";
   private static final String TAG_SLOT = "Slot";
   private static final String TAG_ITEM = "Item";
   private final Long2ObjectOpenHashMap<ItemStack[]> storage = new Long2ObjectOpenHashMap();

   public static PatternStorageSavedData get(ServerLevel level) {
      return (PatternStorageSavedData)level.m_8895_().m_164861_(PatternStorageSavedData::load, PatternStorageSavedData::new, "ae2lt_patterns");
   }

   public ItemStack[] get(long pos) {
      return (ItemStack[])this.storage.get(pos);
   }

   public void set(long pos, ItemStack[] patterns) {
      this.storage.put(pos, patterns);
      this.m_77762_();
   }

   public void remove(long pos) {
      if (this.storage.remove(pos) != null) {
         this.m_77762_();
      }
   }

   public CompoundTag m_7176_(CompoundTag tag) {
      ListTag entries = new ListTag();
      ObjectIterator var3 = this.storage.entrySet().iterator();

      while (var3.hasNext()) {
         Entry<Long, ItemStack[]> entry = (Entry<Long, ItemStack[]>)var3.next();
         CompoundTag entryTag = new CompoundTag();
         entryTag.m_128356_("Pos", entry.getKey());
         ListTag slotsTag = new ListTag();
         ItemStack[] patterns = entry.getValue();

         for (int i = 0; i < patterns.length; i++) {
            if (patterns[i] != null && !patterns[i].m_41619_()) {
               CompoundTag slotTag = new CompoundTag();
               slotTag.m_128405_("Slot", i);
               slotTag.m_128365_("Item", patterns[i].m_41739_(new CompoundTag()));
               slotsTag.add(slotTag);
            }
         }

         entryTag.m_128365_("Slots", slotsTag);
         entries.add(entryTag);
      }

      tag.m_128365_("Entries", entries);
      return tag;
   }

   private static PatternStorageSavedData load(CompoundTag tag) {
      PatternStorageSavedData data = new PatternStorageSavedData();
      if (tag.m_128425_("Entries", 9)) {
         ListTag entries = tag.m_128437_("Entries", 10);

         for (int i = 0; i < entries.size(); i++) {
            CompoundTag entryTag = entries.m_128728_(i);
            long pos = entryTag.m_128454_("Pos");
            ListTag slotsTag = entryTag.m_128437_("Slots", 10);
            int maxSlot = 0;

            for (int j = 0; j < slotsTag.size(); j++) {
               maxSlot = Math.max(maxSlot, slotsTag.m_128728_(j).m_128451_("Slot") + 1);
            }

            ItemStack[] patterns = new ItemStack[maxSlot];

            for (int j = 0; j < maxSlot; j++) {
               patterns[j] = ItemStack.f_41583_;
            }

            for (int j = 0; j < slotsTag.size(); j++) {
               CompoundTag slotTag = slotsTag.m_128728_(j);
               int slot = slotTag.m_128451_("Slot");
               patterns[slot] = ItemStack.m_41712_(slotTag.m_128469_("Item"));
            }

            data.storage.put(pos, patterns);
         }
      }

      return data;
   }
}
