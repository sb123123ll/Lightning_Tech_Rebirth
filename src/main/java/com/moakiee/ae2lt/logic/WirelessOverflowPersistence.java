package com.moakiee.ae2lt.logic;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.crafting.IPatternDetails.IInput;
import appeng.api.stacks.GenericStack;
import com.moakiee.ae2lt.blockentity.OverloadedPatternProviderBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

final class WirelessOverflowPersistence {
   private static final String TAG_W_SEND_LIST = "WirelessSendList";
   private static final String TAG_W_SEND_CONN = "WirelessSendConn";
   private static final String TAG_WIRELESS_OVERFLOW = "ae2lt:wireless_overflow";
   private static final String TAG_OVERFLOW_PATTERNS = "patterns";
   private static final String TAG_OVERFLOW_PATTERN_ID = "id";
   private static final String TAG_OVERFLOW_PATTERN = "pattern";
   private static final String TAG_OVERFLOW_BUCKETS = "buckets";
   private static final String TAG_OVERFLOW_CONN = "conn";
   private static final String TAG_OVERFLOW_PID = "pid";
   private static final String TAG_OVERFLOW_IDX = "idx";
   private static final String TAG_OVERFLOW_REMAINING = "remaining";
   private static final String TAG_OVERFLOW_FALLBACK = "fallback";
   private static final String TAG_OVERFLOW_FACE = "ae2lt:face";
   private static final String TAG_OVERFLOW_COMPACT = "compact";
   private final Map<Integer, ItemStack> pendingPatternDefinitions = new HashMap<>();
   private final List<WirelessOverflowPersistence.PendingBucketLoad> pendingBuckets = new ArrayList<>();

   void write(CompoundTag tag, WirelessOverflowQueue overflow) {
      if (!overflow.isEmpty()) {
         CompoundTag overflowTag = new CompoundTag();
         ListTag patternList = new ListTag();
         HashMap<Integer, Short> remappedIds = new HashMap<>();
         short nextWriteId = 0;

         for (WirelessOverflowQueue.Bucket bucket : overflow.buckets()) {
            if (bucket.compactMode) {
               int runtimeId = Short.toUnsignedInt(bucket.patternId);
               if (!remappedIds.containsKey(runtimeId)) {
                  IPatternDetails pattern = overflow.pattern(runtimeId);
                  if (pattern != null) {
                     short writeId = nextWriteId++;
                     remappedIds.put(runtimeId, writeId);
                     CompoundTag patternTag = new CompoundTag();
                     patternTag.m_128376_("id", writeId);
                     patternTag.m_128365_("pattern", pattern.getDefinition().toStack().m_41739_(new CompoundTag()));
                     patternList.add(patternTag);
                  }
               }
            }
         }

         overflowTag.m_128365_("patterns", patternList);
         ListTag bucketList = new ListTag();

         for (OverloadedPatternProviderBlockEntity.WirelessConnection connection : overflow.connections()) {
            WirelessOverflowQueue.Bucket bucketx = overflow.get(connection);
            if (bucketx != null) {
               CompoundTag bucketTag = new CompoundTag();
               bucketTag.m_128365_("conn", connection.toTag());
               bucketTag.m_128379_("compact", bucketx.compactMode);
               if (bucketx.compactMode) {
                  Short remapped = remappedIds.get(Short.toUnsignedInt(bucketx.patternId));
                  if (remapped == null) {
                     continue;
                  }

                  bucketTag.m_128376_("pid", remapped);
                  bucketTag.m_128376_("idx", bucketx.stuckIndex);
                  bucketTag.m_128356_("remaining", bucketx.remaining);
               } else {
                  bucketTag.m_128365_("fallback", writeRoutedOverflow(bucketx.fallback));
               }

               bucketList.add(bucketTag);
            }
         }

         overflowTag.m_128365_("buckets", bucketList);
         tag.m_128365_("ae2lt:wireless_overflow", overflowTag);
      }
   }

   void read(CompoundTag tag) {
      this.clear();
      if (!tag.m_128425_("ae2lt:wireless_overflow", 10)) {
         this.readLegacy(tag);
      } else {
         CompoundTag overflowTag = tag.m_128469_("ae2lt:wireless_overflow");
         ListTag patterns = overflowTag.m_128437_("patterns", 10);

         for (int i = 0; i < patterns.size(); i++) {
            CompoundTag patternTag = patterns.m_128728_(i);
            int id = Short.toUnsignedInt(patternTag.m_128448_("id"));
            ItemStack stack = ItemStack.m_41712_(patternTag.m_128469_("pattern"));
            if (!stack.m_41619_()) {
               this.pendingPatternDefinitions.put(id, stack);
            }
         }

         ListTag buckets = overflowTag.m_128437_("buckets", 10);

         for (int ix = 0; ix < buckets.size(); ix++) {
            CompoundTag bucketTag = buckets.m_128728_(ix);
            if (bucketTag.m_128425_("conn", 10)) {
               OverloadedPatternProviderBlockEntity.WirelessConnection connection = OverloadedPatternProviderBlockEntity.WirelessConnection.fromTag(
                  bucketTag.m_128469_("conn")
               );
               if (bucketTag.m_128471_("compact")) {
                  this.pendingBuckets
                     .add(
                        new WirelessOverflowPersistence.PendingBucketLoad(
                           connection, bucketTag.m_128448_("pid"), bucketTag.m_128448_("idx"), bucketTag.m_128454_("remaining"), List.of(), true
                        )
                     );
               } else {
                  List<RoutedPatternOverflow.Entry> fallback = readRoutedOverflow(bucketTag.m_128437_("fallback", 10));
                  if (!fallback.isEmpty()) {
                     this.pendingBuckets.add(new WirelessOverflowPersistence.PendingBucketLoad(connection, (short)0, (short)0, 0L, fallback, false));
                  }
               }
            }
         }
      }
   }

   boolean finishLoad(
      Level level,
      long gameTick,
      WirelessOverflowQueue overflow,
      Function<OverloadedPatternProviderBlockEntity.WirelessConnection, OverloadedPatternProviderBlockEntity.WirelessConnection> connectionResolver,
      Consumer<OverloadedPatternProviderBlockEntity.WirelessConnection> restoredConnection
   ) {
      if (this.pendingBuckets.isEmpty()) {
         return false;
      } else if (!this.pendingPatternDefinitions.isEmpty() && level == null) {
         return false;
      } else {
         for (Entry<Integer, ItemStack> entry : this.pendingPatternDefinitions.entrySet()) {
            IPatternDetails details = PatternDetailsHelper.decodePattern(entry.getValue(), level);
            if (details != null) {
               overflow.restorePattern(entry.getKey(), details);
            }
         }

         for (WirelessOverflowPersistence.PendingBucketLoad pending : this.pendingBuckets) {
            WirelessOverflowQueue.Bucket bucket;
            if (pending.compactMode()) {
               IPatternDetails pattern = overflow.pattern(Short.toUnsignedInt(pending.patternId()));
               if (pattern == null || pending.remaining() <= 0L) {
                  continue;
               }

               IInput[] inputs = pattern.getInputs();
               if (pending.stuckIndex() < 0 || pending.stuckIndex() >= inputs.length) {
                  continue;
               }

               bucket = WirelessOverflowQueue.Bucket.compact(pending.patternId(), pending.stuckIndex(), pending.remaining());
            } else {
               if (pending.fallback().isEmpty()) {
                  continue;
               }

               bucket = WirelessOverflowQueue.Bucket.routedFallback(pending.patternId(), pending.fallback());
            }

            OverloadedPatternProviderBlockEntity.WirelessConnection connection = connectionResolver.apply(pending.connection());
            overflow.restoreBucket(connection, bucket, gameTick);
            restoredConnection.accept(connection);
         }

         this.clear();
         return true;
      }
   }

   void clear() {
      this.pendingPatternDefinitions.clear();
      this.pendingBuckets.clear();
   }

   static ListTag writeRoutedOverflow(RoutedPatternOverflow overflow) {
      ListTag list = new ListTag();

      for (RoutedPatternOverflow.Entry entry : overflow.snapshot()) {
         CompoundTag stackTag = GenericStack.writeTag(entry.stack());
         if (entry.face() != null) {
            stackTag.m_128344_("ae2lt:face", (byte)entry.face().m_122411_());
         }

         list.add(stackTag);
      }

      return list;
   }

   static List<RoutedPatternOverflow.Entry> readRoutedOverflow(ListTag list) {
      ArrayList<RoutedPatternOverflow.Entry> entries = new ArrayList<>(list.size());

      for (int i = 0; i < list.size(); i++) {
         CompoundTag stackTag = list.m_128728_(i);
         GenericStack stack = GenericStack.readTag(stackTag);
         if (stack != null && stack.amount() > 0L) {
            Direction face = null;
            if (stackTag.m_128425_("ae2lt:face", 1)) {
               int faceId = stackTag.m_128445_("ae2lt:face");
               if (faceId >= 0 && faceId < Direction.values().length) {
                  face = Direction.m_122376_(faceId);
               }
            }

            entries.add(new RoutedPatternOverflow.Entry(face, stack));
         }
      }

      return entries;
   }

   private void readLegacy(CompoundTag tag) {
      if (tag.m_128425_("WirelessSendList", 9) && tag.m_128425_("WirelessSendConn", 10)) {
         List<GenericStack> fallback = readGenericStackList(tag.m_128437_("WirelessSendList", 10));
         if (!fallback.isEmpty()) {
            this.pendingBuckets
               .add(
                  new WirelessOverflowPersistence.PendingBucketLoad(
                     OverloadedPatternProviderBlockEntity.WirelessConnection.fromTag(tag.m_128469_("WirelessSendConn")),
                     (short)0,
                     (short)0,
                     0L,
                     toUnroutedOverflow(fallback),
                     false
                  )
               );
         }
      }
   }

   private static List<RoutedPatternOverflow.Entry> toUnroutedOverflow(List<GenericStack> stacks) {
      ArrayList<RoutedPatternOverflow.Entry> entries = new ArrayList<>(stacks.size());

      for (GenericStack stack : stacks) {
         entries.add(new RoutedPatternOverflow.Entry(null, stack));
      }

      return entries;
   }

   private static List<GenericStack> readGenericStackList(ListTag list) {
      ArrayList<GenericStack> stacks = new ArrayList<>(list.size());

      for (int i = 0; i < list.size(); i++) {
         GenericStack stack = GenericStack.readTag(list.m_128728_(i));
         if (stack != null && stack.amount() > 0L) {
            stacks.add(stack);
         }
      }

      return stacks;
   }

   private static record PendingBucketLoad(
      OverloadedPatternProviderBlockEntity.WirelessConnection connection,
      short patternId,
      short stuckIndex,
      long remaining,
      List<RoutedPatternOverflow.Entry> fallback,
      boolean compactMode
   ) {
   }
}
