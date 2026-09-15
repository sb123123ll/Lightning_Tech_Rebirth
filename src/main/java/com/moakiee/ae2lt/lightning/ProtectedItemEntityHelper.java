package com.moakiee.ae2lt.lightning;

import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.ae2lt.registry.ModItems;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ProtectedItemEntityHelper {
   private static final String PROTECT_UNTIL_TAG = "ae2lt.lightning_transform.protect_until";
   private static final String NO_TRANSFORM_UNTIL_TAG = "ae2lt.lightning_transform.no_transform_until";
   private static final String TRANSFORM_LOCK_UNTIL_TAG = "ae2lt.lightning_transform.lock_until";
   private static volatile Set<Item> fireproofItems;

   private static Set<Item> getFireproofItems() {
      if (fireproofItems == null) {
         fireproofItems = Set.of(
            (Item)ModItems.OVERLOAD_ALLOY_BLANK.get(),
            (Item)ModItems.OVERLOAD_CRYSTAL_DUST.get(),
            (Item)ModItems.UNOVERLOADED_CIRCUIT_BOARD.get(),
            (Item)ModItems.OVERLOAD_PROCESSOR.get(),
            (Item)ModItems.OVERLOAD_CRYSTAL.get(),
            (Item)ModItems.OVERLOAD_ALLOY.get(),
            (Item)ModItems.OVERLOAD_CIRCUIT_BOARD.get(),
            (Item)ModItems.RESEARCH_NOTE.get(),
            (Item)ModItems.CHARRED_RITUAL_FRAGMENT.get(),
            ((Block)ModBlocks.OVERLOAD_CRYSTAL_BLOCK.get()).m_5456_(),
            (Item)ModItems.OVERLOAD_SINGULARITY.get(),
            (Item)ModItems.LIGHTNING_COLLAPSE_MATRIX.get()
         );
      }

      return fireproofItems;
   }

   public static boolean isFireproofItem(ItemEntity itemEntity) {
      return getFireproofItems().contains(itemEntity.m_32055_().m_41720_());
   }

   private ProtectedItemEntityHelper() {
   }

   public static void applyOutputProtection(ItemEntity itemEntity, long gameTime) {
      CompoundTag data = itemEntity.getPersistentData();
      long protectUntil = gameTime + 40L;
      putMax(data, "ae2lt.lightning_transform.protect_until", protectUntil);
      putMax(data, "ae2lt.lightning_transform.no_transform_until", protectUntil);
      data.m_128473_("ae2lt.lightning_transform.lock_until");
      itemEntity.m_20095_();
      itemEntity.m_7311_(0);
   }

   public static void applyTransformLock(ItemEntity itemEntity, long gameTime) {
      CompoundTag data = itemEntity.getPersistentData();
      putMax(data, "ae2lt.lightning_transform.lock_until", gameTime + 2L);
      putMax(data, "ae2lt.lightning_transform.protect_until", gameTime + 40L);
      itemEntity.m_20095_();
      itemEntity.m_7311_(0);
   }

   public static boolean isProtectedItem(ItemEntity itemEntity) {
      return isProtectedItem(itemEntity, itemEntity.m_9236_().m_46467_());
   }

   public static boolean isProtectedItem(ItemEntity itemEntity, long gameTime) {
      return itemEntity.getPersistentData().m_128454_("ae2lt.lightning_transform.protect_until") > gameTime;
   }

   public static boolean canParticipateInTransform(ItemEntity itemEntity, long gameTime) {
      if (itemEntity.m_6084_() && !itemEntity.m_32055_().m_41619_()) {
         CompoundTag data = itemEntity.getPersistentData();
         return data.m_128454_("ae2lt.lightning_transform.protect_until") <= gameTime
            && data.m_128454_("ae2lt.lightning_transform.no_transform_until") <= gameTime
            && data.m_128454_("ae2lt.lightning_transform.lock_until") <= gameTime;
      } else {
         return false;
      }
   }

   public static boolean shouldIgnoreDamage(ItemEntity itemEntity, DamageSource damageSource) {
      if (!isLightningOrFireDamage(damageSource)) {
         return false;
      } else if (!isProtectedItem(itemEntity) && !isFireproofItem(itemEntity)) {
         return false;
      } else {
         itemEntity.m_20095_();
         itemEntity.m_7311_(0);
         return true;
      }
   }

   private static boolean isLightningOrFireDamage(DamageSource damageSource) {
      return damageSource.m_276093_(DamageTypes.f_268450_)
         || damageSource.m_276093_(DamageTypes.f_268631_)
         || damageSource.m_276093_(DamageTypes.f_268468_)
         || damageSource.m_276093_(DamageTypes.f_268546_)
         || damageSource.m_269533_(DamageTypeTags.f_268745_);
   }

   public static void tick(ItemEntity itemEntity) {
      if (!itemEntity.m_9236_().m_5776_()) {
         CompoundTag data = itemEntity.getPersistentData();
         if (hasTimedState(data)) {
            long gameTime = itemEntity.m_9236_().m_46467_();
            if (isProtectedItem(itemEntity, gameTime)) {
               itemEntity.m_20095_();
               itemEntity.m_7311_(0);
            }

            clearIfExpired(data, "ae2lt.lightning_transform.protect_until", gameTime);
            clearIfExpired(data, "ae2lt.lightning_transform.no_transform_until", gameTime);
            clearIfExpired(data, "ae2lt.lightning_transform.lock_until", gameTime);
         }
      }
   }

   private static boolean hasTimedState(CompoundTag data) {
      return data.m_128441_("ae2lt.lightning_transform.protect_until")
         || data.m_128441_("ae2lt.lightning_transform.no_transform_until")
         || data.m_128441_("ae2lt.lightning_transform.lock_until");
   }

   private static void clearIfExpired(CompoundTag data, String key, long gameTime) {
      if (data.m_128454_(key) <= gameTime) {
         data.m_128473_(key);
      }
   }

   private static void putMax(CompoundTag data, String key, long until) {
      if (data.m_128454_(key) < until) {
         data.m_128356_(key, until);
      }
   }
}
