package com.moakiee.ae2lt.me.cell;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

public final class InfiniteCellSavedData extends SavedData {
   private static final String DATA_NAME = "ae2lt_infinite_cells";
   private final Map<UUID, CompoundTag> cells = new HashMap<>();
   private final transient Map<UUID, IndexedStorage> storageCache = new HashMap<>();

   public static InfiniteCellSavedData get(MinecraftServer server) {
      return (InfiniteCellSavedData)server.m_129783_().m_8895_().m_164861_(InfiniteCellSavedData::load, InfiniteCellSavedData::new, "ae2lt_infinite_cells");
   }

   @Nullable
   public static InfiniteCellSavedData getOrNull() {
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      return server != null ? get(server) : null;
   }

   public IndexedStorage getOrCreateStorage(UUID id) {
      IndexedStorage cached = this.storageCache.get(id);
      if (cached != null) {
         return cached;
      } else {
         IndexedStorage storage = new IndexedStorage();
         CompoundTag data = this.cells.get(id);
         if (data != null) {
            storage.load(data);
         }

         this.storageCache.put(id, storage);
         return storage;
      }
   }

   public void persistStorage(UUID id, IndexedStorage storage) {
      if (storage != null) {
         this.storageCache.put(id, storage);
         CompoundTag lastRoot = this.cells.get(id);
         CompoundTag data = storage.persist(lastRoot);
         this.cells.put(id, data);
         this.m_77762_();
      }
   }

   public void markStorageDirty(UUID id, IndexedStorage storage) {
      if (id != null && storage != null) {
         this.storageCache.put(id, storage);
         this.m_77762_();
      }
   }

   public void removeCell(UUID id) {
      boolean had = this.cells.remove(id) != null;
      this.storageCache.remove(id);
      if (had) {
         this.m_77762_();
      }
   }

   private static InfiniteCellSavedData load(CompoundTag tag) {
      InfiniteCellSavedData data = new InfiniteCellSavedData();
      CompoundTag cellsTag = tag.m_128469_("cells");

      for (String key : cellsTag.m_128431_()) {
         try {
            data.cells.put(UUID.fromString(key), cellsTag.m_128469_(key));
         } catch (IllegalArgumentException var6) {
         }
      }

      return data;
   }

   public CompoundTag m_7176_(CompoundTag tag) {
      for (Entry<UUID, IndexedStorage> entry : this.storageCache.entrySet()) {
         if (entry.getValue().needsPersist()) {
            CompoundTag lastRoot = this.cells.get(entry.getKey());
            CompoundTag data = entry.getValue().persist(lastRoot);
            this.cells.put(entry.getKey(), data);
         }
      }

      CompoundTag cellsTag = new CompoundTag();

      for (Entry<UUID, CompoundTag> entryx : this.cells.entrySet()) {
         cellsTag.m_128365_(entryx.getKey().toString(), (Tag)entryx.getValue());
      }

      tag.m_128365_("cells", cellsTag);
      return tag;
   }
}
