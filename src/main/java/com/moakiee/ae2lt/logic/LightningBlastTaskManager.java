package com.moakiee.ae2lt.logic;

import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt"
)
public final class LightningBlastTaskManager {
   private static final List<LightningBlastTask> ACTIVE_TASKS = new ArrayList<>();

   private LightningBlastTaskManager() {
   }

   public static void schedule(LightningBlastTask task) {
      if (!task.isCompleted()) {
         ACTIVE_TASKS.add(task);
      }
   }

   @SubscribeEvent
   public static void onServerStopped(ServerStoppedEvent event) {
      ACTIVE_TASKS.clear();
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent event) {
      if (event.phase == Phase.END) {
         if (!ACTIVE_TASKS.isEmpty()) {
            int remainingBlockBudget = AE2LTCommonConfig.overloadTntGlobalBlockBudgetPerTick();
            int remainingLightningBudget = AE2LTCommonConfig.overloadTntGlobalLightningBudgetPerTick();
            Iterator<LightningBlastTask> iterator = ACTIVE_TASKS.iterator();
            int remainingTasks = ACTIVE_TASKS.size();

            while (iterator.hasNext()) {
               LightningBlastTask task = iterator.next();
               int blockShare = remainingBlockBudget <= 0
                  ? 0
                  : (remainingTasks > 0 ? Math.max(1, remainingBlockBudget / remainingTasks) : remainingBlockBudget);
               int lightningShare = remainingTasks > 0 ? Math.max(0, remainingLightningBudget / remainingTasks) : remainingLightningBudget;
               LightningBlastTask.TickResult tickResult = task.tick(blockShare, lightningShare);
               remainingBlockBudget = Math.max(0, remainingBlockBudget - tickResult.consumedBlocks());
               remainingLightningBudget = Math.max(0, remainingLightningBudget - tickResult.consumedLightning());
               remainingTasks--;
               if (task.isCompleted()) {
                  iterator.remove();
               }
            }
         }
      }
   }
}
