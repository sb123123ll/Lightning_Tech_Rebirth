package com.moakiee.ae2lt.logic.tianshu.terminal;

import appeng.api.stacks.AEKey;
import appeng.menu.guisync.PacketWritable;
import com.moakiee.ae2lt.logic.tianshu.loop.TianshuSeedRefillService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

public record SeedRefillSync(int state, List<SeedRefillSync.Entry> problems) implements PacketWritable {
   public static final int STATE_NONE = 0;
   public static final int STATE_COMPLETE = 1;
   public static final int STATE_NETWORK_MISSING = 2;
   public static final int STATE_STORAGE_BLOCKED = 3;
   public static final int STATE_MIXED = 4;
   public static final int STATE_UNAVAILABLE = 5;
   private static final int MAX_MISSING_ENTRIES = 16;
   private static final SeedRefillSync NONE = new SeedRefillSync(0, List.of());

   public SeedRefillSync(int state, List<SeedRefillSync.Entry> problems) {
      problems = List.copyOf(problems);
      if (state >= 0 && state <= 5) {
         this.state = state;
         this.problems = problems;
      } else {
         throw new IllegalArgumentException("invalid seed refill state: " + state);
      }
   }

   public SeedRefillSync(FriendlyByteBuf data) {
      this(data.m_130242_(), readMissing(data));
   }

   public static SeedRefillSync none() {
      return NONE;
   }

   public static SeedRefillSync of(TianshuSeedRefillService.RefillResult result) {
      if (!result.available()) {
         return new SeedRefillSync(5, List.of());
      } else {
         boolean networkMissing = !result.networkMissing().isEmpty();
         boolean storageBlocked = !result.storageBlocked().isEmpty();
         if (!networkMissing && !storageBlocked) {
            return new SeedRefillSync(1, List.of());
         } else {
            ArrayList<SeedRefillSync.Entry> entries = new ArrayList<>();
            LinkedHashSet<AEKey> keys = new LinkedHashSet<>();
            keys.addAll(result.networkMissing().keySet());
            keys.addAll(result.storageBlocked().keySet());

            for (AEKey key : keys) {
               if (entries.size() >= 16) {
                  break;
               }

               entries.add(new SeedRefillSync.Entry(key, result.networkMissing().getOrDefault(key, 0L), result.storageBlocked().getOrDefault(key, 0L)));
            }

            int state = networkMissing && storageBlocked ? 4 : (networkMissing ? 2 : 3);
            return new SeedRefillSync(state, entries);
         }
      }
   }

   public void writeToPacket(FriendlyByteBuf data) {
      data.m_130130_(this.state);
      data.m_130130_(this.problems.size());

      for (SeedRefillSync.Entry entry : this.problems) {
         AEKey.writeKey(data, entry.what());
         data.m_130103_(entry.networkMissing());
         data.m_130103_(entry.storageBlocked());
      }
   }

   private static List<SeedRefillSync.Entry> readMissing(FriendlyByteBuf data) {
      int size = data.m_130242_();
      ArrayList<SeedRefillSync.Entry> result = new ArrayList<>(size);

      for (int i = 0; i < size; i++) {
         result.add(new SeedRefillSync.Entry(AEKey.readKey(data), data.m_130258_(), data.m_130258_()));
      }

      return result;
   }

   public static record Entry(AEKey what, long networkMissing, long storageBlocked) {
   }
}
