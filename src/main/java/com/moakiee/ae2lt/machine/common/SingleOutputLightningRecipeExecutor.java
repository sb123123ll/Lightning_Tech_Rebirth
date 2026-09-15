package com.moakiee.ae2lt.machine.common;

import com.moakiee.ae2lt.me.key.LightningKey;
import java.util.Optional;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;

public final class SingleOutputLightningRecipeExecutor {
   private SingleOutputLightningRecipeExecutor() {
   }

   public static boolean complete(
      int inputStartSlot,
      int inputEndSlot,
      IntUnaryOperator slotConsumption,
      ItemStack result,
      Supplier<Optional<SingleOutputLightningRecipeExecutor.LightningPlan>> lightningPlanSupplier,
      SingleOutputLightningRecipeExecutor.InventoryAdapter inventory,
      SingleOutputLightningRecipeExecutor.LightningAdapter lightning
   ) {
      if (!inventory.canAcceptOutput(result)) {
         return false;
      } else {
         for (int slot = inputStartSlot; slot <= inputEndSlot; slot++) {
            int toConsume = slotConsumption.applyAsInt(slot);
            if (toConsume > 0 && inventory.getStackInSlot(slot).m_41613_() < toConsume) {
               return false;
            }
         }

         Optional<SingleOutputLightningRecipeExecutor.LightningPlan> lightningPlan = lightningPlanSupplier.get();
         if (lightningPlan.isEmpty()) {
            return false;
         } else {
            SingleOutputLightningRecipeExecutor.LightningPlan plan = lightningPlan.get();
            if (lightning.simulateExtract(plan.key(), plan.amount()) < plan.amount()) {
               return false;
            } else {
               ItemStack[] extractedInputs = new ItemStack[inputEndSlot + 1];

               for (int slotx = inputStartSlot; slotx <= inputEndSlot; slotx++) {
                  int toConsume = slotConsumption.applyAsInt(slotx);
                  if (toConsume > 0) {
                     ItemStack extracted = inventory.extractItem(slotx, toConsume);
                     if (extracted.m_41613_() != toConsume) {
                        rollbackInputs(inputStartSlot, inputEndSlot, extractedInputs, inventory);
                        return false;
                     }

                     extractedInputs[slotx] = extracted;
                  }
               }

               long extractedLightning = lightning.extract(plan.key(), plan.amount());
               if (extractedLightning < plan.amount()) {
                  rollbackInputs(inputStartSlot, inputEndSlot, extractedInputs, inventory);
                  if (extractedLightning > 0L) {
                     lightning.insert(plan.key(), extractedLightning);
                  }

                  return false;
               } else if (!inventory.insertOutput(result).m_41619_()) {
                  lightning.insert(plan.key(), extractedLightning);
                  rollbackInputs(inputStartSlot, inputEndSlot, extractedInputs, inventory);
                  return false;
               } else {
                  return true;
               }
            }
         }
      }
   }

   private static void rollbackInputs(
      int inputStartSlot, int inputEndSlot, ItemStack[] extractedInputs, SingleOutputLightningRecipeExecutor.InventoryAdapter inventory
   ) {
      for (int slot = inputStartSlot; slot <= inputEndSlot; slot++) {
         ItemStack extracted = extractedInputs[slot];
         if (extracted != null && !extracted.m_41619_()) {
            inventory.insertInput(slot, extracted);
         }
      }
   }

   public interface InventoryAdapter {
      boolean canAcceptOutput(ItemStack var1);

      ItemStack getStackInSlot(int var1);

      ItemStack extractItem(int var1, int var2);

      ItemStack insertOutput(ItemStack var1);

      void insertInput(int var1, ItemStack var2);
   }

   public interface LightningAdapter {
      long simulateExtract(LightningKey var1, long var2);

      long extract(LightningKey var1, long var2);

      long insert(LightningKey var1, long var2);
   }

   public static record LightningPlan(LightningKey key, long amount) {
   }
}
