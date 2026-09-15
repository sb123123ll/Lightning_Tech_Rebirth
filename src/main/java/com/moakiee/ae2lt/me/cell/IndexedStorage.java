package com.moakiee.ae2lt.me.cell;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.KeyCounter;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.Arrays;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import org.jetbrains.annotations.Nullable;

final class IndexedStorage {
   private static final int INITIAL_CAPACITY = 256;
   private static final int COMPACT_THRESHOLD = 2;
   private final Object2IntOpenHashMap<AEKey> keyToId = new Object2IntOpenHashMap();
   private AEKey[] idToKey;
   private int nextId;
   private int[] freeIds;
   private int freeCount;
   private long[] lo;
   private long[] hi;
   private CompoundTag[] serializedKey;
   private int[] dirtyQueue;
   private int dirtyCount;
   private boolean[] inQueue;
   private boolean[] isStructDirty;
   private int totalTypes;
   private boolean needsPersist;
   private boolean needsCompact;
   private long modCount;
   private final Object2IntOpenHashMap<AEKeyType> typeCounts = new Object2IntOpenHashMap();
   private final Object2LongOpenHashMap<AEKeyType> typeAmountLo = new Object2LongOpenHashMap();
   private final Object2LongOpenHashMap<AEKeyType> typeAmountHi = new Object2LongOpenHashMap();

   IndexedStorage() {
      this.keyToId.defaultReturnValue(-1);
      this.initArrays(256);
   }

   int getTotalTypes() {
      return this.totalTypes;
   }

   boolean needsPersist() {
      return this.needsPersist;
   }

   long getModCount() {
      return this.modCount;
   }

   Object2LongOpenHashMap<AEKeyType> getTypeAmountLo() {
      return this.typeAmountLo;
   }

   Object2LongOpenHashMap<AEKeyType> getTypeAmountHi() {
      return this.typeAmountHi;
   }

   Object2IntOpenHashMap<AEKeyType> getTypeCounts() {
      return this.typeCounts;
   }

   private void enqueueDirty(int id) {
      if (!this.inQueue[id]) {
         this.inQueue[id] = true;
         if (this.dirtyCount == this.dirtyQueue.length) {
            this.dirtyQueue = Arrays.copyOf(this.dirtyQueue, this.dirtyQueue.length * 2);
         }

         this.dirtyQueue[this.dirtyCount++] = id;
      }
   }

   long insert(AEKey key, long amount, Actionable mode) {
      if (amount <= 0L) {
         return 0L;
      } else if (mode == Actionable.SIMULATE) {
         return amount;
      } else {
         int id = this.keyToId.getInt(key);
         boolean isNewKey = id == -1;
         if (isNewKey) {
            id = this.allocateId(key);
            this.totalTypes++;
         } else {
            this.enqueueDirty(id);
         }

         long newLo = this.lo[id] + amount;
         if (newLo < 0L) {
            newLo &= Long.MAX_VALUE;
            this.hi[id]++;
         }

         this.lo[id] = newLo;
         AEKeyType kt = key.getType();
         if (isNewKey) {
            this.typeCounts.addTo(kt, 1);
         }

         long sumLo = this.typeAmountLo.getLong(kt) + amount;
         long sumHi = this.typeAmountHi.getLong(kt);
         if (sumLo < 0L) {
            sumLo &= Long.MAX_VALUE;
            sumHi++;
         }

         this.typeAmountLo.put(kt, sumLo);
         this.typeAmountHi.put(kt, sumHi);
         this.needsPersist = true;
         this.modCount++;
         return amount;
      }
   }

   long extract(AEKey key, long amount, Actionable mode) {
      if (amount <= 0L) {
         return 0L;
      } else {
         int id = this.keyToId.getInt(key);
         if (id == -1) {
            return 0L;
         } else {
            long curLo = this.lo[id];
            long curHi = this.hi[id];
            long taken = DualLong126.geq(curHi, curLo, amount) ? amount : curLo;
            if (mode == Actionable.SIMULATE) {
               return taken;
            } else {
               long newLo = curLo - taken;
               if (newLo < 0L) {
                  newLo &= Long.MAX_VALUE;
                  this.hi[id]--;
               }

               boolean keyRemoved = newLo == 0L && this.hi[id] == 0L;
               if (keyRemoved) {
                  this.recycleId(id, key);
                  this.totalTypes--;
                  if (this.freeCount > Math.max(this.totalTypes, 1) * 2) {
                     this.needsCompact = true;
                  }
               } else {
                  this.lo[id] = newLo;
                  this.enqueueDirty(id);
               }

               AEKeyType kt = key.getType();
               long sumLo = this.typeAmountLo.getLong(kt) - taken;
               long sumHi = this.typeAmountHi.getLong(kt);
               if (sumLo < 0L) {
                  sumLo &= Long.MAX_VALUE;
                  sumHi--;
               }

               if (keyRemoved) {
                  int remaining = this.typeCounts.addTo(kt, -1);
                  if (remaining <= 0) {
                     this.typeCounts.removeInt(kt);
                     this.typeAmountLo.removeLong(kt);
                     this.typeAmountHi.removeLong(kt);
                  } else {
                     this.typeAmountLo.put(kt, sumLo);
                     this.typeAmountHi.put(kt, sumHi);
                  }
               } else {
                  this.typeAmountLo.put(kt, sumLo);
                  this.typeAmountHi.put(kt, sumHi);
               }

               this.needsPersist = true;
               this.modCount++;
               return taken;
            }
         }
      }
   }

   void getAvailableStacks(KeyCounter out) {
      for (int id = 0; id < this.nextId; id++) {
         if (this.idToKey[id] != null) {
            out.add(this.idToKey[id], DualLong126.cap(this.hi[id], this.lo[id]));
         }
      }
   }

   boolean containsKey(AEKey key) {
      return this.keyToId.containsKey(key);
   }

   long getAmount(AEKey key) {
      int id = this.keyToId.getInt(key);
      return id == -1 ? 0L : DualLong126.cap(this.hi[id], this.lo[id]);
   }

   CompoundTag persist(@Nullable CompoundTag lastRoot) {
      return this.persist(lastRoot, AEKey::toTagGeneric);
   }

   CompoundTag persist(@Nullable CompoundTag lastRoot, IndexedStorage.KeySerializer keySerializer) {
      if (this.needsCompact) {
         this.compact();
         lastRoot = null;
      }

      if (lastRoot == null) {
         return this.persistFull(keySerializer);
      } else {
         ListTag keys = lastRoot.m_128437_("keys", 10);
         long[] pLo = lastRoot.m_128467_("lo");
         long[] pHi = lastRoot.m_128467_("hi");
         int tagLen = alignPow2(this.nextId);
         if (pLo.length < this.nextId) {
            pLo = Arrays.copyOf(pLo, tagLen);
            pHi = Arrays.copyOf(pHi, tagLen);
            lastRoot.m_128365_("lo", new LongArrayTag(pLo));
            lastRoot.m_128365_("hi", new LongArrayTag(pHi));
         }

         while (keys.size() < this.nextId) {
            keys.add(new CompoundTag());
         }

         for (int i = 0; i < this.dirtyCount; i++) {
            int id = this.dirtyQueue[i];
            this.inQueue[id] = false;
            if (this.isStructDirty[id]) {
               this.isStructDirty[id] = false;
               if (this.idToKey[id] != null) {
                  if (this.serializedKey[id] == null) {
                     this.serializedKey[id] = keySerializer.toTag(this.idToKey[id]);
                  }

                  CompoundTag tag = new CompoundTag();
                  tag.m_128365_("key", this.serializedKey[id]);
                  keys.set(id, tag);
               } else {
                  keys.set(id, new CompoundTag());
               }
            }

            pLo[id] = this.lo[id];
            pHi[id] = this.hi[id];
         }

         this.dirtyCount = 0;
         lastRoot.m_128405_("totalTypes", this.totalTypes);
         this.needsPersist = false;
         return lastRoot;
      }
   }

   private CompoundTag persistFull(IndexedStorage.KeySerializer keySerializer) {
      for (int i = 0; i < this.dirtyCount; i++) {
         int id = this.dirtyQueue[i];
         this.inQueue[id] = false;
         this.isStructDirty[id] = false;
      }

      this.dirtyCount = 0;
      int tagLen = alignPow2(this.nextId);
      ListTag keys = new ListTag();
      long[] pLo = new long[tagLen];
      long[] pHi = new long[tagLen];

      for (int id = 0; id < this.nextId; id++) {
         if (this.idToKey[id] != null) {
            if (this.serializedKey[id] == null) {
               this.serializedKey[id] = keySerializer.toTag(this.idToKey[id]);
            }

            CompoundTag tag = new CompoundTag();
            tag.m_128365_("key", this.serializedKey[id]);
            keys.add(tag);
            pLo[id] = this.lo[id];
            pHi[id] = this.hi[id];
         } else {
            keys.add(new CompoundTag());
         }
      }

      CompoundTag root = new CompoundTag();
      root.m_128365_("keys", keys);
      root.m_128365_("lo", new LongArrayTag(pLo));
      root.m_128365_("hi", new LongArrayTag(pHi));
      root.m_128405_("totalTypes", this.totalTypes);
      this.needsPersist = false;
      return root;
   }

   private void compact() {
      int newCap = alignPow2(Math.max(this.totalTypes, 1));
      int newNext = 0;
      AEKey[] nKey = new AEKey[newCap];
      long[] nLo = new long[newCap];
      long[] nHi = new long[newCap];
      CompoundTag[] nSer = new CompoundTag[newCap];

      for (int old = 0; old < this.nextId; old++) {
         if (this.idToKey[old] != null) {
            int nid = newNext++;
            nKey[nid] = this.idToKey[old];
            nLo[nid] = this.lo[old];
            nHi[nid] = this.hi[old];
            nSer[nid] = this.serializedKey[old];
            this.keyToId.put(this.idToKey[old], nid);
         }
      }

      this.idToKey = nKey;
      this.lo = nLo;
      this.hi = nHi;
      this.serializedKey = nSer;
      this.inQueue = new boolean[newCap];
      this.isStructDirty = new boolean[newCap];
      this.dirtyQueue = new int[Math.max(64, newNext)];
      this.dirtyCount = 0;
      this.nextId = newNext;
      this.freeIds = new int[64];
      this.freeCount = 0;
      this.needsCompact = false;
   }

   private static int alignPow2(int n) {
      return n <= 256 ? 256 : Integer.highestOneBit(n - 1) << 1;
   }

   void load(CompoundTag root) {
      this.keyToId.clear();
      this.nextId = 0;
      this.freeCount = 0;
      this.totalTypes = 0;
      this.dirtyCount = 0;
      this.typeCounts.clear();
      this.typeAmountLo.clear();
      this.typeAmountHi.clear();
      ListTag keys = root.m_128437_("keys", 10);
      long[] pLo = root.m_128467_("lo");
      long[] pHi = root.m_128467_("hi");
      int size = keys.size();
      this.ensureCapacity(size);
      this.nextId = size;

      for (int id = 0; id < size; id++) {
         this.inQueue[id] = false;
         this.isStructDirty[id] = false;
         CompoundTag entry = keys.m_128728_(id);
         if (!entry.m_128441_("key")) {
            this.addFree(id);
         } else {
            AEKey key = AEKey.fromTagGeneric(entry.m_128469_("key"));
            if (key == null) {
               this.addFree(id);
            } else {
               this.keyToId.put(key, id);
               this.idToKey[id] = key;
               this.lo[id] = id < pLo.length ? pLo[id] : 0L;
               this.hi[id] = id < pHi.length ? pHi[id] : 0L;
               this.serializedKey[id] = entry.m_128469_("key");
               this.totalTypes++;
               AEKeyType kt = key.getType();
               this.typeCounts.addTo(kt, 1);
               long sumLo = this.typeAmountLo.getLong(kt) + this.lo[id];
               long sumHi = this.typeAmountHi.getLong(kt) + this.hi[id];
               if (sumLo < 0L) {
                  sumLo &= Long.MAX_VALUE;
                  sumHi++;
               }

               this.typeAmountLo.put(kt, sumLo);
               this.typeAmountHi.put(kt, sumHi);
            }
         }
      }

      if (this.freeCount > Math.max(this.totalTypes, 1) * 2) {
         this.needsCompact = true;
      }
   }

   private int allocateId(AEKey key) {
      int id;
      if (this.freeCount > 0) {
         id = this.freeIds[--this.freeCount];
      } else {
         id = this.nextId++;
         this.ensureCapacity(id);
      }

      this.keyToId.put(key, id);
      this.idToKey[id] = key;
      this.lo[id] = 0L;
      this.hi[id] = 0L;
      this.serializedKey[id] = null;
      this.isStructDirty[id] = true;
      this.enqueueDirty(id);
      return id;
   }

   private void recycleId(int id, AEKey key) {
      this.keyToId.removeInt(key);
      this.idToKey[id] = null;
      this.lo[id] = 0L;
      this.hi[id] = 0L;
      this.serializedKey[id] = null;
      this.isStructDirty[id] = true;
      this.enqueueDirty(id);
      this.addFree(id);
   }

   private void addFree(int id) {
      if (this.freeCount == this.freeIds.length) {
         this.freeIds = Arrays.copyOf(this.freeIds, this.freeIds.length * 2);
      }

      this.freeIds[this.freeCount++] = id;
   }

   private void ensureCapacity(int required) {
      if (required >= this.lo.length) {
         int newCap = Math.max(256, Integer.highestOneBit(required) << 1);
         this.lo = Arrays.copyOf(this.lo, newCap);
         this.hi = Arrays.copyOf(this.hi, newCap);
         this.idToKey = Arrays.copyOf(this.idToKey, newCap);
         this.serializedKey = Arrays.copyOf(this.serializedKey, newCap);
         this.inQueue = Arrays.copyOf(this.inQueue, newCap);
         this.isStructDirty = Arrays.copyOf(this.isStructDirty, newCap);
      }
   }

   private void initArrays(int capacity) {
      this.lo = new long[capacity];
      this.hi = new long[capacity];
      this.idToKey = new AEKey[capacity];
      this.serializedKey = new CompoundTag[capacity];
      this.inQueue = new boolean[capacity];
      this.isStructDirty = new boolean[capacity];
      this.dirtyQueue = new int[64];
      this.freeIds = new int[64];
   }

   @FunctionalInterface
   interface KeySerializer {
      CompoundTag toTag(AEKey var1);
   }
}
