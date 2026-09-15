package com.moakiee.ae2lt.crafting.timewheel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

final class SharedBatchSeedConsumerState {
   private static final String TAG_CONSUMERS = "sharedBatchSeedConsumers";
   private static final String TAG_CONSUMER = "consumer";
   private final Set<UUID> consumers = new HashSet<>();

   void recordSuccessfulBatch(UUID consumerId) {
      if (consumerId != null) {
         this.consumers.add(consumerId);
      }
   }

   long pendingDemand(UUID consumerId, long perCopy, long remainingCopies) {
      if (perCopy <= 0L || remainingCopies <= 0L || this.consumers.contains(consumerId)) {
         return 0L;
      } else {
         return perCopy > Long.MAX_VALUE / remainingCopies ? Long.MAX_VALUE : perCopy * remainingCopies;
      }
   }

   void readFromNBT(CompoundTag data) {
      this.consumers.clear();
      ListTag tags = data.m_128437_("sharedBatchSeedConsumers", 10);

      for (int i = 0; i < tags.size(); i++) {
         CompoundTag entry = tags.m_128728_(i);
         if (entry.m_128403_("consumer")) {
            this.consumers.add(entry.m_128342_("consumer"));
         }
      }
   }

   void writeToNBT(CompoundTag data) {
      if (this.consumers.isEmpty()) {
         data.m_128473_("sharedBatchSeedConsumers");
      } else {
         ListTag tags = new ListTag();
         ArrayList<UUID> orderedConsumers = new ArrayList<>(this.consumers);
         orderedConsumers.sort(UUID::compareTo);

         for (UUID consumerId : orderedConsumers) {
            CompoundTag entry = new CompoundTag();
            entry.m_128362_("consumer", consumerId);
            tags.add(entry);
         }

         data.m_128365_("sharedBatchSeedConsumers", tags);
      }
   }
}
