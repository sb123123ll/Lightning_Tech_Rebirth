package com.moakiee.ae2lt.client;

import appeng.menu.SlotSemantics;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuPatternUploadRouting;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuUploadTargetData;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import java.lang.ref.WeakReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt",
   value = {Dist.CLIENT}
)
public final class TianshuDirectUploadClient {
   private static WeakReference<TianshuPatternEncodingTermMenu> heldMenu = new WeakReference<>(null);
   private static WeakReference<Screen> heldRecipeScreen = new WeakReference<>(null);
   private static WeakReference<TianshuPatternEncodingTermMenu> awaitingResult = new WeakReference<>(null);
   private static Component awaitingTargetName;

   private TianshuDirectUploadClient() {
   }

   public static boolean holdRecipeScreen(TianshuPatternEncodingTermMenu menu, Screen recipeScreen) {
      if (menu != null && recipeScreen != null && menu.hasPendingDirectUpload()) {
         heldMenu = new WeakReference<>(menu);
         heldRecipeScreen = new WeakReference<>(recipeScreen);
         return true;
      } else {
         return false;
      }
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      Minecraft minecraft = Minecraft.m_91087_();
      handleUploadResult(minecraft);
      TianshuPatternEncodingTermMenu menu = heldMenu.get();
      Screen recipeScreen = heldRecipeScreen.get();
      if (menu == null || recipeScreen == null) {
         clearHeldRecipe();
      } else if (minecraft.f_91074_ == null || minecraft.f_91074_.f_36096_ != menu || minecraft.f_91080_ != recipeScreen) {
         clearHeldRecipe();
      } else if (!menu.hasPendingDirectUpload()) {
         clearHeldRecipe();
      } else if (menu.hasTriggeredUploadAck()) {
         ItemStack stack = firstEncodedPattern(menu);
         TianshuRecipeTransferContext.acceptEncodedPattern(menu, stack);
         if (TianshuRecipeTransferContext.isEncodingResultReady(menu, stack)) {
            TianshuPatternUploadRouting.Route route = minecraft.f_91073_ == null
               ? TianshuPatternUploadRouting.Route.INVALID
               : TianshuPatternUploadRouting.classify(stack, minecraft.f_91073_);
            switch (route) {
               case CLOSED_LOOP_STORAGE:
               case CRAFTING_ASSEMBLER:
                  if (consumeDirectRequest(menu)) {
                     menu.uploadEncodedPattern();
                     awaitResult(menu, null);
                  }

                  clearHeldRecipe();
                  break;
               case PROCESSING_PROVIDER:
                  if (!menu.requestDirectUploadTargetsAfterEncoding()) {
                     return;
                  }

                  if (!menu.hasFreshDirectUploadTargets()) {
                     return;
                  }

                  TianshuUploadSourceSelection.Selection selection = TianshuUploadSourceSelection.collect(menu);
                  TianshuUploadTargetData target = TianshuUploadTargetMatcher.findUniqueCandidate(menu.getUploadTargets(), selection.initialQuery());
                  if (target != null && consumeDirectRequest(menu)) {
                     menu.uploadTianshuPatternToTarget(target.group());
                     awaitResult(menu, target.group().name());
                     clearHeldRecipe();
                  } else {
                     clearHeldRecipe();
                     recipeScreen.m_7379_();
                     if (minecraft.f_91080_ instanceof TianshuPatternEncodingTermScreen<?> terminalScreen) {
                        terminalScreen.openDirectUploadFallback();
                     }
                  }
                  break;
               case INVALID:
                  clearHeldRecipe();
                  recipeScreen.m_7379_();
            }
         }
      }
   }

   private static boolean consumeDirectRequest(TianshuPatternEncodingTermMenu menu) {
      return menu.consumeTriggeredUpload() && menu.consumeDirectUploadRequest();
   }

   private static ItemStack firstEncodedPattern(TianshuPatternEncodingTermMenu menu) {
      return menu.getSlots(SlotSemantics.ENCODED_PATTERN)
         .stream()
         .map(slot -> slot.m_7993_())
         .filter(stack -> !stack.m_41619_())
         .findFirst()
         .orElse(ItemStack.f_41583_);
   }

   private static void awaitResult(TianshuPatternEncodingTermMenu menu, Component targetName) {
      awaitingResult = new WeakReference<>(menu);
      awaitingTargetName = targetName == null ? null : targetName.m_6881_();
   }

   private static void handleUploadResult(Minecraft minecraft) {
      TianshuPatternEncodingTermMenu menu = awaitingResult.get();
      if (menu == null) {
         clearAwaitingResult();
      } else if (minecraft.f_91074_ == null || minecraft.f_91074_.f_36096_ != menu) {
         clearAwaitingResult();
      } else if (menu.uploadState == 1 || menu.uploadState == 3) {
         Component result = menu.uploadState == 1
            ? (
               awaitingTargetName == null
                  ? Component.m_237115_("ae2lt.tianshu.upload.success")
                  : Component.m_237110_("ae2lt.tianshu.upload.success_target", new Object[]{awaitingTargetName})
            )
            : Component.m_237115_("ae2lt.tianshu.upload.failed");
         minecraft.f_91074_.m_5661_(result, false);
         clearAwaitingResult();
      }
   }

   private static void clearAwaitingResult() {
      awaitingResult = new WeakReference<>(null);
      awaitingTargetName = null;
   }

   private static void clearHeldRecipe() {
      heldMenu = new WeakReference<>(null);
      heldRecipeScreen = new WeakReference<>(null);
   }
}
