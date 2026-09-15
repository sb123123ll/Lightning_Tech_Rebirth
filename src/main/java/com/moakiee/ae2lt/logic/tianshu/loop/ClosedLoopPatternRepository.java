package com.moakiee.ae2lt.logic.tianshu.loop;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public final class ClosedLoopPatternRepository {
   private static final String TAG_PATTERNS = "Patterns";
   private final IntSupplier capacity;
   private final ArrayList<ClosedLoopPatternPayload> patterns = new ArrayList<>();

   public ClosedLoopPatternRepository(IntSupplier capacity) {
      this.capacity = capacity;
   }

   public int capacity() {
      return Math.max(0, this.capacity.getAsInt());
   }

   public int size() {
      return this.patterns.size();
   }

   public List<ClosedLoopPatternPayload> patterns() {
      return List.copyOf(this.patterns);
   }

   public List<ClosedLoopPatternPayload> activePatterns() {
      return this.patterns.stream().limit((long)this.capacity()).toList();
   }

   public ClosedLoopPatternPayload get(int index) {
      return index >= 0 && index < this.patterns.size() ? this.patterns.get(index) : null;
   }

   public int indexOf(ClosedLoopPatternPayload payload) {
      if (payload == null) {
         return -1;
      } else {
         for (int i = 0; i < this.patterns.size(); i++) {
            if (this.patterns.get(i) == payload) {
               return i;
            }
         }

         return -1;
      }
   }

   public ClosedLoopPatternRepository.PutResult add(ClosedLoopPatternPayload payload) {
      if (payload == null) {
         return ClosedLoopPatternRepository.PutResult.INVALID;
      } else if (this.capacity() <= 0) {
         return ClosedLoopPatternRepository.PutResult.UNAVAILABLE;
      } else if (this.patterns.size() >= this.capacity()) {
         return ClosedLoopPatternRepository.PutResult.FULL;
      } else {
         this.patterns.add(payload);
         return ClosedLoopPatternRepository.PutResult.ADDED;
      }
   }

   public ClosedLoopPatternRepository.PutResult replace(ClosedLoopPatternPayload current, ClosedLoopPatternPayload replacement) {
      int index = this.indexOf(current);
      if (index >= 0 && replacement != null) {
         this.patterns.set(index, replacement);
         return ClosedLoopPatternRepository.PutResult.UPDATED;
      } else {
         return ClosedLoopPatternRepository.PutResult.INVALID;
      }
   }

   public boolean remove(ClosedLoopPatternPayload payload) {
      int index = this.indexOf(payload);
      if (index < 0) {
         return false;
      } else {
         this.patterns.remove(index);
         return true;
      }
   }

   public void replaceAll(List<ClosedLoopPatternPayload> payloads) {
      this.patterns.clear();
      if (payloads != null) {
         for (ClosedLoopPatternPayload payload : payloads) {
            if (payload != null && this.patterns.size() < this.capacity()) {
               this.patterns.add(payload);
            }
         }
      }
   }

   public void clear() {
      this.patterns.clear();
   }

   public void writeTo(CompoundTag parent) {
      ListTag list = new ListTag();

      for (ClosedLoopPatternPayload pattern : this.patterns) {
         list.add(ClosedLoopPatternPayloadTagCodec.write(pattern));
      }

      parent.m_128365_("Patterns", list);
   }

   public void readFrom(CompoundTag parent) {
      this.patterns.clear();
      ListTag list = parent.m_128437_("Patterns", 10);

      for (int i = 0; i < list.size(); i++) {
         try {
            ClosedLoopPatternPayload payload = ClosedLoopPatternPayloadTagCodec.read(list.m_128728_(i));
            this.patterns.add(payload);
         } catch (RuntimeException var5) {
         }
      }
   }

   public List<ClosedLoopPatternPayload> overflowedPatterns() {
      int keep = this.capacity();
      if (this.patterns.size() <= keep) {
         return List.of();
      } else {
         ArrayList<ClosedLoopPatternPayload> overflow = new ArrayList<>(this.patterns.size() - keep);
         int index = 0;

         for (ClosedLoopPatternPayload pattern : this.patterns) {
            if (index++ >= keep) {
               overflow.add(pattern);
            }
         }

         return List.copyOf(overflow);
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
