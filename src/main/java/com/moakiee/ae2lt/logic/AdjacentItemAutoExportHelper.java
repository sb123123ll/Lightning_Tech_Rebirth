package com.moakiee.ae2lt.logic;

import appeng.api.behaviors.ExternalStorageStrategy;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.RelativeSide;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.storage.MEStorage;
import appeng.me.storage.CompositeStorage;
import appeng.parts.automation.StackWorldBehaviors;
import java.lang.ref.WeakReference;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public final class AdjacentItemAutoExportHelper {
   private static final long WRAPPER_REFRESH_TICKS = 20L;

   private AdjacentItemAutoExportHelper() {
   }

   public static boolean hasAnyOutput(boolean autoExport, int firstSlot, int slotCount, AdjacentItemAutoExportHelper.SlotStackReader stackReader) {
      if (!autoExport) {
         return false;
      } else {
         for (int slot = firstSlot; slot < firstSlot + slotCount; slot++) {
            if (!stackReader.getStack(slot).m_41619_()) {
               return true;
            }
         }

         return false;
      }
   }

   public static boolean pushOutResult(
      IActionHost host,
      @Nullable BlockOrientation orientation,
      Set<RelativeSide> allowedOutputs,
      int firstSlot,
      int slotCount,
      AdjacentItemAutoExportHelper.SlotStackReader stackReader,
      AdjacentItemAutoExportHelper.SlotExtractor extractor,
      AdjacentItemAutoExportHelper.RemainderInserter remainderInserter,
      AdjacentItemAutoExportHelper.TargetResolver targetResolver
   ) {
      if (orientation != null && !allowedOutputs.isEmpty()) {
         IActionSource actionSource = IActionSource.ofMachine(host);

         for (RelativeSide side : allowedOutputs) {
            Direction direction = orientation.getSide(side);
            if (direction != null) {
               CompositeStorage target = targetResolver.resolve(direction);
               if (target != null) {
                  for (int slot = firstSlot; slot < firstSlot + slotCount; slot++) {
                     ItemStack output = stackReader.getStack(slot);
                     if (!output.m_41619_()) {
                        AEItemKey key = AEItemKey.of(output);
                        if (key != null) {
                           ItemStack extracted = extractor.extract(slot, output.m_41613_());
                           if (!extracted.m_41619_()) {
                              long inserted = target.insert(key, (long)extracted.m_41613_(), Actionable.MODULATE, actionSource);
                              if (inserted < (long)extracted.m_41613_()) {
                                 remainderInserter.insert(extracted.m_255036_((int)((long)extracted.m_41613_() - inserted)));
                              }

                              if (inserted > 0L) {
                                 return true;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean pushOutFluid(
      IActionHost host,
      @Nullable BlockOrientation orientation,
      Set<RelativeSide> allowedOutputs,
      AdjacentItemAutoExportHelper.FluidTankReader tankReader,
      AdjacentItemAutoExportHelper.FluidDrainer drainer,
      AdjacentItemAutoExportHelper.TargetResolver targetResolver
   ) {
      if (orientation != null && !allowedOutputs.isEmpty()) {
         FluidStack stored = tankReader.getFluid();
         if (stored.isEmpty()) {
            return false;
         } else {
            AEFluidKey fluidKey = AEFluidKey.of(stored);
            if (fluidKey == null) {
               return false;
            } else {
               IActionSource actionSource = IActionSource.ofMachine(host);
               int remaining = stored.getAmount();
               boolean pushed = false;

               for (RelativeSide side : allowedOutputs) {
                  if (remaining <= 0) {
                     break;
                  }

                  Direction direction = orientation.getSide(side);
                  if (direction != null) {
                     CompositeStorage target = targetResolver.resolve(direction);
                     if (target != null) {
                        long inserted = target.insert(fluidKey, (long)remaining, Actionable.MODULATE, actionSource);
                        if (inserted > 0L) {
                           int drained = drainer.drain((int)inserted);
                           if (drained > 0) {
                              pushed = true;
                              remaining -= drained;
                           }
                        }
                     }
                  }
               }

               return pushed;
            }
         }
      } else {
         return false;
      }
   }

   @Nullable
   public static CompositeStorage resolveTarget(ServerLevel level, BlockPos origin, Direction direction) {
      if (direction == null) {
         return null;
      } else {
         IdentityHashMap<AEKeyType, MEStorage> externalStorages = new IdentityHashMap<>(2);
         Map<AEKeyType, ExternalStorageStrategy> strategies = StackWorldBehaviors.createExternalStorageStrategies(
            level, origin.m_121945_(direction), direction.m_122424_()
         );

         for (Entry<AEKeyType, ExternalStorageStrategy> entry : strategies.entrySet()) {
            MEStorage wrapper = entry.getValue().createWrapper(false, () -> {
            });
            if (wrapper != null) {
               externalStorages.put(entry.getKey(), wrapper);
            }
         }

         return externalStorages.isEmpty() ? null : new CompositeStorage(externalStorages);
      }
   }

   private static final class CacheEntry {
      private final WeakReference<BlockEntity> blockEntityRef;
      private final Map<AEKeyType, ExternalStorageStrategy> strategies;
      private Map<AEKeyType, MEStorage> wrappers;
      private CompositeStorage compositeStorage;
      private long wrapperCreatedTick;

      private CacheEntry(BlockEntity blockEntity, Map<AEKeyType, ExternalStorageStrategy> strategies) {
         this.blockEntityRef = new WeakReference<>(blockEntity);
         this.strategies = strategies;
      }

      private boolean isValid(BlockEntity currentBlockEntity) {
         return this.blockEntityRef.get() == currentBlockEntity;
      }

      @Nullable
      private CompositeStorage getCompositeStorage(long gameTick) {
         if (this.wrappers == null || gameTick - this.wrapperCreatedTick >= 20L) {
            this.rebuildWrappers(gameTick);
         }

         return this.compositeStorage;
      }

      private void rebuildWrappers(long gameTick) {
         IdentityHashMap<AEKeyType, MEStorage> rebuiltWrappers = new IdentityHashMap<>(this.strategies.size());

         for (Entry<AEKeyType, ExternalStorageStrategy> entry : this.strategies.entrySet()) {
            MEStorage wrapper = entry.getValue().createWrapper(false, () -> {
            });
            if (wrapper != null) {
               rebuiltWrappers.put(entry.getKey(), wrapper);
            }
         }

         this.wrappers = rebuiltWrappers.isEmpty() ? null : rebuiltWrappers;
         this.compositeStorage = this.wrappers == null ? null : new CompositeStorage(this.wrappers);
         this.wrapperCreatedTick = gameTick;
      }
   }

   public static final class DirectionalTargetCache {
      private final EnumMap<Direction, AdjacentItemAutoExportHelper.CacheEntry> cache = new EnumMap<>(Direction.class);

      @Nullable
      public CompositeStorage resolve(ServerLevel level, BlockPos origin, @Nullable Direction direction) {
         if (direction == null) {
            return null;
         } else {
            BlockPos targetPos = origin.m_121945_(direction);
            BlockEntity targetBlockEntity = level.m_7702_(targetPos);
            if (targetBlockEntity == null) {
               this.cache.remove(direction);
               return null;
            } else {
               AdjacentItemAutoExportHelper.CacheEntry cached = this.cache.get(direction);
               if (cached == null || !cached.isValid(targetBlockEntity)) {
                  Map<AEKeyType, ExternalStorageStrategy> strategies = StackWorldBehaviors.createExternalStorageStrategies(
                     level, targetPos, direction.m_122424_()
                  );
                  if (strategies.isEmpty()) {
                     this.cache.remove(direction);
                     return null;
                  }

                  cached = new AdjacentItemAutoExportHelper.CacheEntry(targetBlockEntity, strategies);
                  this.cache.put((Enum)direction, cached);
               }

               return cached.getCompositeStorage(level.m_46467_());
            }
         }
      }

      public void invalidate() {
         this.cache.clear();
      }
   }

   @FunctionalInterface
   public interface FluidDrainer {
      int drain(int var1);
   }

   @FunctionalInterface
   public interface FluidTankReader {
      FluidStack getFluid();
   }

   @FunctionalInterface
   public interface RemainderInserter {
      void insert(ItemStack var1);
   }

   @FunctionalInterface
   public interface SlotExtractor {
      ItemStack extract(int var1, int var2);
   }

   @FunctionalInterface
   public interface SlotStackReader {
      ItemStack getStack(int var1);
   }

   @FunctionalInterface
   public interface TargetResolver {
      @Nullable
      CompositeStorage resolve(Direction var1);
   }
}
