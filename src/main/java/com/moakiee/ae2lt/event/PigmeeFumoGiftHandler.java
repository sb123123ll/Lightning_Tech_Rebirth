package com.moakiee.ae2lt.event;

import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.registry.ModFumos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt"
)
public final class PigmeeFumoGiftHandler {
   private static final String GIFTED_TAG = "ae2lt.pigmee_fumo_gifted";
   private static final int HOTBAR_START = 0;
   private static final int HOTBAR_END_EXCLUSIVE = 9;

   private PigmeeFumoGiftHandler() {
   }

   @SubscribeEvent
   public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
      if (!(event.getEntity() instanceof FakePlayer) && AE2LTCommonConfig.pigmeeFumoGiftOnFirstJoin()) {
         Player player = event.getEntity();
         CompoundTag data = player.getPersistentData();
         if (!data.m_128471_("ae2lt.pigmee_fumo_gifted")) {
            ItemStack gift = new ItemStack((ItemLike)ModFumos.PIGMEE_FUMO_ITEM.get());
            insertGift(player.m_150109_(), gift);
            if (!gift.m_41619_()) {
               dropOverflow(player, gift);
            }

            player.m_150109_().m_6596_();
            data.m_128379_("ae2lt.pigmee_fumo_gifted", true);
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerClone(Clone event) {
      if (event.getOriginal().getPersistentData().m_128471_("ae2lt.pigmee_fumo_gifted")) {
         event.getEntity().getPersistentData().m_128379_("ae2lt.pigmee_fumo_gifted", true);
      }
   }

   private static void insertGift(Inventory inventory, ItemStack gift) {
      tryInsertIntoSlots(inventory, gift, 0, 9);
      if (!gift.m_41619_()) {
         tryInsertIntoSlots(inventory, gift, 9, inventory.f_35974_.size());
      }
   }

   private static void dropOverflow(Player player, ItemStack gift) {
      ItemStack dropStack = gift.m_41777_();
      gift.m_41764_(0);
      ItemEntity dropped = player.m_7197_(dropStack, false, false);
      if (dropped != null) {
         dropped.m_32061_();
         dropped.m_149678_();
         player.m_9236_().m_7967_(dropped);
      }
   }

   private static void tryInsertIntoSlots(Inventory inventory, ItemStack gift, int start, int endExclusive) {
      for (int slot = start; slot < endExclusive && !gift.m_41619_(); slot++) {
         ItemStack stack = (ItemStack)inventory.f_35974_.get(slot);
         if (canMerge(stack, gift)) {
            int amount = Math.min(gift.m_41613_(), stack.m_41741_() - stack.m_41613_());
            stack.m_41769_(amount);
            gift.m_41774_(amount);
         }
      }

      for (int slotx = start; slotx < endExclusive && !gift.m_41619_(); slotx++) {
         if (((ItemStack)inventory.f_35974_.get(slotx)).m_41619_()) {
            inventory.f_35974_.set(slotx, gift.m_41777_());
            gift.m_41764_(0);
         }
      }
   }

   private static boolean canMerge(ItemStack stack, ItemStack gift) {
      return !stack.m_41619_() && ItemStack.m_150942_(stack, gift) && stack.m_41613_() < stack.m_41741_();
   }
}
